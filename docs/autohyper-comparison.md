# Exact checker comparison with AutoHyper

Run all external checks with the [combined Gradle validation task](validation.md).

The checked-in Java benchmark formulas and matching local AutoHyper `.hq` files
now explicitly mention every quantified trace. Ten formula pairs needed clauses
of the form `G(a_pi | !a_pi)` for otherwise unused variables. These clauses are
tautologies, so they preserve the full quantified formula, including its prefix,
while avoiding AutoHyper's missing-variable lookup during initialization.
Reference snapshot formula text has been synchronized; verdicts are unchanged.

The [seeded small-system comparison](generated-comparison.md) adds 64 cases,
including 36 false alternating properties: all exact and approximate verdicts
agree with AutoHyper.

The subsequent [approximation comparison](approximation-comparison.md) checks
SCC and reachability on this benchmark set: after SCC over-refinement was added,
all 288 results are conclusive and agree, with no errors or incorrect verdicts.

## Existing benchmark suite

All **144 existing benchmark cases agree** with AutoHyper: six systems times
24 formulas from `TestSystems` and `TestFormulas`. Both solvers returned a verdict
for every case: **138 SAT, 6 UNSAT, zero errors, zero disagreements**.
The run used exact approximations and AutoHyper `--explicit --comp --no-bisim`.
The per-case snapshot is [autohyper-benchmark-results.tsv](autohyper-benchmark-results.tsv).
Generated explicit systems, formulas, and individual AutoHyper logs are in
`build/autohyper-benchmarks`.

The exporter preserves all declared Boolean propositions and state valuations,
initial states, and transitions. It adds tautologies referencing quantified trace
variables to accommodate the local AutoHyper parser's unused-variable limitation;
this preserves the formulas' truth. No checker semantics changed for this run.

```powershell
$env:JAVA_TOOL_OPTIONS = '-Dautohyper.compare=true'
./gradlew.bat test --tests '*AutoHyperComparisonTest.compareExisting144BenchmarkCasesWithAutoHyper' --console=plain
Remove-Item Env:JAVA_TOOL_OPTIONS
```

These benchmarks have relatively few negative verdicts; the small-system suite
below supplies additional false alternating cases. Agreement is regression
evidence, not a proof of correctness for arbitrary models and formulas.

## Small-system comparison

After the correction, **all 36 cases agree** with AutoHyper, including prefixes
with zero, one, two, and three quantifier alternations. The original 24 cases
had 17 disagreements; their pre-fix results are recorded below.

Comparisons use AutoHyper `--explicit --comp --no-bisim` and hyperMC's
`ExactApproximationStrategy` for both approximations. AutoHyper is assumed correct.
All AutoHyper processes succeeded; hyperMC returned a verdict in every case.
The original failures were present with exact domains, independently of approximation.

The models have one Boolean proposition `a`:

- `always-a`: one initial self-looping state labeled `a`.
- `never-a`: one initial self-looping state with no labels.
- `branching`: initial state without `a`, branching to two self-looping states,
  one labeled `a` and one without labels. Initial labels are part of the trace.

In this pre-fix table each cell is **AutoHyper / hyperMC**. SAT means the formula holds.

| Formula | always-a | never-a | branching |
| --- | --- | --- | --- |
| `forall p. G(a_p)` | SAT / UNSAT | UNSAT / SAT | UNSAT / SAT |
| `exists p. G(a_p)` | SAT / UNSAT | UNSAT / SAT | UNSAT / SAT |
| `forall p. F(a_p)` | SAT / UNSAT | UNSAT / SAT | UNSAT / UNSAT |
| `exists p. F(a_p)` | SAT / UNSAT | UNSAT / SAT | SAT / UNSAT |
| `forall p. forall q. G(a_p <-> a_q)` | SAT / UNSAT | SAT / UNSAT | UNSAT / UNSAT |
| `exists p. exists q. F(a_p & a_q)` | SAT / UNSAT | UNSAT / SAT | SAT / UNSAT |
| `forall p. exists q. G(a_p <-> a_q)` | SAT / SAT | SAT / SAT | SAT / UNSAT |
| `exists p. forall q. G(a_p <-> a_q)` | SAT / SAT | SAT / SAT | UNSAT / UNSAT |

