package ca.tjug.gradle.plugins.jlink_application;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;

import javax.inject.Inject;

import static ca.tjug.gradle.plugins.jlink_application.Os.javaBinaryName;

public abstract class RunImageTask extends DefaultTask {

    @InputDirectory
    public abstract DirectoryProperty getImageDirectory();

    @Input
    public abstract Property<String> getMainModule();

    @Input
    public abstract Property<String> getMainClass();

    @Input
    public abstract ListProperty<String> getVmOptions();

    @Inject
    public abstract ExecOperations getExecOperations();

    @TaskAction
    public void execute() {
        getExecOperations().javaexec(spec -> {
            spec.getMainClass().set(getMainClass());
            spec.getMainModule().set(getMainModule());
            spec.setJvmArgs(getVmOptions().get());
            spec.setExecutable(getImageDirectory().get().dir("bin").file(javaBinaryName()));
        });
    }

}
