package org.openu.hyperltl.os;

public class OsUtils {

    public static OperatingSystemEnum detectOS() {
        return switch (System.getProperty("os.name").toLowerCase()) {
            case "linux" -> OperatingSystemEnum.LINUX;
            case "windows", "windows 11" -> OperatingSystemEnum.WINDOWS;
            case "mac" -> OperatingSystemEnum.MAC;
            case "solaris" -> OperatingSystemEnum.SOLARIS;
            default -> OperatingSystemEnum.OTHER;
        };
    }
}