$autoHyper = ".\app\AutoHyper.exe"
$base = ".\benchmarks\my_tests\compare-to-my-solver"

$systemsDir = "$base\systems"
$formulasDir = "$base\formulas"
$resultsDir = "$base\results"
$excelFile = Join-Path $resultsDir "autohyper-results.xlsx"

$alternationTypes = @{
    "0" = @("AA", "EE")
    "1" = @("AE", "EA")
    "2" = @("AEA", "EAE")
    "3" = @("AEAE", "EAEA")
}

# One entry is added here for every run and exported to Excel at the end.
$excelResults = [System.Collections.Generic.List[object]]::new()

function Export-ResultsToExcel {
    param (
        [Parameter(Mandatory = $true)]
        [System.Collections.Generic.List[object]] $Results,

        [Parameter(Mandatory = $true)]
        [string] $Path
    )

    $excel = $null
    $workbook = $null
    $worksheet = $null

    try {
        # Uses the Microsoft Excel COM API available on Windows when Excel is installed.
        $excel = New-Object -ComObject Excel.Application
        $excel.Visible = $false
        $excel.DisplayAlerts = $false

        $workbook = $excel.Workbooks.Add()
        $worksheet = $workbook.Worksheets.Item(1)
        $worksheet.Name = "Results"

        # Required columns.
        $worksheet.Cells.Item(1, 1) = "System"
        $worksheet.Cells.Item(1, 2) = "Formula"
        $worksheet.Cells.Item(1, 3) = "Result"
        $worksheet.Cells.Item(1, 4) = "Total Time (s)"

        $row = 2
        foreach ($result in $Results) {
            $worksheet.Cells.Item($row, 1) = $result.System
            $worksheet.Cells.Item($row, 2) = $result.Formula
            $worksheet.Cells.Item($row, 3) = $result.Result
            $worksheet.Cells.Item($row, 4) = $result.TotalTimeSeconds
            $row++
        }

        # Basic formatting/readability.
        $header = $worksheet.Range("A1:D1")
        $header.Font.Bold = $true
        $worksheet.Columns.Item("A:D").AutoFit() | Out-Null
        $worksheet.Columns.Item("D").NumberFormat = "0.0000"
        $worksheet.Range("A1:D$($row - 1)").AutoFilter() | Out-Null

        # 51 = xlOpenXMLWorkbook (.xlsx)
        $absolutePath = [System.IO.Path]::GetFullPath($Path)
        Write-Host "Excel results written to: $absolutePath"
        
        $workbook.SaveAs("./result-1.xlsx", 51)
    }
    finally {
        if ($workbook -ne $null) {
            $workbook.Close($false)
        }
        if ($excel -ne $null) {
            $excel.Quit()
        }

        if ($worksheet -ne $null) {
            [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($worksheet)
        }
        if ($workbook -ne $null) {
            [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($workbook)
        }
        if ($excel -ne $null) {
            [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($excel)
        }

        [GC]::Collect()
        [GC]::WaitForPendingFinalizers()
    }
}

New-Item -ItemType Directory -Force -Path $resultsDir | Out-Null

for ($systemNumber = 1; $systemNumber -le 1; $systemNumber++) {
    $systemFile = "$systemsDir\system-$systemNumber.txt"

    # Sort the keys so the run order is always 0, 1, 2, 3 alternations.
    foreach ($alternationCount in ($alternationTypes.Keys | Sort-Object {[int]$_})) {
        foreach ($alternationType in $alternationTypes[$alternationCount]) {
            for ($formulaNumber = 1; $formulaNumber -le 1; $formulaNumber++) {
                $formulaFile = "$formulasDir\$alternationCount-alternations\$alternationType\formula-$formulaNumber.hq"
                $formulaName = "$alternationCount-alternations/$alternationType/formula-$formulaNumber"

                for ($runNumber = 1; $runNumber -le 1; $runNumber++) {
                    $outDir = "$resultsDir\system-$systemNumber\$alternationCount-alternations\$alternationType\formula-$formulaNumber"
                    New-Item -ItemType Directory -Force -Path $outDir | Out-Null

                    $outFile = "$outDir\run-$runNumber.txt"

                    Write-Host "Running system-$systemNumber | $alternationCount-alternations | $alternationType | formula-$formulaNumber | run-$runNumber"

                    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
                    $exitCode = -1

                    try {
                        & $autoHyper `
                            --witness `
                            --log `
                            --explicit `
                            --incl-spot `
                            $systemFile `
                            $formulaFile `
                            *> $outFile

                        $exitCode = $LASTEXITCODE
                    }
                    catch {
                        # Preserve the exception in the normal per-run output file as well.
                        $_ | Out-String | Set-Content -Path $outFile
                    }
                    finally {
                        $stopwatch.Stop()
                    }

                    $resultValue = "FAILED"
                    $totalTimeSeconds = [Math]::Round($stopwatch.Elapsed.TotalSeconds, 4)

                    try {
                        # Get-Content detects the BOM in the solver output (the examples are UTF-16).
                        $outputText = Get-Content -Path $outFile -Raw

                        $hasErrorBlock = $outputText -match '(?im)^=+\s*ERROR\s*=+\s*$'
                        $resultMatch = [regex]::Match($outputText, '(?im)^\s*(SAT|UNSAT)\s*$')
                        $timeMatch = [regex]::Match($outputText, '>\s*total-time:\s*[0-9.]+\s*ms\s*\(([0-9.]+)s\)')

                        # A run is successful only when the process returned 0, no ERROR block
                        # exists, and the solver actually printed SAT or UNSAT.
                        if (($exitCode -eq 0) -and (-not $hasErrorBlock) -and $resultMatch.Success) {
                            $resultValue = $resultMatch.Groups[1].Value.ToUpperInvariant()

                            # Prefer AutoHyper's own reported total-time for successful runs.
                            if ($timeMatch.Success) {
                                $totalTimeSeconds = [double]::Parse(
                                    $timeMatch.Groups[1].Value,
                                    [System.Globalization.CultureInfo]::InvariantCulture
                                )
                            }
                        }
                    }
                    catch {
                        # If the output cannot be read/parsed, keep FAILED and the measured wall time.
                        $resultValue = "FAILED"
                    }

                    $excelResults.Add([PSCustomObject]@{
                        System           = $systemNumber
                        Formula          = $formulaName
                        Result           = $resultValue
                        TotalTimeSeconds = $totalTimeSeconds
                    })

                    Write-Host "  -> $resultValue | $totalTimeSeconds s"
                }
            }
        }
    }
}

Export-ResultsToExcel -Results $excelResults -Path $excelFile
