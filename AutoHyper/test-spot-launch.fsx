#load "src/FsOmegaLib/Util.fs"

open System
open System.IO
open FsOmegaLib.Util.SubprocessUtil

// Run with: dotnet fsi AutoHyper/test-spot-launch.fsx
// Spot must be on the Linux PATH (native Linux or WSL on Windows).
let work = Path.Combine(Path.GetTempPath(), "autohyper launch " + Guid.NewGuid().ToString("N"))
Directory.CreateDirectory(work) |> ignore
let hoa = Path.Combine(work, "formula with spaces.hoa")

let private check result =
    if result.ExitCode <> 0 then failwithf "Spot failed: %s" result.Stderr

try
    let formula = "G (\"quoted ap\" -> F \"other ap\")"
    executeSubprocess Map.empty "ltl2tgba" [ "-B"; formula; "-o"; spotPath hoa ] |> check
    let content = File.ReadAllText(hoa)
    if not (content.Contains("quoted ap") && content.Contains("other ap")) then
        failwith "Quoted atomic propositions were not preserved"
    executeSubprocess Map.empty "autfilt" [ "-pmax even"; spotPath hoa ] |> check
    let inclusion = executeSubprocess Map.empty "autfilt" [ "--included-in=" + spotPath hoa; spotPath hoa ]
    check inclusion
    if String.IsNullOrWhiteSpace inclusion.Stdout then failwith "Self inclusion must hold"
    let forq = executeSubprocess (Map.ofList [ "SPOT_CONTAINMENT_CHECK", "forq" ]) "autfilt"
                    [ "--included-in=" + spotPath hoa; spotPath hoa ]
    check forq
    if String.IsNullOrWhiteSpace forq.Stdout then failwith "FORQ self inclusion must hold"
    let empty = Path.Combine(work, "empty language.hoa")
    executeSubprocess Map.empty "ltl2tgba" [ "-B"; "0"; "-o"; spotPath empty ] |> check
    let isEmpty = executeSubprocess Map.empty "autfilt" [ "--is-empty"; spotPath empty ]
    if isEmpty.ExitCode <> 0 then failwith "The false formula must have an empty language"
    let full = Path.Combine(work, "full language.hoa")
    executeSubprocess Map.empty "autfilt" [ "--complement"; "-B"; spotPath empty; "-o"; spotPath full ] |> check
    let isFullEmpty = executeSubprocess Map.empty "autfilt" [ "--is-empty"; spotPath full ]
    if isFullEmpty.ExitCode <> 1 then failwith "The complement of an empty language must be nonempty"
    printfn "Spot launch regressions passed (quotes, paths, parity, inclusion, complement and emptiness)."
finally
    Directory.Delete(work, true)