## Findings

The single existential cases demonstrated reversed verdict handling:
the checker built the positive body automaton but reported an accepting trace
as a counterexample and an empty product as satisfaction.

The universal cases require searching for violations of the body; the checker
previously started from the positive body for every prefix. An intermediate empty
product is also not an unconditional proof of the complete quantified formula.

The branching `forall p. exists q. G(a_p <-> a_q)` case is a minimal alternating
regression: every trace can choose itself as `q`, so it must hold. AutoHyper's
system product substitutes the eliminated trace's valuation and removes its
propositions before later complementation. hyperMC's intersection retained those
propositions. The observed failure is consistent with this quantifier-elimination
error; these comparisons alone do not isolate it from the polarity errors.

## Correction and semantic invariant

The checker tracks whether its NBA represents the remaining subformula or its
negation. It starts with the negated body for an innermost universal quantifier,
and the positive body for an innermost existential quantifier. Before each
elimination it complements only when the required polarity changes.

Each product restricts the eliminated variable to system traces. It substitutes
the source state's valuation into transition guards and removes that variable's
APs. Nondeterministic system paths implement existential projection. The product
retains its Büchi intersection acceptance condition and state decomposition;
removing propositions changes the alphabet, not the accepting runs compatible
with a system path. Universal elimination uses
`not(forall x. P) = exists x. not P`, restricted to the system trace domain.
Only after all quantifiers are eliminated is emptiness interpreted: an empty
violation language proves a universal sentence, while a nonempty satisfaction
language proves an existential sentence. Intermediate emptiness never terminates
quantifier processing, including when a domain has no infinite paths.

Approximation is evaluated in both directions. Assuming each strategy provides
`T_under ⊆ T_original ⊆ T_over`, choosing under-domains for existential quantifiers
and over-domains for universal quantifiers produces a lower truth bound. Reversing
the choices produces an upper truth bound. This follows from monotonicity in each
quantified domain and remains valid with alternation. The checker proves true
only when the lower evaluation is true, and false only when the upper evaluation
is false. Otherwise it refines; the existing five-round limit and stalled-refinement
errors remain.

Reachability under-approximation now retains only original transitions, leaving
cut frontiers as dead ends rather than inventing self-loops. Its over-approximation
is the full reachable induced subgraph, which has exactly the original initial
trace language. A larger depth-limited subgraph did not contain every original
trace and was therefore not a valid over-approximation. A chain ending in an
`a`-labeled loop tests both problems and eventual existential refinement.

False formulas beginning with a universal block can expose concrete traces from
that outermost block. Reconstruction stops at complementation because the earlier
product-state decomposition no longer describes the new automaton. It preserves
every time step, including repeated projected states, and emits the loop entry
once rather than duplicating it at the end of the prefix. A concrete tuple is no
longer used to decide quantified truth. False existential-leading formulas return
no counterexample tuple. Lasso extraction checks all reachable accepting states
rather than stopping at a transient accepting state without a cycle.

Spot is launched through `wsl --exec`, passing formulas directly rather than
interpreting parentheses and operators through the WSL shell.

`QuantifierSemanticsTest` covers polarity, alternation, empty trace domains, SCC
refinement, and counterexample path validity. The added AutoHyper cases reference
otherwise unused variables through tautologies because the local AutoHyper input
parser fails on unused quantified variables; the tautologies do not change truth.

## Reproduce

Requires the existing Windows AutoHyper build in `AutoHyper/app`, its working
`paths.json`, Java 25, .NET 8, and WSL Spot. The local AutoHyper build already
invokes Spot through WSL. The comparison is opt-in and deliberately fails when
verdicts disagree; ordinary test runs skip it. It now passes all 36 cases.

```powershell
$env:JAVA_TOOL_OPTIONS = '-Dautohyper.compare=true'
./gradlew.bat test --tests org.openu.hyperltl.mc.AutoHyperComparisonTest --console=plain
Remove-Item Env:JAVA_TOOL_OPTIONS
```

Inputs, AutoHyper logs, and `results.tsv` are written under
`build/autohyper-comparison`. The test completes all comparisons before asserting
agreement, making all discrepancies available from one run.
