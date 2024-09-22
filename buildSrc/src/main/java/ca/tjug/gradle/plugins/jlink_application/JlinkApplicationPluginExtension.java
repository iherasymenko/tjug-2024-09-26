package ca.tjug.gradle.plugins.jlink_application;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

public abstract class JlinkApplicationPluginExtension {

    public abstract Property<String> getMainModule();

    public abstract Property<String> getMainClass();

    public abstract ListProperty<String> getVmOptions();

    public abstract Property<Boolean> getStripDebug();

}