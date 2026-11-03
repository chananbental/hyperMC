# Routine model checker validation

Run the external validation suite with one command:

```powershell
./gradlew.bat validateModelChecker --console=plain
```

The verified default run took about 20 minutes on this installation.

This task enables and runs only the dedicated validation tests, using Java 25,
WSL Spot, and the existing Windows AutoHyper installation/configuration. It runs
with one test worker so solver intermediate files do not collide. It always
executes fresh checks and does not use cached test outcomes from another solver
environment.

The suite includes:

- 36 small exact-versus-AutoHyper cases.
- 144 existing benchmark cases against AutoHyper.
- 288 SCC/reachability comparisons against the verified exact benchmark snapshot.
- 64 seeded generated cases, including exact and approximate comparisons.
- 60 counterexample semantic checks and the fixed-domain control.

The combined Markdown summary is written to
`build/reports/model-checker-validation/summary.md`. Separate JUnit HTML/XML
reports are under `build/reports/tests/validateModelChecker` and
`build/test-results/validateModelChecker`. The summary counts agreement,
inconclusive outcomes, and failures separately and identifies missing or partial
datasets. Its finalizer runs even when a validation test fails. Individual inputs,
solver logs, and TSV tables remain in their existing build directories.

Before starting, the task clears only the current result tables and its own JUnit
XML files so stale results cannot be mistaken for a successful new run. Saved
case inputs and diagnostics remain available. The counterexample test checks the
fixed seed 20261012 snapshot; refresh verified snapshots if benchmark or generator
definitions change.

Choose another seed for the generated comparison using:

```powershell
./gradlew.bat validateModelChecker -PvalidationSeed=20261013 --console=plain
```

The counterexample suite continues checking the independently verified default
seed's witnesses. Each generated seed has its own artifact directory. Ordinary
`./gradlew.bat test` retains its existing behavior and skips the external suites
unless their opt-in properties are explicitly enabled.

The existing [exact comparisons](autohyper-comparison.md),
[approximation checks](approximation-comparison.md),
[generated comparisons](generated-comparison.md), and
[counterexample validation](counterexample-validation.md) describe the semantics
and saved artifacts of each suite.
