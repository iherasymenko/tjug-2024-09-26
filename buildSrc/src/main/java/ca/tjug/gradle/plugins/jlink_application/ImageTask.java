package ca.tjug.gradle.plugins.jlink_application;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public abstract class ImageTask extends DefaultTask {

    @Classpath
    public abstract Property<FileCollection> getModulePath();

    @Input
    public abstract ListProperty<String> getAddModules();

    @Input
    public abstract MapProperty<String, String> getLauncher();

    @Input
    public abstract ListProperty<String> getLauncherVmOptions();

    @Input
    @Optional
    public abstract Property<Boolean> getStripDebug();

    @InputDirectory
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getCrossTargetJdk();

    @OutputDirectory
    public abstract DirectoryProperty getImageDirectory();

    @Inject
    public abstract FileSystemOperations getFileSystemOperations();

    @TaskAction
    public void execute() throws IOException {
        ToolProvider jlink = ToolProvider.findFirst("jlink")
                .orElseThrow(() -> new GradleException("jlink is not available in this JDK"));
        Directory outputDirectory = getImageDirectory().get();
        getFileSystemOperations().delete(spec -> spec.delete(outputDirectory));

        Set<File> modulePathEntries = getModulePath()
                .get()
                .getFiles();

        String modulePath = Stream.concat(resolveCrossTargetJmodsFolder(), modulePathEntries.stream())
                .map(File::getAbsolutePath)
                .sorted()
                .collect(joining(File.pathSeparator));

        List<String> args = new ArrayList<>();
        args.addAll(List.of("--module-path", modulePath));
        args.addAll(List.of("--output", outputDirectory.getAsFile().getAbsolutePath()));
        if (getStripDebug().getOrElse(false)) {
            args.add("--strip-debug");
        }
        String addModules = String.join(",", getAddModules().get());
        if (!addModules.isEmpty()) {
            args.addAll(List.of("--add-modules", addModules));
        }
        for (var entry : getLauncher().get().entrySet()) {
            args.addAll(List.of("--launcher", entry.getKey() + "=" + entry.getValue()));
        }
        int exitCode = jlink.run(System.out, System.err, args.toArray(String[]::new));
        if (exitCode != 0) {
            throw new GradleException("jlinked exited with code: " + exitCode);
        }
        List<String> launcherVmOptions = getLauncherVmOptions().get();
        String vmOptionsString = String.join(" ", launcherVmOptions);
        for (var entry : getLauncher().get().entrySet()) {
            Path nixFile = outputDirectory.dir("bin")
                    .file(entry.getKey())
                    .getAsFile()
                    .toPath();
            Path winFile = outputDirectory.dir("bin")
                    .file(entry.getKey() + ".bin")
                    .getAsFile()
                    .toPath();
            if (Files.exists(nixFile)) {
                setArgsInLauncher(nixFile, vmOptionsString);
            } else if (Files.exists(winFile)) {
                setArgsInLauncher(winFile, vmOptionsString);
            } else {
                throw new AssertionError("No launcher found");
            }
        }
    }

    private Stream<File> resolveCrossTargetJmodsFolder() throws IOException {
        if (!getCrossTargetJdk().isPresent()) {
            return Stream.empty();
        }
        Path directory = getCrossTargetJdk().get().getAsFile().toPath();
        try (Stream<Path> walker = Files.walk(directory)) {
            List<Path> releaseFiles = walker.filter(path -> path.getFileName().toString().equals("release"))
                    .toList();
            for (Path releaseFile : releaseFiles) {
                try (InputStream is = Files.newInputStream(releaseFile)) {
                    Properties props = new Properties();
                    props.load(is);
                    String javaVersion = props.getProperty("JAVA_VERSION");
                    String osName = props.getProperty("OS_NAME");
                    String osArch = props.getProperty("OS_ARCH");
                    if (javaVersion != null) {
                        Path jdkRoot = releaseFile.getParent();
                        getLogger().info("Resolved cross target JDK: {}, {}/{} in {}", javaVersion, osName, osArch, jdkRoot);
                        return Stream.of(jdkRoot.resolve("jmods").toFile());
                    }
                } catch (Exception e) {
                    getLogger().info("Cannot read 'release' file", e);
                }
            }
        }
        throw new GradleException("Cannot find a valid 'release' file in " + directory + " or any of its subdirectories");
    }

    private static void setArgsInLauncher(Path launcherFile, String vmOptionsString) throws IOException {
        String body = Files.readString(launcherFile);
        String newBody = body.replace("JLINK_VM_OPTIONS=", "JLINK_VM_OPTIONS=" + vmOptionsString);
        Files.writeString(launcherFile, newBody);
    }

}
