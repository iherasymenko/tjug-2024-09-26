package ca.tjug.gradle.plugins.jlink_application;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.*;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@NonNullApi
public class JlinkApplicationPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        PluginContainer plugins = project.getPlugins();
        TaskContainer tasks = project.getTasks();
        JlinkApplicationPluginExtension ext = project.getExtensions().create("jlinkApplication", JlinkApplicationPluginExtension.class);
        plugins.withType(ApplicationPlugin.class, _ -> {
            JavaApplication javaApplication = project.getExtensions().getByType(JavaApplication.class);
            ext.getMainClass().convention(javaApplication.getMainClass());
            ext.getMainModule().convention(javaApplication.getMainModule());
            ext.getVmOptions().convention(project.provider(javaApplication::getApplicationDefaultJvmArgs));
        });

        BiConsumer<ImageTask, String> defaultImageTaskSettings = (task, outputFolderName) -> {
            Provider<Map<String, String>> launchers = ext.getMainModule().zip(
                    ext.getMainClass(),
                    (mainModule, mainClass) -> Map.of(project.getName(), mainModule + "/" + mainClass)
            );
            task.setGroup(BasePlugin.BUILD_GROUP);
            task.getModulePath().convention(project.files(tasks.named(JavaPlugin.JAR_TASK_NAME), project.getConfigurations().named(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)));
            task.getLauncher().convention(launchers);
            task.getAddModules().convention(ext.getMainModule().map(List::of));
            task.getLauncherVmOptions().convention(ext.getVmOptions());
            task.getStripDebug().convention(ext.getStripDebug());
            Provider<Directory> imageDirectory = project.getLayout()
                    .getBuildDirectory()
                    .map(it -> it.dir("images"))
                    .map(it -> it.dir(outputFolderName));
            task.getImageDirectory().convention(imageDirectory);
        };

        plugins.withType(JavaPlugin.class, _ -> {
            TaskProvider<ImageTask> imageTask = tasks.register("image", ImageTask.class, task -> {
                task.setDescription("Builds a jlink image using the current JDK");
                defaultImageTaskSettings.accept(task, project.getName());
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

        NamedDomainObjectContainer<Image> jlinkImages = project.container(Image.class, name -> project.getObjects().newInstance(Image.class, name));
        project.getExtensions().add("jlinkImages", jlinkImages);

        DependencyHandler dependencies = project.getDependencies();
        Attribute<Boolean> extractedArchive = registerExtractTransform(dependencies);

        jlinkImages.all(image -> {
            String capitalizedName = image.getCapitalizedName();
            Configuration conf = project.getConfigurations().create("jdkArchive" + capitalizedName, it -> it.getAttributes().attribute(extractedArchive, true));
            dependencies.addProvider(conf.getName(), image.getDependencyClassifier());

            TaskProvider<ImageTask> crossTargetImage = tasks.register("image" + capitalizedName, ImageTask.class, task -> {
                task.setDescription("Builds a jlink image using the JDK for " + image.name);
                task.getCrossTargetJdk().convention(project.getLayout().dir(project.provider(() -> project.files(conf).getSingleFile())));
                defaultImageTaskSettings.accept(task, image.name);
            });
            tasks.named(BasePlugin.ASSEMBLE_TASK_NAME).configure(task -> task.dependsOn(crossTargetImage));
        });
    }

    private static Attribute<Boolean> registerExtractTransform(DependencyHandler dependencies) {
        Attribute<Boolean> extractedArchive = Attribute.of("extracted", Boolean.class);
        Attribute<String> artifactType = Attribute.of("artifactType", String.class);

        dependencies.getArtifactTypes().maybeCreate("zip").getAttributes().attribute(extractedArchive, false);
        dependencies.getArtifactTypes().maybeCreate("tar.gz").getAttributes().attribute(extractedArchive, false);

        dependencies.registerTransform(ExtractJdkTransform.class, transform -> {
            transform.getFrom().attribute(artifactType, "zip").attribute(extractedArchive, false);
            transform.getTo().attribute(artifactType, "zip").attribute(extractedArchive, true);
        });

        dependencies.registerTransform(ExtractJdkTransform.class, transform -> {
            transform.getFrom().attribute(artifactType, "tar.gz").attribute(extractedArchive, false);
            transform.getTo().attribute(artifactType, "tar.gz").attribute(extractedArchive, true);
        });

        return extractedArchive;
    }

}
