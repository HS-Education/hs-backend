param([string] $Region = 'mexicocentral')
$ErrorActionPreference = 'Stop'
if ($Region -notmatch '^[a-z0-9]+$') { throw 'Invalid Azure region.' }
# Public retail API only: no login, no resource creation, no subscription or secret values.
foreach ($product in @('Azure App Service Basic Plan - Linux', 'Azure Database for PostgreSQL Flexible Server Burstable BS Series Compute',
    'Azure Database for PostgreSQL Flex Server Storage', 'Service Bus', 'General Block Blob v2', 'Key Vault', 'Log Analytics')) {
    $filter = "armRegionName eq '$Region' and productName eq '$product' and priceType eq 'Consumption'"
    $uri = 'https://prices.azure.com/api/retail/prices?api-version=2023-01-01-preview&currencyCode=USD&$filter=' + [uri]::EscapeDataString($filter)
    do {
        $page = $null
        for ($attempt = 0; $attempt -lt 3; $attempt++) {
            try { $page = Invoke-RestMethod -Uri $uri -TimeoutSec 30; break }
            catch {
                if ($attempt -eq 2) { throw }
                Start-Sleep -Seconds (10 * ($attempt + 1))
            }
        }
        $page.Items | Where-Object { $_.skuName -in @('B2','B1MS','Storage','Standard','Hot LRS','Analytics Logs') -and
            $_.meterName -in @('B2','B1MS','Storage Data Stored','Standard Base Unit','Hot LRS Data Stored','Hot Write Operations','Hot Read Operations','Operations','Analytics Logs Data Ingestion') } |
            Select-Object serviceName, productName, skuName, meterName, retailPrice, unitOfMeasure, currencyCode, effectiveStartDate
        $uri = $page.NextPageLink
    } while ($uri)
}
