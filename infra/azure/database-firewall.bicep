targetScope = 'resourceGroup'
param serverName string
param outboundIps array
resource server 'Microsoft.DBforPostgreSQL/flexibleServers@2024-08-01' existing = { name: serverName }
resource rules 'Microsoft.DBforPostgreSQL/flexibleServers/firewallRules@2024-08-01' = [for (ip, index) in outboundIps: {
  parent: server
  name: 'app-outbound-${index}'
  properties: { startIpAddress: ip, endIpAddress: ip }
}]
