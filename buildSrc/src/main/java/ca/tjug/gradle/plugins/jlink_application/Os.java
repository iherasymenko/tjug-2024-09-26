package ca.tjug.gradle.plugins.jlink_application;

final class Os {

    static String javaBinaryName() {
        return System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
    }

}
