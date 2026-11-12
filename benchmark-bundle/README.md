# Ready-to-run benchmarks

`hypermc-benchmarks.zip` includes compiled main/test classes, all required Java
dependencies, and the Linux launcher. Commit this directory to make the bundle
available through a normal Git clone or pull.

On the server, activate your Java 25 / Spot environment. From the repository root:

```bash
cd benchmark-bundle
unzip -o hypermc-benchmarks.zip
cd hypermc-benchmarks
bash run-benchmarks.sh discover
```

From the login node, submit the job with your Conda environment activated:

```bash
bash run-benchmarks.sh submit all
squeue --me
```

This requests one CPU, 8 GiB RAM and two hours through `sbatch`, then launches
the benchmarks through `srun`. These are starting estimates; Java uses a 4 GiB
heap. No GPU is needed. Keep the checkout and Conda environment in the group's
shared `work` directory, accessible to compute nodes.

Results are saved under `hypermc-benchmarks/results/job-JOBID/`. Logs are
`hypermc-benchmarks-JOBID.out` and `.err` in the extracted bundle. No Gradle or Java
dependency downloads are needed on the server. AutoHyper is run separately.

After changing source code, rebuild on your local computer:

```powershell
.\gradlew.bat benchmarkBundle
```

Commit the updated ZIP along with your source changes.
See [the detailed instructions](../docs/benchmark-bundle.md) for individual test
selection and output files.
