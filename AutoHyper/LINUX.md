# AutoHyper on Linux

AutoHyper targets .NET 8. A prebuilt portable package is included in
`benchmark-bundle/autohyper.zip`. On the server, from the repository root:

```bash
mkdir -p AutoHyper/app
unzip -o benchmark-bundle/autohyper.zip -d AutoHyper/app
dotnet --list-runtimes
```

The runtime list must include `Microsoft.NETCore.App 8.0.x`.
To rebuild the ZIP locally after source changes (requires the .NET 8 SDK):

```powershell
.\AutoHyper\package-linux.ps1
```

Alternatively, publish with the .NET 8 SDK (on the server or locally):

```bash
dotnet publish AutoHyper/src/AutoHyper/AutoHyper.fsproj -c Release -p:UseAppHost=false -o AutoHyper/app
```

Publishing locally and copying the entire `AutoHyper/app/` directory to the
server avoids NuGet downloads on the server. The server needs the .NET 8
runtime (`dotnet --list-runtimes`), Python 3, and Spot 2.14.1. Use
`dotnet AutoHyper/app/AutoHyper.dll`; a Windows `.exe` is not a Linux launcher.

Activate the Conda environment containing Spot, then locate the executable files:

```bash
command -v ltl2tgba
command -v autfilt
```

Generate `AutoHyper/app/paths.json` from those paths automatically:

```bash
python AutoHyper/configure-spot.py
```

Example generated file (your paths will differ):

```json
{
  "ltl2tgba": "/group/work/conda/envs/hypermc/bin/ltl2tgba",
  "autfilt": "/group/work/conda/envs/hypermc/bin/autfilt"
}
```

These must be executable file paths, not directories. Generate this file on the
server after publishing/copying the app. Do not reuse local Windows paths. Keep
the environment activated when submitting so shared libraries can be found.

From the repository root in the shared group work directory, submit:

```bash
sbatch --export=ALL scripts/hpc/autohyper.sbatch
squeue --me
```

This requests one CPU, 8 GiB RAM and two hours as initial estimates. No GPU is
used. Override memory/time/account/partition with `sbatch` options if needed.
For a smoke check on a compute node, submit only one case:

```bash
sbatch --export=ALL scripts/hpc/autohyper.sbatch --limit 1
```

The Python runner can also run directly inside an existing compute allocation:

```bash
python AutoHyper/run-benchmarks.py --limit 1
python AutoHyper/run-benchmarks.py
```

It uses the same six systems, 24 formulas and `--witness --log --explicit
--incl-spot` options as `run-tests-fixed.ps1`. Inputs are validated before
running. Each case has its own working directory because AutoHyper writes
intermediate HOA files. CSV rows are flushed after every case, and failures
make the runner exit nonzero. SAT/UNSAT is accepted only with exit code zero,
exactly one verdict and no ERROR block. Optional `--timeout SECONDS` limits
each case; it is recorded as TIMEOUT, never SAT/UNSAT.

Results are in `build/autohyper-linux/job-JOBID/results.csv`, with logs and
intermediate files in each case's subdirectory. CSV opens in Excel without
requiring Excel on the server. Slurm output/error logs are in the repository
root. A new output directory is required per invocation. Custom locations use
`--app-dir PATH` and `--output PATH`.

The launch changes preserve the existing automata flags and exit-code handling.
Formulas (including quoted AP names), paths, and parity options remain individual
arguments. On Linux, Spot is executed directly; Windows uses `wsl --exec` with
drive-path translation. No quantifier, complement, product, emptiness, or witness
algorithms are changed.

Runner regression checks: `python AutoHyper/test_linux_runner.py`.
Spot launch regression checks (SDK required): `dotnet fsi AutoHyper/test-spot-launch.fsx`.
