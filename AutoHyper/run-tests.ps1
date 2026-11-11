$autoHyper = ".\app\AutoHyper.exe"
$base = ".\benchmarks\my_tests\compare-to-my-solver"

$systemsDir = "$base\systems"
$formulasDir = "$base\formulas"
$resultsDir = "$base\results"

$alternationTypes = @{
    "0" = @("AA", "EE")
    "1" = @("AE", "EA")
    "2" = @("AEA", "EAE")
    "3" = @("AEAE", "EAEA")
}

for ($systemNumber = 1; $systemNumber -le 6; $systemNumber++) {
    $systemFile = "$systemsDir\system-$systemNumber.txt"

    foreach ($alternationCount in $alternationTypes.Keys) {
        foreach ($alternationType in $alternationTypes[$alternationCount]) {
            for ($formulaNumber = 1; $formulaNumber -le 3; $formulaNumber++) {
                $formulaFile = "$formulasDir\$alternationCount-alternations\$alternationType\formula-$formulaNumber.hq"

                for ($runNumber = 1; $runNumber -le 5; $runNumber++) {
                    $outDir = "$resultsDir\system-$systemNumber\$alternationCount-alternations\$alternationType\formula-$formulaNumber"
                    New-Item -ItemType Directory -Force -Path $outDir | Out-Null

                    $outFile = "$outDir\run-$runNumber.txt"

                    Write-Host "Running system-$systemNumber | $alternationCount-alternations | $alternationType | formula-$formulaNumber | run-$runNumber"

                    & $autoHyper `
                        --witness `
                        --log `
                        --explicit `
                        --incl-spot `
                        $systemFile `
                        $formulaFile `
                        *> $outFile
                }
            }
        }
    }
}