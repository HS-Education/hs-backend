"""Run the migration script against fake CLI layouts; never touch Azure or a DB."""
from pathlib import Path
import shutil
import subprocess
import unittest


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / 'infra/azure/Migrate-Database.ps1'

# All external commands, credentials and the public IP lookup are replaced.
# Execute the actual script, including its Students guard and finally block.
HARNESS = r"""
$ErrorActionPreference='Stop'
$global:layout='LAYOUT'
$global:failure='FAILURE'
$global:created=0; $global:deleted=0; $global:listed=0
$global:migrated=0; $global:secretsRead=0; $global:ruleExists=$false
$global:ruleName=$null; $global:ruleResourceId=$null
$env:GITHUB_ACTIONS='false'
$expectedSubscription='86d9e5e6-b9bf-44b4-915a-106207e0bc02'
foreach ($name in @('MIGRATION_DATABASE_URL','MIGRATION_DATABASE_USER','MIGRATION_DATABASE_PASSWORD','MIGRATION_APP_PASSWORD')) {
    [Environment]::SetEnvironmentVariable($name,$null,'Process')
}
function Get-FlagValue {
    param([object[]]$Arguments,[string]$Flag)
    $index=[array]::IndexOf($Arguments,$Flag)
    if ($index -lt 0 -or $index + 1 -ge $Arguments.Count) { throw "Missing mock flag: $Flag" }
    return $Arguments[$index+1]
}
function az {
    $arguments=@($args)
    $global:LASTEXITCODE=0
    if ($arguments -contains '--help') {
        if (($arguments[0..4] -join ' ') -ne 'postgres flexible-server firewall-rule create --help') { throw 'Unexpected help command.' }
        if ($global:failure -eq 'help') { $global:LASTEXITCODE=1; return }
        if ($global:layout -eq 'legacy') { return "    --name -n [Required] : Server`n    --rule-name -r : Rule" }
        if ($global:layout -eq 'current') { return "    --server-name -s [Required] : Server`n    --name -n : Rule" }
        return 'Unknown CLI flags.'
    }
    if ((Get-FlagValue $arguments '--subscription') -ne $expectedSubscription) { throw 'Operation escaped Students.' }
    if ($arguments[0] -eq 'rest') {
        if ((Get-FlagValue $arguments '--method') -ne 'get') { throw 'Unexpected ARM mutation.' }
        return (@{subscriptionId=$expectedSubscription; state='Enabled'; tenantId='0e0cb060-09ad-49f5-a005-68b9b49aa1f6';
            subscriptionPolicies=@{quotaId='AzureForStudents_2018-03-01'; spendingLimit='On'}} | ConvertTo-Json -Depth 4)
    }
    if (($arguments[0..2] -join ' ') -eq 'postgres flexible-server show') {
        if ((Get-FlagValue $arguments '--name') -ne 'hs-thesis-pg-offline') { throw 'Unexpected PostgreSQL target.' }
        return 'hs-thesis-pg-offline.postgres.database.azure.com'
    }
    if (($arguments[0..2] -join ' ') -eq 'keyvault secret show') {
        $global:secretsRead++
        if (!$global:ruleExists) { throw 'Credentials read before the firewall existed.' }
        if ((Get-FlagValue $arguments '--vault-name') -ne 'hskv-offline') { throw 'Unexpected vault.' }
        if ($global:failure -eq 'credentials') { $global:LASTEXITCODE=1; return }
        switch (Get-FlagValue $arguments '--name') {
            'postgres-password' { return 'offline-owner-test-password' }
            'postgres-app-password' { return 'offline-app-test-password' }
            default { throw 'Unexpected credential access.' }
        }
    }
    if (($arguments[0..2] -join ' ') -ne 'postgres flexible-server firewall-rule') { throw 'Unexpected Azure command.' }
    $serverFlag=if ($global:layout -eq 'current') {'--server-name'} else {'--name'}
    $ruleFlag=if ($global:layout -eq 'current') {'--name'} else {'--rule-name'}
    switch ($arguments[3]) {
        'create' {
            $global:created++
            if ((Get-FlagValue $arguments $serverFlag) -ne 'hs-thesis-pg-offline') { throw 'Wrong server argument.' }
            if ($global:layout -eq 'current' -and $arguments -contains '--rule-name') { throw 'Legacy flags sent to the current CLI.' }
            $global:ruleName=Get-FlagValue $arguments $ruleFlag
            if ($global:ruleName -cnotmatch '^cd-[0-9a-f]{32}$') { throw 'Unsafe temporary rule name.' }
            $global:ruleResourceId="/subscriptions/$expectedSubscription/resourceGroups/rg-hs-thesis-azure/providers/Microsoft.DBforPostgreSQL/flexibleServers/hs-thesis-pg-offline/firewallRules/$global:ruleName"
            foreach ($flag in @('--start-ip-address','--end-ip-address')) {
                if ((Get-FlagValue $arguments $flag) -ne '203.0.113.44') { throw 'The firewall must allow only the runner IP.' }
            }
            if ($global:failure -eq 'create') { $global:LASTEXITCODE=1; return }
            $global:ruleExists=$true
            if ($global:failure -eq 'create-throw') { throw 'Simulated creation exception.' }
            if ($global:failure -eq 'create-response') { $global:LASTEXITCODE=1 }
            return
        }
        'delete' {
            $global:deleted++
            if ((Get-FlagValue $arguments '--ids') -cne $global:ruleResourceId) { throw 'Cleanup targeted a different resource.' }
            if ($arguments -contains '--rule-name' -or $arguments -contains '--server-name' -or $arguments -contains '--name') { throw 'Cleanup must use the exact resource ID.' }
            if ($global:failure -in @('delete','migration-and-delete')) { $global:LASTEXITCODE=1; return }
            $global:ruleExists=$false
            if ($global:failure -eq 'delete-response') { $global:LASTEXITCODE=1 }
            return
        }
        'list' {
            $global:listed++
            if ((Get-FlagValue $arguments $serverFlag) -ne 'hs-thesis-pg-offline') { throw 'Cleanup verification used the wrong server.' }
            if ((Get-FlagValue $arguments '--query') -cne "[?name=='$global:ruleName'].name") { throw 'Cleanup verification must select only this rule.' }
            if ($global:failure -eq 'list') { $global:LASTEXITCODE=1; return }
            if ($global:failure -eq 'list-json') { return 'invalid json' }
            if ($global:failure -eq 'list-null') { return 'null' }
            if ($global:ruleExists) { return (ConvertTo-Json -InputObject @($global:ruleName) -Compress) }
            return '[]'
        }
        default { throw 'Unexpected firewall operation.' }
    }
}
function Invoke-RestMethod {
    param([string]$Uri)
    if ($Uri -ne 'https://api.ipify.org') { throw 'Unexpected network request.' }
    if ($global:failure -eq 'ipv6') { return '2001:db8::1' }
    if ($global:failure -eq 'zero-ip') { return '0.0.0.0' }
    return '203.0.113.44'
}
function java {
    $global:migrated++
    if (!$global:ruleExists -or $global:secretsRead -ne 2) { throw 'Migration ran before firewall and credentials.' }
    if ($env:MIGRATION_DATABASE_USER -ne 'thesis_owner' -or $env:MIGRATION_DATABASE_PASSWORD -ne 'offline-owner-test-password' -or
        $env:MIGRATION_APP_PASSWORD -ne 'offline-app-test-password') { throw 'Separate migration credentials were not supplied.' }
    if ($env:MIGRATION_DATABASE_URL -notmatch 'sslmode=verify-full' -or $env:MIGRATION_DATABASE_URL -notmatch 'hs-thesis-pg-offline.postgres.database.azure.com') { throw 'Migration must verify PostgreSQL TLS.' }
    if (@($args) -notcontains '-Dloader.main=com.hs.hstesis.shared.infrastructure.azure.DatabaseMigrationMain') { throw 'Unexpected migration entry point.' }
    if ($global:failure -eq 'migration-throw') { throw 'Simulated migration exception.' }
    $global:LASTEXITCODE=if ($global:failure -in @('migration','migration-and-delete')) {1} else {0}
}
$caught=$null
try {
    & 'SCRIPT_PATH' -ResourceGroup 'rg-hs-thesis-azure' -PostgresServer 'hs-thesis-pg-offline' -KeyVault 'hskv-offline' -Jar 'offline-app.jar'
} catch { $caught=$_.Exception.Message }
if ('EXPECTED_ERROR' -eq '') {
    if ($caught) { throw "Unexpected script failure: $caught" }
} elseif (!$caught -or !$caught.Contains('EXPECTED_ERROR')) { throw "Expected error was not preserved: $caught" }
if ($global:created -ne EXPECTED_CREATE -or $global:deleted -ne EXPECTED_CREATE -or $global:listed -ne EXPECTED_CREATE) {
    throw "Unexpected firewall lifecycle: $global:created / $global:deleted / $global:listed"
}
if ($global:migrated -ne EXPECTED_MIGRATION) { throw 'Unexpected migration count.' }
if ($global:created -eq 0 -and $global:secretsRead -ne 0) { throw 'Credentials accessed before capability/IP checks.' }
foreach ($name in @('MIGRATION_DATABASE_URL','MIGRATION_DATABASE_USER','MIGRATION_DATABASE_PASSWORD','MIGRATION_APP_PASSWORD')) {
    if ([Environment]::GetEnvironmentVariable($name,'Process')) { throw "Migration environment leaked: $name" }
}
if ($global:failure -eq 'migration-and-delete' -and !$caught.Contains('cleanup could not be confirmed')) { throw 'Cleanup failure context was lost.' }
Write-Output 'Offline migration lifecycle verified.'
"""


