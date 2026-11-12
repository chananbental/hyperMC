#!/usr/bin/env bash
set -euo pipefail

bundle_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
mode="${1:-all}"
if [[ "$mode" == submit ]]; then
    shift
    mode="${1:-all}"
    case "$mode" in all|approximate|exact) ;; *) echo 'Submit mode must be all, approximate, or exact.' >&2; exit 2 ;; esac
    command -v sbatch >/dev/null || { echo 'sbatch is not on PATH.' >&2; exit 1; }
    # The activated Conda environment must be visible on compute nodes.
    exec sbatch --export=ALL --chdir="$bundle_dir" \
        "$bundle_dir/benchmarks.sbatch" "$mode" "${2:-}"
fi

case "$mode" in
    all) selectors=(--select-class=org.openu.hyperltl.mc.ModelCheckerTest --select-class=org.openu.hyperltl.mc.ExactModelCheckerTest) ;;
    approximate) selectors=(--select-class=org.openu.hyperltl.mc.ModelCheckerTest) ;;
    exact) selectors=(--select-class=org.openu.hyperltl.mc.ExactModelCheckerTest) ;;
    discover) selectors=(--select-class=org.openu.hyperltl.mc.ModelCheckerTest --select-class=org.openu.hyperltl.mc.ExactModelCheckerTest) ;;
    *) echo 'Usage: bash run-benchmarks.sh [submit [all|approximate|exact]|all|approximate|exact|discover] [output-directory]' >&2; exit 2 ;;
esac

if [[ "$mode" == discover ]]; then
    exec java -cp "$bundle_dir/classes:$bundle_dir/lib/*" \
        org.junit.platform.console.ConsoleLauncher discover "${selectors[@]}" --disable-ansi-colors
fi

if [[ -z "${SLURM_JOB_ID:-}" ]]; then
    echo 'No Slurm allocation. Use: bash run-benchmarks.sh submit [all|approximate|exact]' >&2
    echo 'Or run inside an interactive srun allocation.' >&2
    exit 1
fi

for tool in java ltl2tgba autfilt; do
    command -v "$tool" >/dev/null || { echo "Missing $tool on PATH; activate your environment." >&2; exit 1; }
done

# The existing tests write relative to their working directory. Isolate each run.
output_dir="${2:-$bundle_dir/results/$(date +%Y%m%d-%H%M%S)-$$}"
mkdir -p -- "$output_dir"
cd -- "$output_dir"
echo "Benchmark results: $PWD"
java -version
ltl2tgba --version
autfilt --version

exec java -Xmx4g "-XX:ActiveProcessorCount=${SLURM_CPUS_PER_TASK:-1}" -Djava.awt.headless=true \
    -Djunit.jupiter.execution.parallel.enabled=false \
    -cp "$bundle_dir/classes:$bundle_dir/lib/*" \
    org.junit.platform.console.ConsoleLauncher execute \
    "${selectors[@]}" --fail-if-no-tests --disable-ansi-colors \
    --reports-dir=junit-reports
