# Portable benchmark bundle

Build on a computer with Java 25 and working Maven Central access:

```powershell
.\gradlew.bat benchmarkBundle
```

This compiles, but does not run, the tests. The ZIP is written to
`benchmark-bundle/hypermc-benchmarks.zip`, a directory intended for committing
to Git. Commit the rebuilt ZIP after source changes, then pull on the server.
Alternatively, transfer the ZIP directly to the university server. All Java
runtime dependencies and compiled main/test classes are included. No Gradle,
Maven access, or repository checkout is required on the server.

On Linux, activate the environment containing Java 25 and Spot 2.14.1, then:

```bash
# From the repository root after git pull:
cd benchmark-bundle
unzip hypermc-benchmarks.zip
cd hypermc-benchmarks
bash run-benchmarks.sh discover
```

Discovery lists the two benchmark tests without running them or requiring Spot.
Run computational work on a compute node, following the university's scheduler
rules. In that allocation, with the Conda environment activated, run:

```bash
bash run-benchmarks.sh all > benchmarks.log 2>&1
```

Use `approximate` to run `ModelCheckerTest` (all configured strategy
combinations, including exact), or `exact` to run only `ExactModelCheckerTest`.
The default is `all`. An optional second argument selects an output directory:

```bash
bash run-benchmarks.sh exact /path/to/work/exact-run-001
```

Use a new output directory per run; existing result files in an explicitly
reused directory will be overwritten by the tests. By default, each invocation
creates its own directory under `results/` in the extracted bundle.
Results are `results.json` / `results.xlsx` for ModelCheckerTest and
`extact-results.json` / `extact-results.xlsx` for ExactModelCheckerTest (the
existing spelling). JUnit XML reports are in `junit-reports/` inside that run's
directory. Output files are saved when each benchmark test finishes; console
progress is in the redirected log. A nonzero exit status means the run failed.

The launcher uses a 4 GiB Java heap; request enough scheduler memory for Java
and Spot subprocesses. Do not compare timings from simultaneously running jobs
on the same allocated CPU. Rebuild and transfer the bundle after source changes.

This bundle selects the existing tests without changing solver logic or
HyperLTL semantics. It does not include AutoHyper or its .NET runtime; run
AutoHyper separately using a Linux build.
