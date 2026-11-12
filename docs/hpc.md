# Running approximation benchmarks on the university HPC

For running the original Excel-exporting benchmark tests without Gradle or
dependency downloads on the server, see [the portable benchmark bundle](benchmark-bundle.md).

This setup follows the supplied Open University HPC guidelines dated February
17, 2026 (pages 1–7): connect through `sheshet` to `login`, keep project data in
your research group's `work` directory, load software using Lmod, and run
computational work through Slurm. Exact hostnames, module names, account,
partition, and work path must come from your HPC account details; they are not
specified in the PDF text. Ask `hpc@openu.ac.il` for missing software rather than
installing it yourself.

## First connection using PuTTY

The additional university PuTTY/VNC guide (October 2024) supplies these addresses
in its screenshots. If your account email gives newer settings, use those.

1. Open PuTTY. Set Host Name to `sheshet.cslab.openu.ac.il`, port `22`, and
   connection type SSH. Click Open.
2. Enter the username and initial password supplied by HPC support. The guide
   says you will be prompted to replace the initial password. Enter passwords
   only in the terminal; typed password characters normally remain invisible.
3. Close that terminal once the password change completes, and reopen PuTTY.
4. Under Session, set Host Name to `login8.openu.ac.il`, port `22`, SSH.
5. Under Connection → Proxy, select `SSH to proxy and use port forwarding`,
   set Proxy hostname to `sheshet.cslab.openu.ac.il`, and port to `22`.
   Leave the password field empty so credentials are entered at connection time.
6. Return to Session, enter `login8` under Saved Sessions, and click Save, then
   Open. The guide says to expect password prompts for both gateway and login.

For a first-time SSH host-key prompt, compare the fingerprint with the value
provided by HPC support before accepting it.

Once connected, run these lightweight discovery commands and retain their output:

```bash
hostname
pwd
module spider java
module spider spot
```

The group work-directory path must come from your account details or support;
`pwd` will typically show home, not the shared work directory. VNC and its tunnel
are optional and are not needed to submit these command-line benchmarks.
The PuTTY saved session is not automatically an OpenSSH alias: the `scp` examples
below require the separate SSH configuration supplied by support, or use WinSCP
configured with the same gateway instead.

## Prerequisites

On the login server, discover available modules:

```bash
module spider java
module spider spot
module spider gradle
```

The project needs Java 25 and Spot 2.14.1 (`ltl2tgba` and `autfilt`). Linux runs
Spot directly; WSL is only used on Windows. The Gradle wrapper uses Gradle 9.2.0
and downloads dependencies. Confirm with support that using the wrapper and
accessing its dependency repositories from compute nodes is permitted; if not,
ask for an approved Gradle/dependency setup before submission. The script stores
its Gradle cache in the project work directory, not in home.

## Transfer

Use the SSH configuration supplied by HPC support, including the gateway.
`login` below is an example configured SSH alias, not a public hostname. From
local PowerShell, create a source archive (including local tracked edits):

```powershell
tar -czf hypermc-hpc.tar.gz AGENTS.md build.gradle settings.gradle gradlew gradle src docs scripts
scp hypermc-hpc.tar.gz login:/YOUR/GROUP/WORK/
```

On the login server, extract into a new directory under your actual group work
path. Use a separate project directory for each concurrently running job because
the benchmark writes to fixed paths in `build`.

```bash
cd /YOUR/GROUP/WORK
mkdir hypermc-run-001
tar -xzf hypermc-hpc.tar.gz -C hypermc-run-001
cd hypermc-run-001
```

Keep the shell script's line endings as LF when editing on Windows.

## Submit and retrieve

Set the actual module names reported by `module spider`, then submit from the
project root:

```bash
export JAVA_MODULE='REPLACE_WITH_JAVA_25_MODULE'
export SPOT_MODULE='REPLACE_WITH_SPOT_2.14.1_MODULE'
sbatch scripts/hpc/approximations.sbatch
squeue -u "$USER"
```

The script requests one CPU, 8 GiB RAM and two hours, with a 4 GiB test JVM heap.
These are initial resource estimates, not university limits or measured needs.
Adjust them to site policy and observed usage; supply `--account` or `--partition`
to `sbatch` if required by your account. No GPU is used by this implementation.
The benchmark is serial; requesting more CPUs does not automatically parallelize
the cases. Do not run Gradle tests or benchmarks directly on the login server.

Inspect `hypermc-approx-JOBID.out` and `.err`. To cancel, use `scancel JOBID`.
After completion, retrieve the table and reports from local PowerShell:

```powershell
scp login:/YOUR/GROUP/WORK/hypermc-run-001/build/approximation-benchmarks/results.tsv .
scp -r login:/YOUR/GROUP/WORK/hypermc-run-001/build/reports/tests/benchmarkApproximations .
scp -r login:/YOUR/GROUP/WORK/hypermc-run-001/build/test-results/benchmarkApproximations .
```

This task reruns the existing 288 checks: six systems × 24 formulas × two
approximation strategies. It checks against the saved AutoHyper-verified exact
baseline; it does not rerun AutoHyper. Results include elapsed milliseconds,
verdicts, refinement rounds and inconclusive outcomes. Inconclusive outcomes do
not fail this soundness comparison, so inspect their counts as well as the test
status. A timeout can leave a partial table; a complete run has 288 data rows.
These measurements include solver process overhead and are not a controlled
performance study with warmups and repetitions.

The full `validateModelChecker` suite currently expects the Windows
`AutoHyper/app/AutoHyper.exe` installation. It needs a Linux-compatible AutoHyper
installation and launcher/configuration changes before it can run on the HPC.
No quantifier, complementation, product, emptiness, approximation, or witness
semantics are changed by this submission setup.
