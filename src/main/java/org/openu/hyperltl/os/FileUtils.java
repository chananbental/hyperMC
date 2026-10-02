package org.openu.hyperltl.os;

import java.nio.file.Path;

import static org.openu.hyperltl.os.OsUtils.detectOS;

public class FileUtils {

    public static String toSpotPath(Path path) {
        String absolute = path.toAbsolutePath().toString();

        if (!detectOS().equals(OperatingSystemEnum.WINDOWS)) {
            return absolute;
        }

        // Example:
        // C:\Users\chana\AppData\Local\Temp\nba.hoa
        // becomes:
        // /mnt/c/Users/chana/AppData/Local/Temp/nba.hoa

        String normalized = absolute.replace("\\", "/");

        if (normalized.length() >= 2 && normalized.charAt(1) == ':') {
            char drive = Character.toLowerCase(normalized.charAt(0));
            return "/mnt/" + drive + normalized.substring(2);
        }

        return normalized;
    }
}