@unittest.skipUnless(shutil.which('pwsh'), 'PowerShell is required for migration behavioral tests')
class MigrationFirewallCliTest(unittest.TestCase):
    def run_case(self, layout='current', failure='none', error='', creates=1, migrations=1):
        command = HARNESS.replace('SCRIPT_PATH', str(SCRIPT).replace("'", "''"))
        for marker, value in {
            'LAYOUT': layout, 'FAILURE': failure, 'EXPECTED_ERROR': error,
            'EXPECTED_CREATE': str(creates), 'EXPECTED_MIGRATION': str(migrations),
        }.items():
            command = command.replace(marker, value)
        result = subprocess.run([shutil.which('pwsh'), '-NoProfile', '-NonInteractive', '-Command', command],
                                capture_output=True, text=True, timeout=30)
        self.assertEqual(result.returncode, 0, result.stderr + result.stdout)
        self.assertIn('Offline migration lifecycle verified.', result.stdout)
        self.assertNotIn('offline-owner-test-password', result.stdout + result.stderr)
        self.assertNotIn('offline-app-test-password', result.stdout + result.stderr)

    def test_current_cli_creates_migrates_and_verifies_exact_rule_cleanup(self):
        self.run_case()

    def test_legacy_cli_creates_migrates_and_verifies_exact_rule_cleanup(self):
        self.run_case(layout='legacy')

    def test_unknown_cli_fails_before_mutations_or_credentials(self):
        self.run_case(layout='unknown', error='Unsupported PostgreSQL firewall CLI syntax', creates=0, migrations=0)

    def test_unreadable_help_fails_before_mutations_or_credentials(self):
        self.run_case(failure='help', error='Cannot determine the PostgreSQL firewall CLI syntax', creates=0, migrations=0)

    def test_ipv6_runner_cannot_open_the_firewall(self):
        self.run_case(failure='ipv6', error='nonzero IPv4 runner address', creates=0, migrations=0)

    def test_zero_address_cannot_enable_broad_azure_access(self):
        self.run_case(failure='zero-ip', error='broad Azure access is forbidden', creates=0, migrations=0)

    def test_creation_failure_preserves_primary_error_and_verifies_absence(self):
        self.run_case(failure='create', error='Temporary runner firewall rule could not be created', migrations=0)

    def test_lost_creation_response_still_cleans_up_the_created_rule(self):
        self.run_case(failure='create-response', error='Temporary runner firewall rule could not be created', migrations=0)

    def test_creation_exception_still_cleans_up_the_created_rule(self):
        self.run_case(failure='create-throw', error='Simulated creation exception', migrations=0)

    def test_credential_failure_still_cleans_up_without_migrating(self):
        self.run_case(failure='credentials', error='Database owner credential read failed', migrations=0)

    def test_migration_failure_cleans_up_and_clears_credentials(self):
        self.run_case(failure='migration', error='Database migration failed')

    def test_migration_exception_cleans_up_and_clears_credentials(self):
        self.run_case(failure='migration-throw', error='Simulated migration exception')

    def test_remaining_rule_blocks_continuation_after_successful_migration(self):
        self.run_case(failure='delete', error='cleanup could not be confirmed')

    def test_cleanup_failure_does_not_hide_the_migration_failure(self):
        self.run_case(failure='migration-and-delete', error='Database migration failed')

    def test_lost_delete_response_is_safe_only_after_absence_is_verified(self):
        self.run_case(failure='delete-response')

    def test_failed_absence_verification_blocks_continuation(self):
        self.run_case(failure='list', error='cleanup could not be confirmed')

    def test_malformed_absence_verification_blocks_continuation(self):
        self.run_case(failure='list-json', error='cleanup could not be confirmed')

    def test_null_response_is_not_proof_of_rule_absence(self):
        self.run_case(failure='list-null', error='cleanup could not be confirmed')


if __name__ == '__main__':
    unittest.main()
