targetScope = 'resourceGroup'

param location string
param projectName string
@allowed(['app-service', 'swa'])
param frontendHosting string
param frontendOrigin string
@secure()
param postgresPassword string
@secure()
param postgresAppPassword string
@secure()
param jwtSecret string
@secure()
param workerApiKey string
@secure()
param openrouterApiKey string
@secure()
param bootstrapAdminPassword string
param bootstrapAdminUsername string

var suffix = uniqueString(subscription().subscriptionId, resourceGroup().id)
var tags = { project: projectName, environment: 'thesis', offer: 'Azure-for-Students' }
var backendName = '${projectName}-api-${suffix}'
var workerName = '${projectName}-worker-${suffix}'
var apiOrigin = 'https://${backendName}.azurewebsites.net'
var selectedOrigin = frontendHosting == 'app-service' ? apiOrigin : frontendOrigin

resource plan 'Microsoft.Web/serverfarms@2023-12-01' = {
  name: '${projectName}-linux'
  location: location
  tags: tags
  kind: 'linux'
  sku: { name: 'B2', tier: 'Basic', capacity: 1 }
  properties: { reserved: true }
}

resource workspace 'Microsoft.OperationalInsights/workspaces@2023-09-01' = {
  name: '${projectName}-logs'
  location: location
  tags: tags
  properties: {
    sku: { name: 'PerGB2018' }
    retentionInDays: 30
    workspaceCapping: { dailyQuotaGb: json('0.1') }
  }
}
resource insights 'Microsoft.Insights/components@2020-02-02' = {
  name: '${projectName}-insights'
  location: location
  tags: tags
  kind: 'web'
  properties: { Application_Type: 'web', WorkspaceResourceId: workspace.id }
}

resource storage 'Microsoft.Storage/storageAccounts@2023-05-01' = {
  name: 'hst${suffix}'
  location: location
  tags: tags
  kind: 'StorageV2'
  sku: { name: 'Standard_LRS' }
  properties: {
    accessTier: 'Hot'
    minimumTlsVersion: 'TLS1_2'
    supportsHttpsTrafficOnly: true
    allowBlobPublicAccess: false
    allowSharedKeyAccess: false
  }
}
resource blobs 'Microsoft.Storage/storageAccounts/blobServices@2023-05-01' = {
  parent: storage
  name: 'default'
  properties: { deleteRetentionPolicy: { enabled: true, days: 7 } }
}
resource documents 'Microsoft.Storage/storageAccounts/blobServices/containers@2023-05-01' = {
  parent: blobs
  name: 'documents'
  properties: { publicAccess: 'None' }
}
resource results 'Microsoft.Storage/storageAccounts/blobServices/containers@2023-05-01' = {
  parent: blobs
  name: 'processing-results'
  properties: { publicAccess: 'None' }
}
resource lifecycle 'Microsoft.Storage/storageAccounts/managementPolicies@2023-05-01' = {
  parent: storage
  name: 'default'
  properties: {
    policy: {
      rules: [{
        name: 'expire-processing-results'
        enabled: true
        type: 'Lifecycle'
        definition: {
          filters: { blobTypes: ['blockBlob'], prefixMatch: ['processing-results/'] }
          // Queue TTL is 7 days; retain blobs longer than all normal delivery/retry windows.
          actions: { baseBlob: { delete: { daysAfterModificationGreaterThan: 14 } } }
        }
      }]
    }
  }
}

