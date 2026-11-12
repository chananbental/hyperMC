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
The university guide (August 17, 2026, pages 7-9) requires Slurm for computation.
Keep this bundle and your Conda environment in the shared group `work` area.
With that environment activated on the login node, submit:

```bash
bash run-benchmarks.sh submit all
squeue --me
```

The bundled `benchmarks.sbatch` requests one node, one task, one CPU, 8 GiB
RAM, and two hours. These are starting estimates, not measured requirements.
It inherits the activated Conda environment and uses `srun` to launch the tests.
No GPU is used. The guide's four-CPU example is illustrative; the benchmarks
execute serially. Java's processor count is limited to the allocated CPUs.

Logs are `hypermc-benchmarks-JOBID.out` and `.err` in the extracted bundle;
results and temporary files are in `results/job-JOBID/`.
To cancel, run `scancel JOBID`. After completion, inspect usage with:

```bash
sacct -j JOBID --format=JobID,State,Elapsed,MaxRSS,AllocCPUS
```

To override resources or provide a site-specific account/partition, submit
directly from the extracted bundle, for example:

```bash
sbatch --export=ALL --mem=16G --time=04:00:00 benchmarks.sbatch all
```

Add `--account=YOUR_ACCOUNT` or `--partition=YOUR_PARTITION` only if your account
requires them; the supplied guide does not specify these values. Increase the
memory allocation based on measured usage; increasing it does not increase the
launcher's 4 GiB Java heap. No code or automata semantics are changed.

Use `approximate` to run `ModelCheckerTest` (all configured strategy
combinations, including exact), or `exact` to run only `ExactModelCheckerTest`.
The default is `all`. An optional second argument selects an output directory:

```bash
bash run-benchmarks.sh submit exact /path/to/work/exact-run-001
```

Use a new output directory per run; existing result files in an explicitly
reused directory will be overwritten by the tests. By default, each invocation
creates its own directory under `results/` in the extracted bundle.
Direct `all`, `approximate`, or `exact` execution requires an existing Slurm
allocation; `discover` can run on the login node without allocating resources.
Results are `results.json` / `results.xlsx` for ModelCheckerTest and
`extact-results.json` / `extact-results.xlsx` for ExactModelCheckerTest (the
existing spelling). JUnit XML reports are in `junit-reports/` inside that run's
directory. Output files are saved when each benchmark test finishes; console
progress is in Slurm's output log. A nonzero job exit status means the run failed.

The launcher uses a 4 GiB Java heap; request enough scheduler memory for Java
and Spot subprocesses. Do not compare timings from simultaneously running jobs
on the same allocated CPU. Rebuild and transfer the bundle after source changes.

This bundle selects the existing tests without changing solver logic or
HyperLTL semantics. It does not include AutoHyper or its .NET runtime; run
AutoHyper separately using a Linux build.
