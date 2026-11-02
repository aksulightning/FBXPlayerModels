"""Target definitions and artifact provenance shared by the release workflows."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import sys

ROOT = Path(__file__).resolve().parents[2]
TARGETS = {
    'fabric-1.21.1': ('1.21.1', 'fabric', '21', 'remapJar', 'fabric_1211_mod_version', 'nightly-1.21.1'),
    'fabric-26.2': ('26.2', 'fabric', '25', 'jar', 'fabric_262_mod_version', 'nightly-26.2'),
    'forge-1.21.1': ('1.21.1', 'forge', '21', 'jarJar', 'forge_1211_mod_version', 'nightly-forge-1.21.1'),
    'fabric-b1.7.3': ('b1.7.3', 'babric', '21', 'remapJar', 'fabric_b173_mod_version', 'nightly-b1.7.3'),
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
    filename = f"{base}{'-forge' if loader == 'forge' else ''}-v{version}+mc{minecraft}.jar"
    return {
        'target': target, 'minecraft': minecraft, 'loader': loader, 'java': java,
        'task': f':{target}:{task}', 'filename': filename,
        'mod_version': version, 'release_version': version.split('+', 1)[0],
        'artifact': f'fbx-player-models-{target}', 'proposed_tag': tag,
    }


def output(key, value):
    with open(os.environ['GITHUB_OUTPUT'], 'a') as stream:
        stream.write(f'{key}={value}\n')


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
        manifest = dict(expected, commit=commit, source_branch=os.environ.get('BUILD_SOURCE_BRANCH', 'modrinth-last-update'),
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
        'source_branch': os.environ.get('BUILD_SOURCE_BRANCH', 'modrinth-last-update'),
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
    version_number = f"{version}+{expected['loader']}.{expected['minecraft']}"
    if len(version_number) > 32:
        raise ValueError('Modrinth version including loader/game suffix exceeds 32 characters')
    notes = os.environ.get('RELEASE_NOTES', '').strip()
    notes += f"\n\nSource commit: {manifest['commit']}\nSHA-256: {manifest['sha256']}\n"
    (bundle / 'release-notes.md').write_text(notes)
    return {
        'file': str(jar.relative_to(ROOT)), 'loader': expected['loader'],
        'minecraft': expected['minecraft'], 'version': version_number,
        'title': f"FBX Player Models {version} ({expected['loader']} {expected['minecraft']})",
    }


if __name__ == '__main__':
    main()
