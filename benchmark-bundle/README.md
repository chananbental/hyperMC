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

Inside a compute-node allocation, run:

```bash
bash run-benchmarks.sh all > benchmarks.log 2>&1
```

Results are saved under `hypermc-benchmarks/results/`. No Gradle or Java
dependency downloads are needed on the server. AutoHyper is run separately.

After changing source code, rebuild on your local computer:

```powershell
.\gradlew.bat benchmarkBundle
```

Commit the updated ZIP along with your source changes.
See [the detailed instructions](../docs/benchmark-bundle.md) for individual test
selection and output files.
