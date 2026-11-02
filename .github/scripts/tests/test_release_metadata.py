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


class ArtifactVerificationTest(unittest.TestCase):
    def setUp(self):
        self.temp = TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        root = Path(self.temp.name)
        self.addCleanup(patch.stopall)
        patch.object(release_metadata, 'ROOT', root).start()
        patch.dict(os.environ, {
            'BUILD_COMMIT': 'a' * 40, 'BUILD_RUN_ID': '123',
            'GITHUB_REPOSITORY': 'aksulightning/FBXPlayerModels',
            'RELEASE_VERSION': '2.0.0', 'RELEASE_NOTES': 'Alpha release',
        }).start()
        os.environ.pop('BUILD_SOURCE_BRANCH', None)
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

    def test_scheduled_release_can_verify_main_branch(self):
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


if __name__ == '__main__':
    unittest.main()
