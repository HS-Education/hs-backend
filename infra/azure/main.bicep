targetScope = 'subscription'

@description('Explicit verified Azure for Students subscription. Never infer the CLI default.')
param studentsSubscriptionId string
@description('Region permitted by the Students policy; keep all services in the same region.')
param location string = 'mexicocentral'
param projectName string = 'hs-thesis'
@allowed(['app-service', 'swa'])
param frontendHosting string = 'app-service'
param frontendOrigin string = ''
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
param bootstrapAdminUsername string = 'thesis_admin'
param budgetEmail string = ''
param monthlyBudgetUsd int = 60
param budgetStartDate string = utcNow('yyyy-MM-01T00:00:00Z')

var correctSubscription = subscription().subscriptionId == studentsSubscriptionId

resource group 'Microsoft.Resources/resourceGroups@2024-03-01' = if (correctSubscription) {
  name: 'rg-${projectName}-azure'
  location: location
  tags: { project: projectName, environment: 'thesis', offer: 'Azure-for-Students' }
}

module resources './resources.bicep' = if (correctSubscription) {
  name: 'hs-students-resources'
  scope: resourceGroup('rg-${projectName}-azure')
  params: {
    location: location
    projectName: projectName
    frontendHosting: frontendHosting
    frontendOrigin: frontendOrigin
    postgresPassword: postgresPassword
    postgresAppPassword: postgresAppPassword
    jwtSecret: jwtSecret
    workerApiKey: workerApiKey
    openrouterApiKey: openrouterApiKey
    bootstrapAdminPassword: bootstrapAdminPassword
    bootstrapAdminUsername: bootstrapAdminUsername
  }
  dependsOn: [group]
}

// A budget alerts; it is not an automatic spending cap. The Students spending limit stays on.
resource budget 'Microsoft.Consumption/budgets@2023-11-01' = if (correctSubscription && !empty(budgetEmail)) {
  name: '${projectName}-monthly'
  properties: {
    category: 'Cost'
    amount: monthlyBudgetUsd
    timeGrain: 'Monthly'
    timePeriod: { startDate: budgetStartDate, endDate: dateTimeAdd(budgetStartDate, 'P1Y') }
    filter: { dimensions: { name: 'ResourceGroupName', operator: 'In', values: ['rg-${projectName}-azure'] } }
    notifications: {
      actual80: { enabled: true, operator: 'GreaterThanOrEqualTo', threshold: 80, thresholdType: 'Actual', contactEmails: [budgetEmail] }
      actual100: { enabled: true, operator: 'GreaterThanOrEqualTo', threshold: 100, thresholdType: 'Actual', contactEmails: [budgetEmail] }
    }
  }
}

output subscriptionGuardPassed bool = correctSubscription
output resourceGroupName string = correctSubscription ? group!.name : ''
output endpoints object = correctSubscription ? resources!.outputs.endpoints : {}
