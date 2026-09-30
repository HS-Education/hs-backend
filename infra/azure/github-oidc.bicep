targetScope = 'resourceGroup'

// CD has its own identity; do not reuse either application's runtime identity.
resource cdIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: 'hs-thesis-github-cd'
  location: 'mexicocentral'
  tags: {
    application: 'hs-thesis'
    purpose: 'protected-github-cd'
  }
}

resource backendFederation 'Microsoft.ManagedIdentity/userAssignedIdentities/federatedIdentityCredentials@2023-01-31' = {
  parent: cdIdentity
  name: 'hs-backend-azure-students'
  properties: {
    issuer: 'https://token.actions.githubusercontent.com'
    subject: 'repo:HS-Education/hs-backend:environment:azure-students'
    audiences: [
      'api://AzureADTokenExchange'
    ]
  }
}

output identity object = {
  id: cdIdentity.id
  clientId: cdIdentity.properties.clientId
  principalId: cdIdentity.properties.principalId
  tenantId: cdIdentity.properties.tenantId
}
