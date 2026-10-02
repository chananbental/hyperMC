package org.openu.hyperltl.spot;

import org.openu.hyperltl.automaton.HoaParser;
import org.openu.hyperltl.automaton.HoaWriter;
import org.openu.hyperltl.automaton.NBA;
import org.openu.hyperltl.os.OperatingSystemEnum;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.openu.hyperltl.os.FileUtils.toSpotPath;
import static org.openu.hyperltl.os.OsUtils.detectOS;
import static org.openu.hyperltl.utils.Timer.runAndTime;

public class SpotAutomatonUtil {
    private final HoaParser parser = new HoaParser();
    private final HoaWriter writer = new HoaWriter();
    private final OperatingSystemEnum os = detectOS();

    public NBA ltlToNba(String ltlFormula) {
        String hoa = run(List.of("ltl2tgba", "-B", "-H", ltlFormula));
        return parser.parse(hoa);
    }

    public NBA complement(NBA nba) {
        try {
            Path temp = Files.createTempFile("nba-", ".hoa");
            try {
                Files.writeString(temp, writer.write(nba), StandardCharsets.UTF_8);
                String hoa = run(List.of("autfilt", "--complement", "-B", toSpotPath(temp)));

                return parser.parse(hoa);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isEmpty(NBA nba) {
        boolean isEmpty = true;

        try {
            if (!nba.transitions().isEmpty()) {
                Path temp = Files.createTempFile("nba-", ".hoa");
                try {
                    Files.writeString(temp, writer.write(nba), StandardCharsets.UTF_8);
                    ProcessResult pr = runProcess(List.of("autfilt", "--is-empty", toSpotPath(temp)));
                    String text = (pr.stdout() + "\n" + pr.stderr()).trim().toLowerCase();
                    if (pr.exitCode == 0) {
                        isEmpty = true;
                    } else if (pr.exitCode == 1) {
                        isEmpty = false;
                    } else {
                        throw new IllegalStateException("Could not determine emptiness from Spot output: " + text);
                    }
                } finally {
                    Files.deleteIfExists(temp);
                }
            }

            return isEmpty;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String run(List<String> command) {
        ProcessResult result = runProcess(command);

        if (result.exitCode() != 0) {
            throw new IllegalStateException("Command failed: " + String.join(" ", command) + "\nstdout:\n" + result.stdout() + "\nstderr:\n" + result.stderr());
        }

        return result.stdout();
    }

    private ProcessResult runProcess(List<String> command) {
        List<String> fullCommand = new ArrayList<>();
        if (os.equals(OperatingSystemEnum.WINDOWS)) {
            fullCommand.add("wsl");
        }
        fullCommand.addAll(command);

        return runAndTime(() -> {
            ProcessResult result;

            try {
                ProcessBuilder pb = new ProcessBuilder(fullCommand);
                Process p = pb.start();
                String stdout = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                String stderr = new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                int exit = p.waitFor();
                result = new ProcessResult(exit, stdout, stderr);
            } catch (Exception e) {
                result = new ProcessResult(-1, null, e.getMessage());
            }

            return result;
        }, elapsedTime -> IO.println(elapsedTime + " time to run " + String.join(" ", fullCommand) + " " + elapsedTime));
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) {
    }
}