package ca.tjug.gradle.plugins.jlink_application;

import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.*;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

import java.util.List;
import java.util.Map;

@NonNullApi
public class JlinkApplicationPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        PluginContainer plugins = project.getPlugins();
        JlinkApplicationPluginExtension ext = project.getExtensions().create("jlinkApplication", JlinkApplicationPluginExtension.class);
        plugins.withType(ApplicationPlugin.class, _ -> {
            JavaApplication javaApplication = project.getExtensions().getByType(JavaApplication.class);
            ext.getMainClass().convention(javaApplication.getMainClass());
            ext.getMainModule().convention(javaApplication.getMainModule());
            ext.getVmOptions().convention(project.provider(javaApplication::getApplicationDefaultJvmArgs));
        });

        plugins.withType(JavaPlugin.class, _ -> {
            TaskContainer tasks = project.getTasks();
            TaskProvider<ImageTask> imageTask = tasks.register("image", ImageTask.class, task -> {
                Provider<Directory> imageDirectory = project.getLayout()
                        .getBuildDirectory()
                        .dir("images/" + project.getName());
                Provider<Map<String, String>> launchers = ext.getMainModule().zip(
                        ext.getMainClass(),
                        (mainModule, mainClass) -> Map.of(project.getName(), mainModule + "/" + mainClass)
                );
                task.setGroup(BasePlugin.BUILD_GROUP);
                task.setDescription("Builds a jlink image");
                task.getImageDirectory().convention(imageDirectory);
                task.getModulePath().convention(project.files(tasks.named(JavaPlugin.JAR_TASK_NAME), project.getConfigurations().named(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)));
                task.getLauncher().convention(launchers);
                task.getAddModules().convention(ext.getMainModule().map(List::of));
                task.getLauncherVmOptions().convention(ext.getVmOptions());
                task.getStripDebug().convention(ext.getStripDebug());
            });

            tasks.named(BasePlugin.ASSEMBLE_TASK_NAME).configure(task -> task.dependsOn(imageTask));

            tasks.register("imageRun", RunImageTask.class, task -> {
                task.setGroup(ApplicationPlugin.APPLICATION_GROUP);
                task.setDescription("Runs the project as a JVM application bundled with jlink");

                task.getImageDirectory().convention(imageTask.flatMap(ImageTask::getImageDirectory));

                task.getMainClass().convention(ext.getMainClass());
                task.getMainModule().convention(ext.getMainModule());
                task.getVmOptions().convention(ext.getVmOptions());
            });

            tasks.register("imageModules", ModulesImageTask.class, task -> {
                task.setGroup(HelpTasksPlugin.HELP_GROUP);
                task.setDescription("Displays modules of the project JVM application bundled with jlink");

                task.getImageDirectory().convention(imageTask.flatMap(ImageTask::getImageDirectory));
                task.getVmOptions().convention(ext.getVmOptions());
            });

        });
    }

}