resource bus 'Microsoft.ServiceBus/namespaces@2024-01-01' = {
  name: '${projectName}-bus-${suffix}'
  location: location
  tags: tags
  sku: { name: 'Standard', tier: 'Standard' }
  properties: { minimumTlsVersion: '1.2', disableLocalAuth: true }
}
resource queues 'Microsoft.ServiceBus/namespaces/queues@2024-01-01' = [for name in ['document-processing', 'embeddings-ready', 'document-processing-failed']: {
  parent: bus
  name: name
  properties: {
    lockDuration: 'PT5M'
    defaultMessageTimeToLive: 'P7D'
    maxDeliveryCount: 5
    deadLetteringOnMessageExpiration: true
    requiresDuplicateDetection: true
    duplicateDetectionHistoryTimeWindow: 'PT10M'
    maxSizeInMegabytes: 1024
  }
}]
resource notifications 'Microsoft.ServiceBus/namespaces/topics@2024-01-01' = {
  parent: bus
  name: 'notifications'
  properties: { defaultMessageTimeToLive: 'P1D', requiresDuplicateDetection: true, duplicateDetectionHistoryTimeWindow: 'PT10M' }
}
resource notificationSubscription 'Microsoft.ServiceBus/namespaces/topics/subscriptions@2024-01-01' = {
  parent: notifications
  name: 'backend-single-instance'
  properties: { lockDuration: 'PT1M', maxDeliveryCount: 5, deadLetteringOnMessageExpiration: true }
}

resource postgres 'Microsoft.DBforPostgreSQL/flexibleServers@2024-08-01' = {
  name: '${projectName}-pg-${suffix}'
  location: location
  tags: tags
  sku: { name: 'Standard_B1ms', tier: 'Burstable' }
  properties: {
    version: '16'
    administratorLogin: 'thesis_owner'
    administratorLoginPassword: postgresPassword
    storage: { storageSizeGB: 32, type: 'Premium_LRS' }
    backup: { backupRetentionDays: 7, geoRedundantBackup: 'Disabled' }
    highAvailability: { mode: 'Disabled' }
    network: { publicNetworkAccess: 'Enabled' }
  }
}
resource extensions 'Microsoft.DBforPostgreSQL/flexibleServers/configurations@2024-08-01' = {
  parent: postgres
  name: 'azure.extensions'
  properties: { value: 'VECTOR', source: 'user-override' }
}
resource database 'Microsoft.DBforPostgreSQL/flexibleServers/databases@2024-08-01' = {
  parent: postgres
  name: 'hs_thesis'
  properties: { charset: 'UTF8', collation: 'en_US.utf8' }
}

resource vault 'Microsoft.KeyVault/vaults@2023-07-01' = {
  name: 'hskv-${suffix}'
  location: location
  tags: tags
  properties: {
    tenantId: subscription().tenantId
    sku: { family: 'A', name: 'standard' }
    enableRbacAuthorization: true
    enableSoftDelete: true
    enablePurgeProtection: true
    softDeleteRetentionInDays: 7
  }
}
var secretValues = [
  { name: 'postgres-password', value: postgresPassword }
  { name: 'postgres-app-password', value: postgresAppPassword }
  { name: 'jwt-secret', value: jwtSecret }
  { name: 'worker-api-key', value: workerApiKey }
  { name: 'openrouter-api-key', value: openrouterApiKey }
  { name: 'bootstrap-admin-password', value: bootstrapAdminPassword }
]
resource secrets 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = [for secret in secretValues: {
  parent: vault
  name: secret.name
  properties: { value: secret.value }
}]

