import importlib.util
import contextlib
import io
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("runner", Path(__file__).with_name("run-benchmarks.py"))
runner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runner)
config_spec = importlib.util.spec_from_file_location("config", Path(__file__).with_name("configure-spot.py"))
config = importlib.util.module_from_spec(config_spec)
config_spec.loader.exec_module(config)


class RunnerTests(unittest.TestCase):
    def test_case_matrix_matches_windows_runner(self):
        cases = list(runner.cases(Path("inputs")))
        self.assertEqual(144, len(cases))
        self.assertEqual(144, len({name for name, _, _ in cases}))
        self.assertEqual("system-1-AA-formula-1", cases[0][0])
        self.assertEqual("system-6-EAEA-formula-3", cases[-1][0])

    def test_uses_solver_verdict_and_reported_time(self):
        self.assertEqual(("UNSAT", 0.0123), runner.parse_result(
            "\nUNSAT\n> total-time: 12ms (0.0123s)\n", 0, 1.5))

    def test_failure_cannot_be_mistaken_for_sat(self):
        for text, code in [("SAT\n", 1), ("SAT\n===== ERROR =====\n", 0),
                           ("no verdict\n", 0), ("SAT\nUNSAT\n", 0), ("SAT\n", None)]:
            with self.subTest(text=text, code=code):
                self.assertEqual(("FAILED", 1.5), runner.parse_result(text, code, 1.5))

    def test_configuration_uses_path_and_preserves_optional_settings(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "paths.json"
            target.write_text(json.dumps({"rabit": "existing.jar", "autfilt": "old"}), encoding="utf-8")
            paths = {name: str(Path(directory) / name) for name in ("ltl2tgba", "autfilt")}
            with patch("sys.argv", ["configure-spot.py", "--app-dir", directory]), \
                 patch.object(config.shutil, "which", side_effect=paths.get), \
                 contextlib.redirect_stdout(io.StringIO()):
                config.main()
            self.assertEqual({"rabit": "existing.jar", **paths}, json.loads(target.read_text(encoding="utf-8")))

    def test_missing_spot_does_not_overwrite_configuration(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "paths.json"
            target.write_text('{"autfilt": "old"}', encoding="utf-8")
            with patch("sys.argv", ["configure-spot.py", "--app-dir", directory]), \
                 patch.object(config.shutil, "which", return_value=None), \
                 contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
                config.main()
            self.assertEqual('{"autfilt": "old"}', target.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
