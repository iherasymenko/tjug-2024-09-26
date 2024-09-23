package ca.tjug.gradle.plugins.jlink_application;

import org.gradle.api.GradleException;
import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.ArchiveOperations;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.file.FileTree;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Provider;
import org.gradle.work.DisableCachingByDefault;

import javax.inject.Inject;
import java.io.File;

@NonNullApi
@DisableCachingByDefault(because = "Not worth caching")
public abstract class ExtractJdkTransform implements TransformAction<TransformParameters.None> {

    @InputArtifact
    protected abstract Provider<FileSystemLocation> getInputArtifact();

    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();

    @Inject
    protected abstract ArchiveOperations getArchiveOperations();

    @Override
    public void transform(TransformOutputs outputs) {
        File jdkArchive = getInputArtifact().get().getAsFile();
        String fileName = jdkArchive.getName();
        File destPath = outputs.dir(fileName);
        FileTree tree;
        if (fileName.endsWith(".zip")) {
            tree = getArchiveOperations().zipTree(jdkArchive);
        } else if (fileName.endsWith(".tar.gz")) {
            tree = getArchiveOperations().tarTree(jdkArchive);
        } else {
            throw new GradleException("Unsupported archive format: " + fileName);
        }
        Logging.getLogger(ExtractJdkTransform.class).debug("Extracting {} to {}", fileName, destPath);
        getFileSystemOperations().sync(spec -> spec.from(tree).into(destPath));
    }

}