resource backend 'Microsoft.Web/sites@2023-12-01' = {
  name: backendName
  location: location
  tags: tags
  kind: 'app,linux'
  identity: { type: 'SystemAssigned' }
  properties: {
    serverFarmId: plan.id
    httpsOnly: true
    siteConfig: {
      linuxFxVersion: 'JAVA|21-java21'
      alwaysOn: true
      ftpsState: 'Disabled'
      minTlsVersion: '1.2'
      healthCheckPath: '/actuator/health'
      appSettings: [
        { name: 'SPRING_PROFILES_ACTIVE', value: 'azure' }
        { name: 'FRONTEND_ENABLED', value: string(frontendHosting == 'app-service') }
        { name: 'FRONTEND_ORIGIN', value: selectedOrigin }
        { name: 'AUTH_COOKIE_SAME_SITE', value: 'Strict' }
        { name: 'POSTGRES_DB_URL', value: 'jdbc:postgresql://${postgres.properties.fullyQualifiedDomainName}:5432/hs_thesis?sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory' }
        { name: 'POSTGRES_USER', value: 'thesis_app' }
        { name: 'POSTGRES_PASSWORD', value: '@Microsoft.KeyVault(SecretUri=${vault.properties.vaultUri}secrets/postgres-app-password/)' }
        { name: 'JWT_SECRET', value: '@Microsoft.KeyVault(SecretUri=${vault.properties.vaultUri}secrets/jwt-secret/)' }
        { name: 'ADMIN_USERNAME', value: bootstrapAdminUsername }
        { name: 'ADMIN_PASSWORD', value: '@Microsoft.KeyVault(SecretUri=${vault.properties.vaultUri}secrets/bootstrap-admin-password/)' }
        { name: 'WORKER_API_KEY', value: '@Microsoft.KeyVault(SecretUri=${vault.properties.vaultUri}secrets/worker-api-key/)' }
        { name: 'PYTHON_WORKER_BASE_URL', value: 'https://${workerName}.azurewebsites.net' }
        { name: 'AZURE_STORAGE_ACCOUNT_URL', value: storage.properties.primaryEndpoints.blob }
        { name: 'AZURE_SERVICEBUS_NAMESPACE', value: '${bus.name}.servicebus.windows.net' }
        { name: 'APPLICATIONINSIGHTS_CONNECTION_STRING', value: insights.properties.ConnectionString }
        { name: 'ApplicationInsightsAgent_EXTENSION_VERSION', value: '~3' }
        { name: 'JAVA_OPTS', value: '-Xms128m -Xmx768m' }
      ]
    }
  }
}
resource worker 'Microsoft.Web/sites@2023-12-01' = {
  name: workerName
  location: location
  tags: tags
  kind: 'app,linux'
  identity: { type: 'SystemAssigned' }
  properties: {
    serverFarmId: plan.id
    httpsOnly: true
    siteConfig: {
      linuxFxVersion: 'PYTHON|3.12'
      alwaysOn: true
      ftpsState: 'Disabled'
      minTlsVersion: '1.2'
      healthCheckPath: '/livez'
      appCommandLine: 'python -m uvicorn main:app --host 0.0.0.0 --port 8000 --workers 1'
      appSettings: [
        { name: 'APP_ENV', value: 'azure' }
        { name: 'SCM_DO_BUILD_DURING_DEPLOYMENT', value: 'true' }
        { name: 'PROVIDER_MAX_CONCURRENCY', value: '2' }
        { name: 'WORKER_API_KEY', value: '@Microsoft.KeyVault(SecretUri=${vault.properties.vaultUri}secrets/worker-api-key/)' }
        { name: 'OPENROUTER_API_KEY', value: '@Microsoft.KeyVault(SecretUri=${vault.properties.vaultUri}secrets/openrouter-api-key/)' }
        { name: 'OPENROUTER_HTTP_REFERER', value: selectedOrigin }
        { name: 'AZURE_STORAGE_ACCOUNT_URL', value: storage.properties.primaryEndpoints.blob }
        { name: 'AZURE_SERVICEBUS_NAMESPACE', value: '${bus.name}.servicebus.windows.net' }
        { name: 'APPLICATIONINSIGHTS_CONNECTION_STRING', value: insights.properties.ConnectionString }
        { name: 'OTEL_SERVICE_NAME', value: 'hs-python-worker' }
      ]
    }
  }
}

// Public database access is limited to the apps' possible outbound IPs, not "all Azure services".
module databaseFirewall './database-firewall.bicep' = {
  name: 'database-app-firewall'
  params: {
    serverName: postgres.name
    outboundIps: union(split(backend.properties.possibleOutboundIpAddresses, ','), split(worker.properties.possibleOutboundIpAddresses, ','))
  }
}

var blobContributor = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', 'ba92f5b4-2d11-453d-a403-e96b0029c9fe')
var blobReader = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '2a2b9908-6ea1-4ae2-8e65-a410df84e7d1')
var senderRole = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '69a216fc-b8fb-44d8-bc22-1f3c2cd27a39')
var receiverRole = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '4f6d3b9b-027b-4f4c-9142-0e5a2a2247e0')
var secretsReader = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '4633458b-17de-408a-b874-0445c86b69e6')

