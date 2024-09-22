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
        plugins.withType(JavaPlugin.class, _ -> {
            TaskContainer tasks = project.getTasks();
            TaskProvider<ImageTask> imageTask = tasks.register("image", ImageTask.class, task -> {
                Provider<Directory> imageDirectory = project.getLayout()
                        .getBuildDirectory()
                        .dir("images/" + project.getName());
                task.setGroup(BasePlugin.BUILD_GROUP);
                task.setDescription("Builds a jlink image");
                task.getImageDirectory().convention(imageDirectory);
                task.getModulePath().convention(project.files(tasks.named(JavaPlugin.JAR_TASK_NAME), project.getConfigurations().named(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)));
            });
            plugins.withType(ApplicationPlugin.class, _ -> {
                JavaApplication javaApplication = project.getExtensions().getByType(JavaApplication.class);
                imageTask.configure(task -> {
                    var launchers = javaApplication.getMainModule().zip(
                            javaApplication.getMainClass(),
                            (mainModule, mainClass) -> Map.of(project.getName(), mainModule + "/" + mainClass)
                    );
                    task.getLauncher().convention(launchers);
                    task.getAddModules().convention(javaApplication.getMainModule().map(List::of));
                    task.getLauncherVmOptions().convention(project.provider(javaApplication::getApplicationDefaultJvmArgs));
                });
            });
            tasks.named(BasePlugin.ASSEMBLE_TASK_NAME).configure(task -> task.dependsOn(imageTask));
        });
    }

}
