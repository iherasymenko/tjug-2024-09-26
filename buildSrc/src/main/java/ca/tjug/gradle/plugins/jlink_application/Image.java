package ca.tjug.gradle.plugins.jlink_application;

import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import javax.inject.Inject;
import java.util.Map;
import java.util.Objects;

public abstract class Image {
    final String name;

    @Inject
    public Image(String name) {
        this.name = Objects.requireNonNull(name, "name");
        if (this.name.isBlank()) {
            throw new IllegalArgumentException("'name' must not be blank");
        }
    }

    public abstract Property<String> getJdkArchive();

    public abstract Property<String> getGroup();

    Provider<Map<String, String>> getDependencyClassifier() {
        return getJdkArchive().zip(getGroup(), ((jdkArchiveName, group) -> {
            String ext;
            String fileName;
            if (jdkArchiveName.endsWith(".zip")) {
                ext = "zip";
                fileName = jdkArchiveName.substring(0, jdkArchiveName.length() - ".zip".length());
            } else if (jdkArchiveName.endsWith(".tar.gz")) {
                ext = "tar.gz";
                fileName = jdkArchiveName.substring(0, jdkArchiveName.length() - ".tar.gz".length());
            } else {
                throw new GradleException("Unsupported archive format: " + jdkArchiveName);
            }
            return Map.of("group", group, "name", fileName, "ext", ext);
        }));
    }

    String getCapitalizedName() {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

}