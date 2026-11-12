#!/usr/bin/env bash
#SBATCH --job-name=hypermc-benchmarks
#SBATCH --nodes=1
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=1
#SBATCH --mem=8G
#SBATCH --time=02:00:00
#SBATCH --output=%x-%j.out
#SBATCH --error=%x-%j.err

set -euo pipefail

# Submit through run-benchmarks.sh submit, or sbatch from the extracted bundle.
# Activate Conda before submission; --export=ALL inherits its environment.
# Slurm spools this script, so use the submit directory to locate the bundle.
cd -- "${SLURM_SUBMIT_DIR:?Submit this script using sbatch}"
mode="${1:-all}"
output_dir="${2:-$PWD/results/job-$SLURM_JOB_ID}"
mkdir -p -- "$output_dir"
output_dir="$(cd -- "$output_dir" && pwd)"
export TMPDIR="$output_dir/tmp"
mkdir -p -- "$TMPDIR"
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djava.io.tmpdir=$TMPDIR"
export OMP_NUM_THREADS="${SLURM_CPUS_PER_TASK:-1}"

date -u
hostname
echo "Slurm job: $SLURM_JOB_ID; CPUs: ${SLURM_CPUS_PER_TASK:-1}; memory: ${SLURM_MEM_PER_NODE:-unknown} MiB"
exec srun --ntasks=1 --cpus-per-task="${SLURM_CPUS_PER_TASK:-1}" --export=ALL \
    bash "$PWD/run-benchmarks.sh" "$mode" "$output_dir"
