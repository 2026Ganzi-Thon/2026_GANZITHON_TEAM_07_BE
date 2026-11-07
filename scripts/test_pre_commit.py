"""Exercise the real pre-commit hook against temporary Git repositories."""

from pathlib import Path
import subprocess
import tempfile
import unittest


HOOK = Path(__file__).resolve().parents[1] / ".githooks/pre-commit"


class PreCommitTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.repo = Path(self.directory.name)
        self.git("init", "-q")
        self.git("config", "user.name", "Test User")
        self.git("config", "user.email", "test@example.com")

    def tearDown(self):
        self.directory.cleanup()

    def git(self, *args):
        return subprocess.run(["git", *args], cwd=self.repo, check=True,
                              capture_output=True, text=True)

    def run_hook(self):
        return subprocess.run(["sh", str(HOOK)], cwd=self.repo,
                              capture_output=True, text=True)

    def test_blocks_staged_env_file(self):
        env_file = self.repo / ".env"
        env_file.write_text("SECRET=123\n")
        self.git("add", ".env")
        result = self.run_hook()
        self.assertEqual(result.returncode, 1)
        self.assertIn("Commit blocked: Do not commit .env or secret environment files.", result.stderr)

    def test_allows_staged_env_example(self):
        env_example = self.repo / ".env.example"
        env_example.write_text("SECRET=\n")
        self.git("add", ".env.example")
        result = self.run_hook()
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_blocks_trailing_whitespace(self):
        test_file = self.repo / "test.txt"
        test_file.write_text("hello   \n")
        self.git("add", "test.txt")
        result = self.run_hook()
        self.assertEqual(result.returncode, 1)
        self.assertIn("Commit blocked: Trailing whitespace or conflict markers detected in staged files.", result.stderr)

    def test_passes_clean_staged_files_without_gradlew(self):
        test_file = self.repo / "test.txt"
        test_file.write_text("hello\n")
        self.git("add", "test.txt")
        result = self.run_hook()
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_runs_gradlew_and_blocks_on_failure(self):
        gradlew = self.repo / "gradlew"
        gradlew.write_text("#!/bin/sh\nexit 1\n")
        gradlew.chmod(0o755)
        test_file = self.repo / "test.txt"
        test_file.write_text("hello\n")
        self.git("add", "test.txt")
        result = self.run_hook()
        self.assertEqual(result.returncode, 1)
        self.assertIn("Commit blocked: Gradle check/tests failed.", result.stderr)

    def test_runs_gradlew_and_passes_on_success(self):
        gradlew = self.repo / "gradlew"
        gradlew.write_text("#!/bin/sh\nexit 0\n")
        gradlew.chmod(0o755)
        test_file = self.repo / "test.txt"
        test_file.write_text("hello\n")
        self.git("add", "test.txt")
        result = self.run_hook()
        self.assertEqual(result.returncode, 0, result.stderr)


if __name__ == "__main__":
    unittest.main(verbosity=2)
