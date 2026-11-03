# Seeded small-system comparison

The subsequent [counterexample validation](counterexample-validation.md) verifies
60 returned counterexamples against original transitions and AutoHyper with fixed
outer trace domains. All pass structural and quantified-semantic checks.

Seed **20261012** produces eight total transition systems with 1–4 states and
Boolean propositions `a` and `b`, and eight formulas per system: **64 cases**.
All exact verdicts agree with AutoHyper `--explicit --comp --no-bisim`.
All **128 SCC/reachability checks** are conclusive and agree with the exact
verdicts. There are zero mismatches, unexpected errors, or inconclusive results.

The reference verdicts are **19 SAT and 45 UNSAT**, including **36 false
alternating properties**. The [result snapshot](generated-comparison-results.tsv)
records the seed, case identifier, state count, prefix, original formula, and all
four solver verdicts.

| Prefix | SAT | UNSAT |
| --- | ---: | ---: |
| AA | 4 | 4 |
| EE | 3 | 5 |
| AE | 5 | 3 |
| EA | 2 | 6 |
| AEA | 1 | 7 |
| EAE | 2 | 6 |
| AEAE | 2 | 6 |
| EAEA | 0 | 8 |

## Generation and coverage

The generator uses Java `Random` with a fixed seed. State labels and transitions
are randomized in a fixed order, and each state has at least one successor.
The larger models also include reachable branching and multiple self-looping
cycles. Two models are generated at each state count.

Formula templates cover next (`X`), until (`U`), recurrence (`GF`), persistence
(`FG`), response, and trace relations. Signs and constraints on intermediate
trace variables are randomized. Templates include an opposite-label relation
at time zero to ensure false cases even on nonempty total systems. Every prefix
is exercised eight times. This is a bounded template-based generator, not an
exhaustive enumeration of systems or formulas.

The same generated model and formula are used for all solvers. AutoHyper input
uses the existing exporter, which preserves valuations and transitions and adds
only tautologies for its parser's unused-variable requirement. Approximation
strategies start fresh for each case, with separate under/over instances.
No production checker semantics changed in this work.

## Reproduce and preserve failures

Requires Java 25, WSL Spot, and the local Windows AutoHyper build/configuration.
Ordinary tests skip this opt-in comparison.

```powershell
$env:JAVA_TOOL_OPTIONS = '-Dgenerated.compare=true -Dgenerated.seed=20261012'
./gradlew.bat test --tests '*GeneratedComparisonTest' --console=plain
Remove-Item Env:JAVA_TOOL_OPTIONS
```

Change `generated.seed` to explore another reproducible batch. Outputs are saved
under `build/generated-comparison/<seed>/`. Every case directory contains:

- `system.json`: native hyperMC model format, loadable by `SystemModelLoader`.
- `system.txt`: AutoHyper explicit-state input.
- `formula.txt` and `formula.hq`: original and AutoHyper formulas.
- `seed.txt` and `autohyper.log`: seed and reference solver output.

Any discrepancy additionally writes `FAILURE.txt` in its case directory and a
row in `failures.tsv`. These saved inputs provide a concrete regression case;
the test completes all cases before asserting agreement. Unexpected errors count
as failures. Legitimate approximation stalling or exhaustion is recorded as
inconclusive and is never treated as agreement. No failing regression cases were
found with the default seed.

Agreement on this batch broadens negative alternating coverage but does not prove
correctness for arbitrary systems or formulas. Further seeds and additional
formulas remain useful follow-up work.
