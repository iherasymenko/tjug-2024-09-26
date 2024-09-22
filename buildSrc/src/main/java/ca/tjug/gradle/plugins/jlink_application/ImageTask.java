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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;

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

        List<String> args = new ArrayList<>();
        args.addAll(List.of("--module-path", getModulePath().get().getAsPath()));
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

    private static void setArgsInLauncher(Path launcherFile, String vmOptionsString) throws IOException {
        String body = Files.readString(launcherFile);
        String newBody = body.replace("JLINK_VM_OPTIONS=", "JLINK_VM_OPTIONS=" + vmOptionsString);
        Files.writeString(launcherFile, newBody);
    }

}
