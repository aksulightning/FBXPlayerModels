"""One-date Modrinth alpha release: time gate, artifact preflight, duplicate protection."""
from datetime import datetime, timezone
import hashlib
import json
import os
import sys
from urllib.parse import quote
from urllib.request import Request, urlopen
from zoneinfo import ZoneInfo

from release_metadata import ROOT, TARGETS, metadata, output, verify_bundle

RELEASE_TIME = datetime(2026, 11, 2, 13, 0, tzinfo=ZoneInfo('Europe/Helsinki'))


def in_release_window(now):
    local = now.astimezone(RELEASE_TIME.tzinfo)
    return local.date() == RELEASE_TIME.date() and local >= RELEASE_TIME


def require_release_window():
    if not in_release_window(datetime.now(timezone.utc)):
        raise RuntimeError('This automatic release is authorized only on 2026-11-02, from 13:00 Helsinki time')


def already_published(row, versions, sha512):
    matches = [version for version in versions if version['version_number'] == row['version']]
    if not matches:
        return False
    if len(matches) != 1:
        raise RuntimeError(f"Multiple existing versions named {row['version']}; refusing automatic publication")
    existing = matches[0]
    files = existing.get('files', [])
    identical = (
        existing.get('version_type') == 'alpha'
        and existing.get('status') == 'listed'
        and set(existing.get('loaders', [])) == {row['loader']}
        and set(existing.get('game_versions', [])) == {row['minecraft']}
        and len(files) == 1
        and files[0].get('hashes', {}).get('sha512') == sha512
    )
    if not identical:
        raise RuntimeError(f"Existing version {row['version']} differs from this artifact; nothing will be replaced")
    return True


def preflight():
    require_release_window()
    project = os.environ.get('MODRINTH_PROJECT', '').strip()
    token = os.environ.get('MODRINTH_TOKEN', '').strip()
    if not project or not token:
        raise RuntimeError('Configure MODRINTH_ID and MODRINTH_TOKEN before the timed release')
    verified = []
    for target in TARGETS:
        expected = metadata(target)
        download = ROOT / '.release' / expected['artifact']
        bundle = ROOT / '.release' / target
        download.rename(bundle)
        os.environ['RELEASE_VERSION'] = expected['release_version']
        verified.append(dict(expected, **verify_bundle(target)))
    request = Request(
        f'https://api.modrinth.com/v2/project/{quote(project, safe="")}/version?include_changelog=false',
        headers={'Authorization': token, 'User-Agent': 'aksulightning/FBXPlayerModels one-time-release'},
    )
    with urlopen(request, timeout=30) as response:
        versions = json.load(response)
    pending = []
    for row in verified:
        digest = hashlib.sha512((ROOT / row['file']).read_bytes()).hexdigest()
        if not already_published(row, versions, digest):
            pending.append(row)
    output('matrix', json.dumps({'include': pending}))
    output('has_pending', str(bool(pending)).lower())
    with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as stream:
        stream.write(f'All four artifacts verified. {len(pending)} alpha versions remain to publish.\n')


def main():
    operation = sys.argv[1]
    if operation == 'gate':
        eligible = in_release_window(datetime.now(timezone.utc))
        output('eligible', str(eligible).lower())
        output('matrix', json.dumps({'include': [metadata(target) for target in TARGETS]}))
        return
    if operation == 'preflight':
        preflight()
        return
    if operation != 'verify':
        raise ValueError('Unknown scheduled release operation')
    require_release_window()
    for key, value in verify_bundle(sys.argv[2]).items():
        output(key, value)


if __name__ == '__main__':
    main()
