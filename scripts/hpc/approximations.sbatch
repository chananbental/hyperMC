#!/bin/bash
#SBATCH --job-name=hypermc-approx
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=1
#SBATCH --mem=8G
#SBATCH --time=02:00:00
#SBATCH --output=%x-%j.out
#SBATCH --error=%x-%j.err

set -euo pipefail

## Submit from the project root in the group's work directory.
cd "${SLURM_SUBMIT_DIR:?Submit this script using sbatch}"
: "${JAVA_MODULE:?Set JAVA_MODULE to the university Java 25 module name}"
: "${SPOT_MODULE:?Set SPOT_MODULE to the university Spot 2.14.1 module name}"
module load "$JAVA_MODULE"
module load "$SPOT_MODULE"

## Keep dependencies, temporary files, and results out of the home directory.
export GRADLE_USER_HOME="$PWD/.gradle-hpc"
export TMPDIR="$PWD/build/hpc-tmp/$SLURM_JOB_ID"
mkdir -p "$TMPDIR"
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djava.io.tmpdir=$TMPDIR"

## Each job gets its own checkout/build directory; do not run concurrent jobs here.
java -version
ltl2tgba --version
autfilt --version
date -u
hostname

## A failed run must not leave a previous run's table looking current.
mkdir -p build/approximation-benchmarks
if [[ -f build/approximation-benchmarks/results.tsv ]]; then
    mv build/approximation-benchmarks/results.tsv \
       "build/approximation-benchmarks/results.before-$SLURM_JOB_ID.tsv"
fi
bash ./gradlew benchmarkApproximations --no-daemon --max-workers=1 \
    --no-configuration-cache --console=plain
