from datetime import datetime, timezone
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from scheduled_release import already_published, in_release_window


class ReleaseWindowTest(unittest.TestCase):
    def test_cannot_release_before_1300_helsinki(self):
        self.assertFalse(in_release_window(datetime(2026, 11, 2, 10, 59, 59, tzinfo=timezone.utc)))

    def test_opens_at_1300_helsinki(self):
        self.assertTrue(in_release_window(datetime(2026, 11, 2, 11, 0, tzinfo=timezone.utc)))

    def test_allows_a_delay_on_the_same_local_day(self):
        self.assertTrue(in_release_window(datetime(2026, 11, 2, 21, 59, 59, tzinfo=timezone.utc)))

    def test_closes_at_helsinki_midnight(self):
        self.assertFalse(in_release_window(datetime(2026, 11, 2, 22, 0, tzinfo=timezone.utc)))

    def test_rejects_next_day_and_next_year(self):
        for instant in [datetime(2026, 11, 3, 11, tzinfo=timezone.utc),
                        datetime(2027, 11, 2, 11, tzinfo=timezone.utc)]:
            self.assertFalse(in_release_window(instant))


class DuplicatePublicationTest(unittest.TestCase):
    def setUp(self):
        self.row = {'version': '2.0.0+forge.1.21.1', 'loader': 'forge', 'minecraft': '1.21.1'}
        self.version = {
            'version_number': self.row['version'], 'version_type': 'alpha', 'status': 'listed',
            'loaders': ['forge'], 'game_versions': ['1.21.1'],
            'files': [{'hashes': {'sha512': 'same-file'}}],
        }

    def test_new_version_is_pending(self):
        self.assertFalse(already_published(self.row, [], 'same-file'))

    def test_identical_published_alpha_is_skipped(self):
        self.assertTrue(already_published(self.row, [self.version], 'same-file'))

    def test_conflicting_bytes_stop_publication(self):
        with self.assertRaises(RuntimeError):
            already_published(self.row, [self.version], 'different-file')

    def test_conflicting_channel_loader_or_game_stops_publication(self):
        for changes in [{'version_type': 'release'}, {'loaders': ['fabric']},
                        {'game_versions': ['26.2']}, {'status': 'draft'}]:
            with self.assertRaises(RuntimeError):
                already_published(self.row, [dict(self.version, **changes)], 'same-file')

    def test_ambiguous_version_numbers_stop_publication(self):
        with self.assertRaises(RuntimeError):
            already_published(self.row, [self.version, self.version], 'same-file')


if __name__ == '__main__':
    unittest.main()
