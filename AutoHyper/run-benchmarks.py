"""Run the 144 custom AutoHyper cases without PowerShell or Excel dependencies."""
import argparse
import csv
import re
import shutil
import subprocess
import time
from datetime import datetime
from pathlib import Path


def cases(base):
    for system in range(1, 7):
        for count, kind in ((0, "AA"), (0, "EE"), (1, "AE"), (1, "EA"),
                            (2, "AEA"), (2, "EAE"), (3, "AEAE"), (3, "EAEA")):
            for number in range(1, 4):
                yield (f"system-{system}-{kind}-formula-{number}",
                       base / "systems" / f"system-{system}.txt",
                       base / "formulas" / f"{count}-alternations" / kind / f"formula-{number}.hq")


def parse_result(text, exit_code, elapsed):
    verdicts = re.findall(r"^\s*(SAT|UNSAT)\s*$", text, re.MULTILINE)
    error = re.search(r"^=+\s*ERROR\s*=+\s*$", text, re.MULTILINE | re.IGNORECASE)
    if exit_code != 0 or error or len(verdicts) != 1:
        return "FAILED", elapsed
    match = re.search(r">\s*total-time:\s*[0-9.]+\s*ms\s*\(([0-9.]+)s\)", text)
    return verdicts[0], float(match[1]) if match else elapsed


def main():
    root = Path(__file__).resolve().parent
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--app-dir", type=Path, default=root / "app")
    parser.add_argument("--output", type=Path)
    parser.add_argument("--limit", type=int, help="Run only the first N cases for a smoke check")
    parser.add_argument("--timeout", type=float, help="Optional per-case timeout in seconds")
    args = parser.parse_args()
    if args.limit is not None and args.limit < 1:
        parser.error("--limit must be positive")
    if args.timeout is not None and args.timeout <= 0:
        parser.error("--timeout must be positive")
    dll = args.app_dir.resolve() / "AutoHyper.dll"
    if not dll.is_file() or not dll.with_name("paths.json").is_file():
        parser.error("Publish AutoHyper and run configure-spot.py first")
    dotnet = shutil.which("dotnet")
    if not dotnet:
        parser.error("dotnet is missing from PATH")
    selected = list(cases(root / "benchmarks/my_tests/compare-to-my-solver"))
    if args.limit:
        selected = selected[:args.limit]
    for _, system, formula in selected:
        if not system.is_file() or not formula.is_file():
            parser.error(f"Missing benchmark input: {system if not system.is_file() else formula}")
    output = (args.output or root.parent / "build/autohyper-linux" /
              datetime.now().strftime("%Y%m%d-%H%M%S-%f")).resolve()
    # Refuse reuse so previous results cannot be mistaken for a completed run.
    output.mkdir(parents=True, exist_ok=False)
    print(f"Results: {output}", flush=True)
    failures = 0
    with (output / "results.csv").open("w", newline="", encoding="utf-8") as table:
        writer = csv.writer(table)
        writer.writerow(["Case", "System", "Formula", "Result", "Total Time (s)", "Exit Code", "Log"])
        table.flush()
        for name, system, formula in selected:
            work = output / name
            work.mkdir()
            log = work / "run.log"
            command = [dotnet, str(dll), "--witness", "--log", "--explicit", "--incl-spot",
                       str(system), str(formula)]
            print(f"Running {name}", flush=True)
            start = time.monotonic()
            code = None
            timed_out = False
            with log.open("w", encoding="utf-8") as stream:
                try:
                    process = subprocess.run(command, cwd=work, stdout=stream,
                                             stderr=subprocess.STDOUT, timeout=args.timeout)
                    code = process.returncode
                except subprocess.TimeoutExpired:
                    timed_out = True
                    stream.write("\nRunner: per-case timeout exceeded\n")
                except OSError as error:
                    stream.write(f"\nRunner: {error}\n")
            elapsed = time.monotonic() - start
            verdict, seconds = parse_result(log.read_text(encoding="utf-8", errors="replace"), code, elapsed)
            if timed_out:
                verdict = "TIMEOUT"
            failures += verdict not in ("SAT", "UNSAT")
            writer.writerow([name, system.stem, " ".join(formula.read_text(encoding="utf-8-sig").split()),
                             verdict, f"{seconds:.4f}", code, str(log.relative_to(output))])
            table.flush()
            print(f"  {verdict} | {seconds:.4f}s", flush=True)
    print(f"Completed {len(selected)} cases; failed or timed out: {failures}", flush=True)
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
