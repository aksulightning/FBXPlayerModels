"""Artifact verification and destination-specific helpers for manual release workflows."""
import hashlib
import json
import os
import subprocess
import sys
from urllib.parse import quote
from urllib.request import Request, urlopen

from release_metadata import ROOT, TARGETS, metadata, output, target_release_note, verify_bundle


def selected_targets(target):
    if target == 'all':
        return list(TARGETS)
    if target not in TARGETS:
        raise ValueError(f'Unknown release target: {target}')
    return [target]


def verify_downloads(target):
    verified = []
    for item in selected_targets(target):
        expected = metadata(item)
        download = ROOT / '.release' / expected['artifact']
        bundle = ROOT / '.release' / item
        # download-artifact keeps each artifact in its own named directory.
        if download.is_dir():
            if bundle.exists():
                raise ValueError(f'Conflicting artifact directories for {item}')
            download.rename(bundle)
        os.environ['RELEASE_VERSION'] = expected['release_version']
        verified.append(dict(expected, **verify_bundle(item)))
    return verified


def already_published(row, versions, sha512, channel):
    matches = [version for version in versions if version['version_number'] == row['version']]
    if not matches:
        return False
    if len(matches) != 1:
        raise RuntimeError(f"Multiple existing versions named {row['version']}; refusing duplicate publication")
    existing = matches[0]
    files = existing.get('files', [])
    identical = (
        existing.get('version_type') == channel
        and existing.get('status') == 'listed'
        and set(existing.get('loaders', [])) == {row['loader']}
        and set(existing.get('game_versions', [])) == {row['minecraft']}
        and len(files) == 1
        and files[0].get('hashes', {}).get('sha512') == sha512
    )
    if not identical:
        raise RuntimeError(f"Existing version {row['version']} differs from this artifact; nothing will be replaced")
    return True


def require_modrinth_config():
    project = os.environ.get('MODRINTH_PROJECT', '').strip()
    token = os.environ.get('MODRINTH_TOKEN', '').strip()
    if not project or not token:
        raise RuntimeError('Configure repository variable MODRINTH_ID and secret MODRINTH_TOKEN')
    return project, token


def modrinth_preflight(target, channel):
    project, token = require_modrinth_config()
    rows = verify_downloads(target)
    request = Request(
        f'https://api.modrinth.com/v2/project/{quote(project, safe="")}/version?include_changelog=false',
        headers={'Authorization': token, 'User-Agent': 'aksulightning/FBXPlayerModels manual-release'},
    )
    with urlopen(request, timeout=30) as response:
        versions = json.load(response)
    pending = []
    for row in rows:
        digest = hashlib.sha512((ROOT / row['file']).read_bytes()).hexdigest()
        if not already_published(row, versions, digest, channel):
            pending.append(row)
    output('matrix', json.dumps({'include': pending}))
    output('has_pending', str(bool(pending)).lower())
    with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as stream:
        stream.write(f'Verified {len(rows)} artifacts; {len(pending)} {channel} versions remain to publish.\n')


def validate_github_tag(tag):
    if not tag or tag.startswith('-'):
        raise ValueError('Supply a new GitHub release tag, for example v2.0.0-lan')
    subprocess.run(['git', 'check-ref-format', f'refs/tags/{tag}'], check=True)


def github_release(target, channel):
    rows = verify_downloads(target)
    tag = os.environ.get('RELEASE_TAG', '').strip()
    validate_github_tag(tag)
    repo = os.environ['GITHUB_REPOSITORY']
    matches = json.loads(subprocess.check_output([
        'gh', 'api', f"repos/{repo}/git/matching-refs/tags/{quote(tag, safe='')}"
    ]))
    if any(item['ref'] == f'refs/tags/{tag}' for item in matches):
        raise ValueError('Tag already exists. Choose a new tag; existing releases and tags are never replaced.')
    notes = os.environ.get('RELEASE_NOTES', '').strip()
    for row in rows:
        target_notes = target_release_note(row['target'])
        if target_notes:
            notes = '\n\n'.join(part for part in [notes, target_notes] if part)
    notes += f"\n\nSource commit: {os.environ['BUILD_COMMIT']}\n\nSHA-256:\n"
    for row in rows:
        manifest = json.loads((ROOT / '.release' / row['target'] / 'release-manifest.json').read_text())
        notes += f"- {row['filename']}: `{manifest['sha256']}`\n"
    notes_file = ROOT / '.release/release-notes.md'
    notes_file.write_text(notes)
    args = ['gh', 'release', 'create', tag, *[row['file'] for row in rows],
            '--repo', repo, '--target', os.environ['BUILD_COMMIT'],
            '--title', f'FBX Player Models {tag}', '--notes-file', str(notes_file)]
    if channel != 'release':
        args.append('--prerelease')
    subprocess.run(args, check=True, cwd=ROOT)


def main():
    operation = sys.argv[1]
    if operation == 'modrinth-config':
        require_modrinth_config()
        print('Modrinth publishing configuration is present.')
        return
    if operation == 'github-config':
        validate_github_tag(os.environ.get('RELEASE_TAG', '').strip())
        return
    channel = os.environ.get('RELEASE_CHANNEL', 'alpha')
    if channel not in {'alpha', 'beta', 'release'}:
        raise ValueError('Invalid release channel')
    target = sys.argv[2]
    if operation == 'modrinth-preflight':
        modrinth_preflight(target, channel)
    elif operation == 'github':
        github_release(target, channel)
    elif operation == 'verify':
        if target not in TARGETS:
            raise ValueError('Verify accepts exactly one target')
        os.environ['RELEASE_VERSION'] = metadata(target)['release_version']
        for key, value in verify_bundle(target).items():
            output(key, value)
    else:
        raise ValueError('Unknown release operation')


if __name__ == '__main__':
    main()
