"""Exercise the real pre-push hook against temporary Git repositories, without network."""

from pathlib import Path
import subprocess
import tempfile
import unittest


HOOK = Path(__file__).resolve().parents[1] / ".githooks/pre-push"
TEAM = "https://github.com/2026Ganzi-Thon/2026_GANZITHON_TEAM_07_BE.git"
SHA = "1" * 40
ZERO = "0" * 40


def update(local="refs/heads/feat/test", remote="refs/heads/feat/test"):
    return f"{local} {SHA} {remote} {ZERO}\n"


class PrePushTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.repo = Path(self.directory.name)
        self.git("init", "-q")
        self.git("remote", "add", "origin", TEAM)

    def tearDown(self):
        self.directory.cleanup()

    def git(self, *args):
        return subprocess.run(["git", *args], cwd=self.repo, check=True,
                              capture_output=True, text=True)

    def check(self, allowed, data=None, name="origin", url=TEAM):
        result = subprocess.run(["sh", str(HOOK), name, url], cwd=self.repo,
                                input=update() if data is None else data,
                                capture_output=True, text=True)
        self.assertEqual(result.returncode, 0 if allowed else 1, result.stderr)
        if not allowed:
            self.assertIn("Push blocked:", result.stderr)

    def test_team_urls_allow_task_branches(self):
        for url in [TEAM, TEAM + "/", TEAM[:-4],
                    "git@github.com:2026Ganzi-Thon/2026_GANZITHON_TEAM_07_BE.git",
                    "ssh://git@github.com/2026Ganzi-Thon/2026_GANZITHON_TEAM_07_BE.git"]:
            with self.subTest(url=url):
                self.git("remote", "set-url", "origin", url)
                self.check(True, url=url)

    def test_rejects_fork_other_repo_host_and_local_path(self):
        for url in [TEAM.replace("2026Ganzi-Thon", "WooDDANG"),
                    TEAM.replace("TEAM_07_BE", "TEAM_07_BE_other"),
                    TEAM.replace("github.com", "github.com.example.org"),
                    "/tmp/local-repository"]:
            with self.subTest(url=url):
                self.git("remote", "set-url", "origin", url)
                self.check(False, url=url)

    def test_rejects_another_remote_name(self):
        self.check(False, name="upstream")

    def test_rejects_url_argument_mismatch(self):
        self.check(False, url=TEAM.replace("2026Ganzi-Thon", "WooDDANG"))

    def test_uses_configured_push_url(self):
        ssh = "git@github.com:2026Ganzi-Thon/2026_GANZITHON_TEAM_07_BE.git"
        self.git("config", "remote.origin.pushurl", ssh)
        self.check(True, url=ssh)
        self.check(False)

    def test_rejects_multiple_push_urls(self):
        self.git("config", "--add", "remote.origin.pushurl", TEAM)
        self.git("config", "--add", "remote.origin.pushurl", TEAM.replace("2026Ganzi-Thon", "WooDDANG"))
        self.check(False)

    def test_protects_main_source_and_destination(self):
        for local, remote in [("refs/heads/main", "refs/heads/main"),
                              ("refs/heads/feat/test", "refs/heads/main"),
                              ("refs/heads/main", "refs/heads/feat/test")]:
            with self.subTest(local=local, remote=remote):
                self.check(False, update(local, remote))

    def test_rejects_tags_deletions_and_detached_head(self):
        for local, remote in [("refs/tags/v1", "refs/tags/v1"),
                              ("(delete)", "refs/heads/feat/test"),
                              ("HEAD", "refs/heads/feat/test"),
                              ("refs/heads/feat/test", "refs/tags/v1")]:
            with self.subTest(local=local, remote=remote):
                self.check(False, update(local, remote))

    def test_checks_every_ref_in_batch(self):
        self.check(True, update() + update("refs/heads/fix/test", "refs/heads/fix/test"))
        self.check(False, update() + update(remote="refs/heads/main"))


if __name__ == "__main__":
    unittest.main(verbosity=2)
