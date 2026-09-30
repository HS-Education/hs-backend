"""Exercise bounded asset coordination and the protected tag-driven workflow contract."""
import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch
import urllib.error


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location('wait_release', ROOT / 'scripts/wait-frontend-release.py')
WAIT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(WAIT)


def ready(tag='v0.2.2'):
    return {'tag_name': tag, 'draft': False, 'prerelease': False,
            'assets': [{'name': name, 'state': 'uploaded', 'size': 10} for name in WAIT.ASSETS]}


class TagDrivenCdTest(unittest.TestCase):
    def test_both_nonempty_uploaded_assets_are_required(self):
        self.assertFalse(WAIT.release_is_ready(None, 'v0.2.2'))
        self.assertTrue(WAIT.release_is_ready(ready(), 'v0.2.2'))
        for field, value in (('state', 'open'), ('size', 0)):
            release = ready()
            release['assets'][0][field] = value
            self.assertFalse(WAIT.release_is_ready(release, 'v0.2.2'))
        release = ready()
        release['assets'].pop()
        self.assertFalse(WAIT.release_is_ready(release, 'v0.2.2'))

    def test_drafts_and_prereleases_never_authorize_deployment(self):
        for field in ('draft', 'prerelease'):
            release = ready()
            release[field] = True
            self.assertFalse(WAIT.release_is_ready(release, 'v0.2.2'))
        with self.assertRaises(ValueError):
            WAIT.release_is_ready(ready('v0.2.1'), 'v0.2.2')

    def test_bounded_wait_allows_tags_and_approvals_in_either_order(self):
        ticks = [0]
        responses = iter((None, {'tag_name': 'v0.2.2', 'assets': []}, ready()))
        WAIT.wait_for_release('v0.2.2', 10, 2, fetch=lambda _: next(responses),
                              clock=lambda: ticks[0], sleep=lambda delay: ticks.__setitem__(0, ticks[0] + delay))
        self.assertEqual(ticks[0], 4)

    def test_missing_frontend_times_out_without_infinite_polling(self):
        ticks = [0]
        with self.assertRaises(TimeoutError):
            WAIT.wait_for_release('v0.2.2', 5, 2, fetch=lambda _: None,
                                  clock=lambda: ticks[0], sleep=lambda delay: ticks.__setitem__(0, ticks[0] + delay))
        self.assertEqual(ticks[0], 5)

    def test_invalid_tags_and_bounds_fail_before_network_access(self):
        for tag in ('main', 'v0.2.2-rc1', 'v0.2.2/path', 'v0.2'):
            with self.assertRaises(ValueError):
                WAIT.wait_for_release(tag, fetch=lambda _: self.fail('Unexpected network access'))
        for timeout, interval in ((0, 1), (1, 0)):
            with self.assertRaises(ValueError):
                WAIT.wait_for_release('v0.2.2', timeout, interval)

    def test_transient_http_errors_retry_but_authorization_errors_fail(self):
        with patch.dict(WAIT.os.environ, {'GH_TOKEN': 'offline-test-token'}):
            for status in (404, 429, 503):
                error = urllib.error.HTTPError('https://api.github.com/', status, 'suppressed', None, None)
                with patch.object(WAIT.urllib.request, 'urlopen', side_effect=error):
                    self.assertIsNone(WAIT.fetch_release('v0.2.2'))
            for status in (401, 403):
                error = urllib.error.HTTPError('https://api.github.com/', status, 'suppressed', None, None)
                with patch.object(WAIT.urllib.request, 'urlopen', side_effect=error):
                    with self.assertRaisesRegex(RuntimeError, 'response suppressed'):
                        WAIT.fetch_release('v0.2.2')

    def test_tag_event_and_manual_fallback_use_the_actual_tag(self):
        source = (ROOT / '.github/workflows/azure-cd.yml').read_text(encoding='utf-8')
        self.assertIn("tags: ['v*']", source)
        self.assertIn('workflow_dispatch:', source)
        self.assertIn("github.event_name == 'push' || github.ref_name == inputs.release_tag", source)
        self.assertNotIn('ref: ${{ inputs.release_tag }}', source)
        self.assertIn('RELEASE_TAG: ${{ github.ref_name }}', source)
        self.assertIn("vars.AZURE_CD_ENABLED == 'true'", source)

    def test_wait_precedes_deploy_and_smoke_follows_successful_deploy(self):
        source = (ROOT / '.github/workflows/azure-cd.yml').read_text(encoding='utf-8')
        self.assertLess(source.index('run: python scripts/wait-frontend-release.py'),
                        source.index('gh release download'))
        self.assertIn('needs: verify', source)
        self.assertIn('browser-smoke:\n    needs: deploy', source)
        deploy = source.split('  deploy:', 1)[1].split('  browser-smoke:', 1)[0]
        smoke = source.split('  browser-smoke:', 1)[1]
        for job in (deploy, smoke):
            self.assertIn('environment: azure-students', job)
        self.assertLess(deploy.index('Require smoke credentials'), deploy.index('Migrate-Database.ps1'))
        self.assertNotIn('id-token: write', smoke)
        self.assertIn('[[ "$(git rev-parse HEAD)" == "$EXPECTED_SHA" ]]', smoke)
        self.assertIn('git merge-base --is-ancestor HEAD origin/main', smoke)
        self.assertIn('../deployment/frontend-release/frontend-manifest.json', smoke)
        self.assertIn('git -C target/frontend-source cat-file -e', source)
        self.assertLess(smoke.index('node scripts/assert-cloud-smoke-target.cjs'),
                        smoke.index('pnpm exec playwright test'))
        self.assertNotIn('repository_dispatch', source)


if __name__ == '__main__':
    unittest.main()
