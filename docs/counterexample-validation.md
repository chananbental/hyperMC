# Counterexample semantic validation

Checked the twenty false universal-leading formulas from generated seed
**20261012** using exact, SCC, and reachability strategies: **60 counterexamples**.
Every returned counterexample has valid original-system lassos and refutes the
remaining quantified property according to AutoHyper. There are zero failures.
The [per-case results](counterexample-validation-results.tsv) record the case,
strategy, prefix, validation outcome, and fixed-domain AutoHyper verdict.

| Prefix | False formulas | Validated counterexamples |
| --- | ---: | ---: |
| AA | 4 | 12 |
| AE | 3 | 9 |
| AEA | 7 | 21 |
| AEAE | 6 | 18 |
| Total | 20 | 60 |

## Structural checks

The test checks the returned traces independently of the production tuple
validator. It requires exactly the outermost universal block's variables,
without duplicates. Each lasso must start at the original initial state at time
zero, match the state's full renamed valuation, follow original transitions,
have a nonempty loop, and close that loop through an original transition.
This validates 72 individual trace lassos across the 60 counterexamples.

## Meaning of the counterexample

For each outer trace, the test builds a deterministic transition system whose
only infinite path spells that lasso. Each time position gets a separate state;
repeated original states are not collapsed, preserving the timing of `X` and
other temporal operators. The last loop position points back to the first loop
position, and initial-state labels are read at time zero.

AutoHyper receives one domain system per quantifier: singleton lasso domains for
the outer universal block, and the full original system for every remaining
quantifier. The original quantifier prefix and body are retained. Universal
quantification over a singleton trace domain is equivalent to fixing that trace,
so an AutoHyper UNSAT result establishes that the returned outer traces refute
the remaining quantified property, including alternation. All 60 checks return
UNSAT using `--explicit --comp --no-bisim`.

A control checks `forall p. forall q. X(a_p <-> a_q)` on a branching system.
The original formula is UNSAT because two traces can have different next labels.
Fixing both variables to the same valid trace yields SAT. This confirms that
the harness restricts the domains and distinguishes physical trace validity
from being an actual counterexample.

No production checker semantics changed. Existential-leading false formulas do
not provide this kind of outer universal counterexample tuple and are outside
this check's scope. The results cover this generated batch, not arbitrary traces
or formulas.

## Reproduce

Requires Java 25, WSL Spot, and the working local Windows AutoHyper build.
The generated reference snapshot is checked against the current generator's
case IDs and formulas; refresh it if generation changes.

```powershell
$env:JAVA_TOOL_OPTIONS = '-Dcounterexample.compare=true'
./gradlew.bat test --tests '*CounterexampleSemanticTest' --console=plain
Remove-Item Env:JAVA_TOOL_OPTIONS
```

Artifacts are saved under `build/counterexample-comparison/20261012/<case>/<strategy>/`:
witness JSON, original system, singleton trace systems, formula, command arguments,
and AutoHyper log. Failed validations additionally save `FAILURE.txt` and a row
in `failures.tsv`; all cases are collected before the test asserts agreement.
The control inputs and logs are saved under `build/counterexample-comparison/control/`.
Ordinary tests skip this opt-in external validation.