resource backendBlobRole 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  scope: storage
  name: guid(storage.id, backend.id, blobContributor)
  properties: { roleDefinitionId: blobContributor, principalId: backend.identity.principalId, principalType: 'ServicePrincipal' }
}
resource workerDocumentsRole 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  scope: documents
  name: guid(documents.id, worker.id, blobReader)
  properties: { roleDefinitionId: blobReader, principalId: worker.identity.principalId, principalType: 'ServicePrincipal' }
}
resource workerResultsRole 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  scope: results
  name: guid(results.id, worker.id, blobContributor)
  properties: { roleDefinitionId: blobContributor, principalId: worker.identity.principalId, principalType: 'ServicePrincipal' }
}
resource backendSend 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  scope: queues[0]
  name: guid(queues[0].id, backend.id, senderRole)
  properties: { roleDefinitionId: senderRole, principalId: backend.identity.principalId, principalType: 'ServicePrincipal' }
}
resource workerReceive 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  scope: queues[0]
  name: guid(queues[0].id, worker.id, receiverRole)
  properties: { roleDefinitionId: receiverRole, principalId: worker.identity.principalId, principalType: 'ServicePrincipal' }
}
resource backendReceive 'Microsoft.Authorization/roleAssignments@2022-04-01' = [for index in [1, 2]: {
  scope: queues[index]
  name: guid(queues[index].id, backend.id, receiverRole)
  properties: { roleDefinitionId: receiverRole, principalId: backend.identity.principalId, principalType: 'ServicePrincipal' }
}]
resource workerSend 'Microsoft.Authorization/roleAssignments@2022-04-01' = [for index in [1, 2]: {
  scope: queues[index]
  name: guid(queues[index].id, worker.id, senderRole)
  properties: { roleDefinitionId: senderRole, principalId: worker.identity.principalId, principalType: 'ServicePrincipal' }
}]
resource backendNotificationSender 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  scope: notifications
  name: guid(notifications.id, backend.id, senderRole)
  properties: { roleDefinitionId: senderRole, principalId: backend.identity.principalId, principalType: 'ServicePrincipal' }
}
resource backendNotificationReceiver 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  scope: notificationSubscription
  name: guid(notificationSubscription.id, backend.id, receiverRole)
  properties: { roleDefinitionId: receiverRole, principalId: backend.identity.principalId, principalType: 'ServicePrincipal' }
}
resource backendSecrets 'Microsoft.Authorization/roleAssignments@2022-04-01' = [for index in [1, 2, 3, 5]: {
  scope: secrets[index]
  name: guid(secrets[index].id, backend.id, secretsReader)
  properties: { roleDefinitionId: secretsReader, principalId: backend.identity.principalId, principalType: 'ServicePrincipal' }
}]
resource workerSecrets 'Microsoft.Authorization/roleAssignments@2022-04-01' = [for index in [3, 4]: {
  scope: secrets[index]
  name: guid(secrets[index].id, worker.id, secretsReader)
  properties: { roleDefinitionId: secretsReader, principalId: worker.identity.principalId, principalType: 'ServicePrincipal' }
}]

resource swa 'Microsoft.Web/staticSites@2023-12-01' = if (frontendHosting == 'swa') {
  name: '${projectName}-front'
  location: location
  tags: tags
  sku: { name: 'Free', tier: 'Free' }
  properties: { allowConfigFileUpdates: true }
}

output endpoints object = {
  backendApp: backend.name
  workerApp: worker.name
  backendUrl: apiOrigin
  workerUrl: 'https://${workerName}.azurewebsites.net'
  frontendHosting: frontendHosting
  frontendUrl: frontendHosting == 'app-service' ? apiOrigin : 'https://${swa!.properties.defaultHostname}'
  keyVault: vault.name
  postgresServer: postgres.name
  postgresHost: postgres.properties.fullyQualifiedDomainName
  storageAccount: storage.name
  serviceBusNamespace: bus.name
}
