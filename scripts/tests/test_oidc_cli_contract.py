"""Guard the Students-scoped, secretless deployment identity contract."""
from pathlib import Path
import shutil
import subprocess
import unittest


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / 'infra/azure/Configure-GitHubOidc.ps1'
TEMPLATE = ROOT / 'infra/azure/github-oidc.bicep'


class OidcCliContractTest(unittest.TestCase):
    def setUp(self):
        self.source = SCRIPT.read_text(encoding='utf-8')
        self.template = TEMPLATE.read_text(encoding='utf-8')

    def test_arm_identity_avoids_directory_permissions_and_client_secrets(self):
        self.assertIn('Microsoft.ManagedIdentity/userAssignedIdentities@', self.template)
        self.assertIn('github-oidc.bicep', self.source)
        self.assertNotRegex(self.source, r'(?m)^\s*[^#\n]*&\s*az\s+ad\s')
        self.assertNotIn('graph.microsoft.com', self.source)
        self.assertNotIn('passwordCredentials', self.source + self.template)
        self.assertNotIn('addPassword', self.source + self.template)

    def test_federation_is_bound_to_the_protected_backend_environment(self):
        for expected in ('repo:HS-Education/hs-backend:environment:azure-students',
                         'https://token.actions.githubusercontent.com', 'api://AzureADTokenExchange'):
            self.assertIn(expected, self.template)
            self.assertIn(expected, self.source)
        self.assertIn('Existing OIDC federation does not match the protected environment', self.source)
        self.assertIn('Unexpected extra OIDC federations', self.source)

    def test_identity_and_resource_guards_precede_mutations(self):
        deploy = self.source.index('& az deployment group create')
        self.assertLess(self.source.index('Get-StudentsAccount -SubscriptionId $subscription'), deploy)
        self.assertLess(self.source.index('if (!$ApproveIdentityChanges)'), deploy)
        self.assertLess(self.source.index('The App Service targets must exist'), deploy)
        self.assertLess(self.source.index('Assert-CdFederations -Federations'), deploy)
        self.assertIn('mexicocentral', self.template)

    def test_roles_stay_scoped_and_never_grant_subscription_write(self):
        self.assertIn("role = 'Reader'; scope = \"/subscriptions/$subscription\"", self.source)
        self.assertNotIn("role = 'Owner'", self.source)
        self.assertNotIn("role = 'Contributor'", self.source)
        self.assertEqual(self.source.count("role = 'Website Contributor'"), 2)
        self.assertEqual(self.source.count("role = 'Key Vault Secrets User'"), 2)
        self.assertIn('--assignee-object-id $principalId --assignee-principal-type ServicePrincipal', self.source)

    @unittest.skipUnless(shutil.which('pwsh'), 'PowerShell is required for behavioral guard checks')
    def test_existing_federation_guard_behavior_offline(self):
        source_path = str(SCRIPT).replace("'", "''")
        command = r"""
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$tokens=$null; $errors=$null
$ast=[System.Management.Automation.Language.Parser]::ParseFile('SOURCE_PATH',[ref]$tokens,[ref]$errors)
if ($errors) { throw 'OIDC script syntax errors.' }
$function=$ast.Find({param($node) $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq 'Assert-CdFederations'},$true)
. ([scriptblock]::Create($function.Extent.Text))
$valid=@{name='hs-backend-azure-students'; issuer='https://token.actions.githubusercontent.com'; subject='repo:HS-Education/hs-backend:environment:azure-students'; audiences=@('api://AzureADTokenExchange')}
Assert-CdFederations -Federations @()
Assert-CdFederations -Federations @([pscustomobject]$valid)
foreach ($field in @('name','issuer','subject','audiences')) {
    $invalid=$valid.Clone()
    $invalid[$field]=if ($field -eq 'audiences') {@('wrong-audience')} else {'wrong-value'}
    $rejected=$false
    try { Assert-CdFederations -Federations @([pscustomobject]$invalid) } catch { $rejected=$true }
    if (!$rejected) { throw "Untrusted federation accepted: $field" }
}
$rejected=$false
try { Assert-CdFederations -Federations @([pscustomobject]$valid,[pscustomobject]$valid) } catch { $rejected=$true }
if (!$rejected) { throw 'Extra federation accepted.' }
""".replace('SOURCE_PATH', source_path)
        result = subprocess.run([shutil.which('pwsh'), '-NoProfile', '-NonInteractive', '-Command', command],
                                capture_output=True, text=True, timeout=30)
        self.assertEqual(result.returncode, 0, result.stderr)


if __name__ == '__main__':
    unittest.main()
