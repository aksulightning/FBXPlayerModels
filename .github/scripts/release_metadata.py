"""Target definitions and artifact provenance shared by the release workflows."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import sys
import uuid

ROOT = Path(__file__).resolve().parents[2]
CLIENT_FABRIC_TARGET = 'fabric-1.21.1-client'
CLIENT_FABRIC_TARGETS = {CLIENT_FABRIC_TARGET, 'fabric-26.3-client'}
TARGETS = {
    'fabric-1.21.1': ('1.21.1', 'fabric', '21', 'remapJar', 'fabric_1211_mod_version', 'nightly-1.21.1'),
    CLIENT_FABRIC_TARGET: ('1.21.1', 'fabric', '21', 'remapJar', 'fabric_1211_client_mod_version', 'nightly-fabric-client-1.21.1'),
    'fabric-26.3': ('26.3', 'fabric', '25', 'jar', 'fabric_263_mod_version', 'nightly-26.3'),
    'fabric-26.3-client': ('26.3', 'fabric', '25', 'jar', 'fabric_263_client_mod_version', 'nightly-fabric-client-26.3'),
    'forge-1.21.1-client': ('1.21.1', 'forge', '21', 'jarJar', 'forge_1211_mod_version', 'nightly-forge-1.21.1'),
    'fabric-b1.7.3-client': ('b1.7.3', 'babric', '21', 'remapJar', 'fabric_b173_mod_version', 'nightly-b1.7.3'),
}


def metadata(target):
    minecraft, loader, java, task, version_key, tag = TARGETS[target]
    properties = {}
    for line in (ROOT / 'gradle.properties').read_text().splitlines():
        if '=' in line and not line.lstrip().startswith('#'):
            key, value = line.split('=', 1)
            properties[key.strip()] = value.strip()
    base = properties['archives_base_name']
    version = properties[version_key]
    variant = '-client' if target in CLIENT_FABRIC_TARGETS else '-forge' if loader == 'forge' else ''
    filename = f"{base}{variant}-v{version}+mc{minecraft}.jar"
    return {
        'target': target, 'minecraft': minecraft, 'loader': loader, 'java': java,
        'task': f':{target}:{task}', 'filename': filename,
        'mod_version': version, 'release_version': version.split('+', 1)[0],
        'artifact': f'fbx-player-models-{target}', 'proposed_tag': tag,
    }


def output(key, value):
    with open(os.environ['GITHUB_OUTPUT'], 'a') as stream:
        if '\n' in value or '\r' in value:
            delimiter = f'release_output_{uuid.uuid4().hex}'
            stream.write(f'{key}<<{delimiter}\n{value}\n{delimiter}\n')
        else:
            stream.write(f'{key}={value}\n')


def target_release_note(target):
    if target in CLIENT_FABRIC_TARGETS:
        return (ROOT / f'release-notes/{target}.md').read_text().strip()
    return ''


def main():
    operation, target = sys.argv[1:3]
    if operation == 'matrix':
        selected = list(TARGETS) if target == 'all' else [target]
        output('matrix', json.dumps({'include': [metadata(item) for item in selected]}))
        return

    expected = metadata(target)
    if operation == 'prepare':
        commit = os.environ['GITHUB_SHA']
        if not re.fullmatch(r'[0-9a-f]{40}', commit):
            raise ValueError('Invalid build commit')
        jar = ROOT / target / 'build/libs' / expected['filename']
        if not jar.is_file():
            raise FileNotFoundError(f'Production jar missing: {jar}')
        bundle = ROOT / '.release' / target
        bundle.mkdir(parents=True, exist_ok=False)
        shutil.copy2(jar, bundle / jar.name)
        manifest = dict(expected, commit=commit, source_branch=os.environ['BUILD_SOURCE_BRANCH'],
                        repository=os.environ['GITHUB_REPOSITORY'],
                        run_id=os.environ['GITHUB_RUN_ID'],
                        sha256=hashlib.sha256(jar.read_bytes()).hexdigest())
        (bundle / 'release-manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as stream:
            stream.write(f"Built **{target}** from `{commit}`. Download **{expected['artifact']}** to review.\n\n")
        return

    if operation != 'verify':
        raise ValueError('Unknown metadata operation')
    for key, value in verify_bundle(target).items():
        output(key, value)


def verify_bundle(target):
    expected = metadata(target)
    bundle = ROOT / '.release' / target
    manifest = json.loads((bundle / 'release-manifest.json').read_text())
    for key, value in expected.items():
        if manifest.get(key) != value:
            raise ValueError(f'Artifact metadata mismatch: {key}')
    required = {
        'commit': os.environ['BUILD_COMMIT'], 'run_id': os.environ['BUILD_RUN_ID'],
        'repository': os.environ['GITHUB_REPOSITORY'],
        'source_branch': os.environ['BUILD_SOURCE_BRANCH'],
    }
    for key, value in required.items():
        if manifest.get(key) != value:
            raise ValueError(f'Artifact provenance mismatch: {key}')
    jar = bundle / expected['filename']
    if hashlib.sha256(jar.read_bytes()).hexdigest() != manifest['sha256']:
        raise ValueError('Artifact checksum mismatch')
    version = os.environ['RELEASE_VERSION']
    if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9.+_-]*', version):
        raise ValueError('Invalid release version characters')
    if version != expected['release_version']:
        raise ValueError(f"Artifact was built as {expected['release_version']}; build a new jar to change its version")
    label = 'fabric-client' if target in CLIENT_FABRIC_TARGETS else expected['loader']
    version_number = f"{version}+{label}.{expected['minecraft']}"
    if len(version_number) > 32:
        raise ValueError('Modrinth version including loader/game suffix exceeds 32 characters')
    notes = os.environ.get('RELEASE_NOTES', '').strip()
    target_notes = target_release_note(target)
    if target_notes:
        notes = '\n\n'.join(part for part in [notes, target_notes] if part)
    notes += f"\n\nSource commit: {manifest['commit']}\nSHA-256: {manifest['sha256']}\n"
    (bundle / 'release-notes.md').write_text(notes)
    return {
        'file': str(jar.relative_to(ROOT)), 'loader': expected['loader'],
        'minecraft': expected['minecraft'], 'version': version_number,
        'title': f"FBX Player Models {version} ({label} {expected['minecraft']})",
        'changelog': notes,
    }


if __name__ == '__main__':
    main()
