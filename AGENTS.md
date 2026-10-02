# HyperLTL Model Checker

## Environment

- Java 25
- Gradle
- Spot 2.14.1
- Spot runs under WSL on Windows

## Architecture

The project implements HyperLTL model checking using Büchi automata.

HyperLTL quantifiers are processed using automata operations.

System approximations are used to reduce model-checking complexity:
- existential quantification uses under-approximations
- universal quantification uses over-approximations
- alternation may be handled through negation

SCC-based refinement is used to iteratively increase the system approximation.

## Correctness

Correctness is more important than performance.

Do not change automata semantics merely to simplify implementation.

When changing:
- quantifier handling
- complementation
- product construction
- emptiness checking
- SCC approximation
- witness reconstruction

explain why the change preserves the intended HyperLTL semantics.

## Testing

Run:

./gradlew test

after relevant changes.

Add regression tests for bugs whenever practical.