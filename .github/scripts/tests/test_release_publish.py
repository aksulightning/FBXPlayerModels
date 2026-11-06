from pathlib import Path
import json
import os
import sys
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from release_publish import already_published, selected_targets
import release_publish


class DuplicatePublicationTest(unittest.TestCase):
    def setUp(self):
        self.row = {'version': '2.0.0+forge.1.21.1', 'loader': 'forge', 'minecraft': '1.21.1'}
        self.version = {
            'version_number': self.row['version'], 'version_type': 'alpha', 'status': 'listed',
            'loaders': ['forge'], 'game_versions': ['1.21.1'],
            'files': [{'hashes': {'sha512': 'same-file'}}],
        }

    def test_new_version_is_pending(self):
        self.assertFalse(already_published(self.row, [], 'same-file', 'alpha'))

    def test_identical_published_alpha_is_skipped(self):
        self.assertTrue(already_published(self.row, [self.version], 'same-file', 'alpha'))

    def test_conflicting_bytes_stop_publication(self):
        with self.assertRaises(RuntimeError):
            already_published(self.row, [self.version], 'different-file', 'alpha')

    def test_conflicting_channel_loader_or_game_stops_publication(self):
        for changes in [{'version_type': 'release'}, {'loaders': ['fabric']},
                        {'game_versions': ['26.3']}, {'status': 'draft'}]:
            with self.assertRaises(RuntimeError):
                already_published(self.row, [dict(self.version, **changes)], 'same-file', 'alpha')

    def test_ambiguous_version_numbers_stop_publication(self):
        with self.assertRaises(RuntimeError):
            already_published(self.row, [self.version, self.version], 'same-file', 'alpha')

    def test_selected_beta_channel_is_used_for_duplicate_checks(self):
        self.version['version_type'] = 'beta'
        self.assertTrue(already_published(self.row, [self.version], 'same-file', 'beta'))


class ReleaseSelectionTest(unittest.TestCase):
    def test_all_selects_each_supported_port(self):
        self.assertEqual({'fabric-1.21.1', 'fabric-1.21.1-client', 'fabric-26.3', 'fabric-26.3-client', 'forge-1.21.1-client', 'forge-1.12.2-client', 'fabric-b1.7.3-client'},
                         set(selected_targets('all')))

    def test_single_target_stays_single(self):
        self.assertEqual(['forge-1.21.1-client'], selected_targets('forge-1.21.1-client'))

    def test_invalid_target_is_rejected(self):
        with self.assertRaises(ValueError):
            selected_targets('../main')


class GitHubPublicationTest(unittest.TestCase):
    def setUp(self):
        temp = TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        self.addCleanup(patch.stopall)
        patch.object(release_publish, 'ROOT', self.root).start()
        patch.dict(os.environ, {
            'RELEASE_TAG': 'v2.0.0-lan', 'GITHUB_REPOSITORY': 'aksulightning/FBXPlayerModels',
            'BUILD_COMMIT': 'a' * 40, 'RELEASE_NOTES': 'First line\nLiteral `$(command)`\nLast line',
        }).start()
        self.rows = []
        for target in ['fabric-b1.7.3-client', 'forge-1.21.1-client']:
            directory = self.root / '.release' / target
            directory.mkdir(parents=True)
            filename = f'{target}.jar'
            (directory / 'release-manifest.json').write_text(json.dumps({'sha256': 'file-hash'}))
            self.rows.append({'target': target, 'filename': filename, 'file': f'.release/{target}/{filename}'})
        patch.object(release_publish, 'verify_downloads', return_value=self.rows).start()
        self.run = patch.object(release_publish.subprocess, 'run').start()
        self.query = patch.object(release_publish.subprocess, 'check_output', return_value=b'[]').start()

    def test_all_selected_jars_go_into_one_prerelease_at_the_built_commit(self):
        release_publish.github_release('all', 'alpha')
        command = self.run.call_args.args[0]
        self.assertEqual(['gh', 'release', 'create', 'v2.0.0-lan'], command[:4])
        self.assertTrue(all(row['file'] in command for row in self.rows))
        self.assertEqual('a' * 40, command[command.index('--target') + 1])
        self.assertIn('--prerelease', command)
        self.assertIn('First line\nLiteral `$(command)`\nLast line',
                      (self.root / '.release/release-notes.md').read_text())

    def test_existing_tag_stops_before_creating_a_release(self):
        self.query.return_value = b'[{"ref":"refs/tags/v2.0.0-lan"}]'
        with self.assertRaises(ValueError):
            release_publish.github_release('all', 'alpha')
        self.assertEqual(1, self.run.call_count)  # Local tag-format validation only.

    def test_stable_channel_does_not_mark_a_prerelease(self):
        release_publish.github_release('all', 'release')
        self.assertNotIn('--prerelease', self.run.call_args.args[0])

    def test_client_fabric_releases_include_their_target_notes(self):
        for minecraft in ['1.21.1', '26.3']:
            target = f'fabric-{minecraft}-client'
            directory = self.root / '.release' / target
            directory.mkdir()
            (directory / 'release-manifest.json').write_text(json.dumps({'sha256': 'client-hash'}))
            self.rows.append({'target': target, 'filename': f'client-{minecraft}.jar', 'file': f'.release/{target}/client-{minecraft}.jar'})
        release_publish.github_release('all', 'alpha')
        notes = (self.root / '.release/release-notes.md').read_text()
        self.assertIn("future-proofing [Aksu's Peaceful Gameplay Pack]", notes)
        self.assertIn('https://modrinth.com/modpack/aksus-peaceful-gameplay-pack', notes)
        self.assertIn('Fabric 26.3 — Client Only', notes)


if __name__ == '__main__':
    unittest.main()
