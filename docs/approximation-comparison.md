# Approximation benchmark comparison

Snapshot formula text includes the unused-trace tautologies now present in
`TestFormulas`. These additions preserve truth. Existing verdicts and timing
measurements are retained from the recorded comparison run; the text update is
not a new performance measurement.

Checked SCC and reachability approximations on the same six systems and 24
formulas as the exact benchmark suite: **288 approximate checks**. The reference
is the [144 exact results verified against AutoHyper](autohyper-benchmark-results.tsv).
All **288 approximate verdicts agree** with that reference after adding SCC
over-approximation refinement. There are no inconclusive results or errors.

| Strategy (under and over) | Agrees | Disagrees | Stalled | Exhausted | Errors |
| --- | ---: | ---: | ---: | ---: | ---: |
| Reachability | 144 | 0 | 0 | 0 | 0 |
| SCC | 144 | 0 | 0 | 0 | 0 |
| Total | 288 | 0 | 0 | 0 | 0 |

The [per-case results](approximation-benchmark-results.tsv) record the exact
verdict, outcome classification, approximate verdict when available, zero-based
refinement round, elapsed milliseconds, and exception details. Inconclusive
results are distinct from wrong verdicts and errors. Each check starts with fresh,
separate under- and over-strategy instances to prevent refinement state leaking
between cases or between the two strategies.

## Resolved SCC stalled cases

Previously six SCC cases stalled. All six now return SAT, agreeing with the exact
verdict. They are these two properties on systems 2, 4,
and 6:

- `forall pi1. forall pi2. G(a_pi1 -> F(a_pi2))`
- `forall pi1. forall pi2. G((a_pi1 & b_pi1) -> F(c_pi2))`

The initial SCC over-approximation completes each reachable SCC internally, including adding
self-loops at singleton SCCs with no original self-loop. For example, system 2's
unlabeled initial state must move to an `a,b,c`-labeled loop in the original
system, but can stutter forever in the over-approximation. An over-approximate
trace reaching `a` can then be paired with a trace staying forever at the initial
state, violating either universal property.

Previously the under-approximation expanded while the over-approximation stayed
fixed. The lower truth bound stayed false and the upper truth bound stayed true,
so the checker correctly reported stalled refinement.

The over-approximation now uses original outgoing transitions at source SCCs
strictly behind the advancing frontier. Its frontier level is the strategy's
retained frontier depth plus the supplied refinement round, so both `refine()`
and increasing rounds tighten it. Sources at or beyond the frontier retain the
SCC completion. Unreachable SCCs use original transitions from the start.

Every approximation preserves the original states, labels, initial state, and
transitions. Refinement removes only added edges, giving
`Traces(original) ⊆ Traces(over_next) ⊆ Traces(over_previous)`. Once the frontier
passes every reachable SCC's distance from the initial SCC, the over-approximation
equals the original graph. This maintains the model checker's upper/lower truth
bounds and preserves the intended HyperLTL semantics throughout refinement.
The existing five-round checker limit remains; larger models may still exhaust it.

Five formerly stalled cases finish in zero-based refinement round 1. System 6's
`G(a_pi1 -> F(a_pi2))` property finishes in round 2. Added regressions check all
six verdicts, monotone edge removal, preservation of original edges and labels,
convergence, explicit refinement, and deadlock preservation at the exact limit.

## Reproduce

Requires Java 25 and working WSL Spot. The test is opt-in; ordinary test runs
skip it. Refresh the AutoHyper-verified baseline before running this comparison
if benchmark systems or formulas change.

```powershell
$env:JAVA_TOOL_OPTIONS = '-Dapproximation.compare=true'
./gradlew.bat test --tests '*ApproximationBenchmarkComparisonTest' --console=plain
Remove-Item Env:JAVA_TOOL_OPTIONS
```

The test writes incremental results to `build/approximation-benchmarks/results.tsv`
and fails after completing all checks if any conclusive verdict disagrees or an
unexpected error occurs. Stalling and exhausting the existing refinement limit
are recorded without failing this soundness comparison.

These checks establish agreement on this benchmark set, not a general proof of
approximation soundness. Most reference verdicts are SAT; generated small models
with more false alternating formulas would broaden coverage.
