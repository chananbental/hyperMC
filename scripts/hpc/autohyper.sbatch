#!/usr/bin/env bash
#SBATCH --job-name=autohyper-benchmarks
#SBATCH --nodes=1
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=1
#SBATCH --mem=8G
#SBATCH --time=02:00:00
#SBATCH --output=%x-%j.out
#SBATCH --error=%x-%j.err

set -euo pipefail
# Submit from the repository root with your Conda environment activated.
cd -- "${SLURM_SUBMIT_DIR:?Submit this script using sbatch}"
export OMP_NUM_THREADS="${SLURM_CPUS_PER_TASK:-1}"
exec srun --ntasks=1 --cpus-per-task="${SLURM_CPUS_PER_TASK:-1}" --export=ALL \
    python AutoHyper/run-benchmarks.py --output "$PWD/build/autohyper-linux/job-$SLURM_JOB_ID" "$@"
