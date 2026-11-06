import hashlib
import json
import os
from pathlib import Path
import sys
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import release_metadata

CLIENT_NOTE = (release_metadata.ROOT / 'release-notes/fabric-1.21.1-client.md').read_text()


class ArtifactVerificationTest(unittest.TestCase):
    def setUp(self):
        self.temp = TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        root = Path(self.temp.name)
        self.addCleanup(patch.stopall)
        patch.object(release_metadata, 'ROOT', root).start()
        patch.dict(os.environ, {
            'BUILD_COMMIT': 'a' * 40, 'BUILD_RUN_ID': '123',
            'BUILD_SOURCE_BRANCH': 'modrinth-last-update',
            'GITHUB_REPOSITORY': 'aksulightning/FBXPlayerModels',
            'RELEASE_VERSION': '2.0.0', 'RELEASE_NOTES': 'Alpha release',
        }).start()
        (root / 'gradle.properties').write_text(
            'archives_base_name=fbx-player-models\nforge_1211_mod_version=2.0.0+1.21.1\n')
        self.bundle = root / '.release/forge-1.21.1'
        self.bundle.mkdir(parents=True)
        self.jar = self.bundle / 'fbx-player-models-forge-v2.0.0+1.21.1+mc1.21.1.jar'
        self.jar.write_bytes(b'reviewed artifact')
        self.manifest = {
            'target': 'forge-1.21.1', 'minecraft': '1.21.1', 'loader': 'forge', 'java': '21',
            'task': ':forge-1.21.1:jarJar', 'filename': self.jar.name,
            'mod_version': '2.0.0+1.21.1', 'release_version': '2.0.0',
            'artifact': 'fbx-player-models-forge-1.21.1', 'proposed_tag': 'nightly-forge-1.21.1',
            'commit': 'a' * 40, 'run_id': '123', 'repository': 'aksulightning/FBXPlayerModels',
            'source_branch': 'modrinth-last-update',
            'sha256': hashlib.sha256(self.jar.read_bytes()).hexdigest(),
        }
        self.write_manifest()

    def write_manifest(self):
        (self.bundle / 'release-manifest.json').write_text(json.dumps(self.manifest))

    def test_manual_release_keeps_its_original_branch_and_version(self):
        verified = release_metadata.verify_bundle('forge-1.21.1')
        self.assertEqual('2.0.0+forge.1.21.1', verified['version'])
        self.assertEqual('forge', verified['loader'])

    def test_selected_main_branch_can_be_verified(self):
        os.environ['BUILD_SOURCE_BRANCH'] = 'main'
        self.manifest['source_branch'] = 'main'
        self.write_manifest()
        self.assertEqual('1.21.1', release_metadata.verify_bundle('forge-1.21.1')['minecraft'])

    def test_wrong_branch_and_commit_are_rejected(self):
        for key, value in [('source_branch', 'main'), ('commit', 'b' * 40), ('run_id', '456')]:
            original = self.manifest[key]
            self.manifest[key] = value
            self.write_manifest()
            with self.assertRaises(ValueError):
                release_metadata.verify_bundle('forge-1.21.1')
            self.manifest[key] = original

    def test_changed_artifact_is_rejected(self):
        self.jar.write_bytes(b'different artifact')
        with self.assertRaises(ValueError):
            release_metadata.verify_bundle('forge-1.21.1')

    def test_relabeling_a_built_version_is_rejected(self):
        os.environ['RELEASE_VERSION'] = '3.0.0'
        with self.assertRaises(ValueError):
            release_metadata.verify_bundle('forge-1.21.1')

    def test_client_fabric_has_distinct_filename_version_and_changelog(self):
        root = release_metadata.ROOT
        with (root / 'gradle.properties').open('a') as stream:
            stream.write(
                'fabric_1211_mod_version=2.0.0+1.21.1\nfabric_1211_client_mod_version=2.0.0+1.21.1\n'
                'fabric_263_mod_version=2.0.0+26.3\nfabric_263_client_mod_version=2.0.0+26.3\n')
        (root / 'release-notes').mkdir()
        notes = {
            'fabric-1.21.1-client': CLIENT_NOTE,
            'fabric-26.3-client': 'Fabric 26.3 — Client Only: local model settings.',
        }
        for minecraft in ['1.21.1', '26.3']:
            target = f'fabric-{minecraft}-client'
            with self.subTest(target=target):
                (root / f'release-notes/{target}.md').write_text(notes[target])
                expected = release_metadata.metadata(target)
                bundle = root / '.release' / target
                bundle.mkdir()
                jar = bundle / expected['filename']
                jar.write_bytes(b'client-only artifact')
                manifest = dict(self.manifest, **expected)
                manifest['sha256'] = hashlib.sha256(jar.read_bytes()).hexdigest()
                (bundle / 'release-manifest.json').write_text(json.dumps(manifest))
                verified = release_metadata.verify_bundle(target)
                self.assertEqual(f'fbx-player-models-client-v2.0.0+{minecraft}+mc{minecraft}.jar', expected['filename'])
                self.assertNotEqual(release_metadata.metadata(f'fabric-{minecraft}')['filename'], expected['filename'])
                self.assertEqual(f'2.0.0+fabric-client.{minecraft}', verified['version'])
                self.assertLessEqual(len(verified['version']), 32)
                self.assertEqual(f':{target}:' + ('jar' if minecraft == '26.3' else 'remapJar'), expected['task'])
                self.assertIn(notes[target].strip(), verified['changelog'])
                self.assertIn('Alpha release', verified['changelog'])
                if minecraft == '26.3':
                    self.assertNotIn("Aksu's Peaceful Gameplay Pack", verified['changelog'])
                else:
                    self.assertIn("future-proofing [Aksu's Peaceful Gameplay Pack]", verified['changelog'])
                    self.assertIn('https://modrinth.com/modpack/aksus-peaceful-gameplay-pack', verified['changelog'])

    def test_multiline_changelog_uses_github_output_delimiter(self):
        destination = Path(self.temp.name) / 'github-output'
        os.environ['GITHUB_OUTPUT'] = str(destination)
        release_metadata.output('changelog', 'First line\nSecond line')
        lines = destination.read_text().splitlines()
        delimiter = lines[0].split('<<', 1)[1]
        self.assertEqual(['First line', 'Second line', delimiter], lines[1:])


if __name__ == '__main__':
    unittest.main()
