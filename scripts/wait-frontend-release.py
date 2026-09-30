"""Wait for the public, matching frontend assets without cross-repository write tokens."""
import json
import os
import re
import time
import urllib.error
import urllib.request


REPOSITORY = 'HS-Education/hs-front'
ASSETS = frozenset(('frontend.zip', 'frontend-manifest.json'))


def fetch_release(tag):
    request = urllib.request.Request(
        f'https://api.github.com/repos/{REPOSITORY}/releases/tags/{tag}',
        headers={'Accept': 'application/vnd.github+json',
                 'Authorization': 'Bearer ' + os.environ['GH_TOKEN'],
                 'X-GitHub-Api-Version': '2022-11-28',
                 'User-Agent': 'hs-thesis-release-readiness'})
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        if error.code in (404, 429, 500, 502, 503, 504):
            return None
        raise RuntimeError(f'Frontend release lookup failed (HTTP {error.code}); response suppressed.') from None
    except (urllib.error.URLError, TimeoutError):
        return None


def release_is_ready(release, tag):
    if release is None:
        return False
    if release.get('tag_name') != tag:
        raise ValueError('The frontend release tag does not match the deployment.')
    if release.get('draft') or release.get('prerelease'):
        return False
    uploaded = {asset.get('name') for asset in release.get('assets', [])
                if asset.get('state') == 'uploaded' and asset.get('size', 0) > 0}
    return ASSETS.issubset(uploaded)


def wait_for_release(tag, timeout_seconds=1800, poll_interval=30, *,
                     fetch=fetch_release, clock=time.monotonic, sleep=time.sleep):
    if not re.fullmatch(r'v[0-9]+\.[0-9]+\.[0-9]+', tag):
        raise ValueError('Only stable semantic release tags are accepted.')
    if timeout_seconds <= 0 or poll_interval <= 0:
        raise ValueError('The release wait must have positive bounds.')
    deadline = clock() + timeout_seconds
    print('Waiting up to 30 minutes for the matching frontend package; approvals remain required.')
    while clock() < deadline:
        if release_is_ready(fetch(tag), tag):
            print('Both matching frontend assets are uploaded; integrity checks follow before deployment.')
            return
        remaining = deadline - clock()
        if remaining > 0:
            sleep(min(poll_interval, remaining))
    raise TimeoutError('Matching frontend assets did not become ready. Check its tag, approval and release run; nothing was deployed.')


if __name__ == '__main__':
    wait_for_release(os.environ['RELEASE_TAG'])
