/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.gradle.plugin;

import javax.inject.Inject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.util.GradleVersion;

/** A Gradle plugin to develop Play application. */
public class PlayPlugin implements Plugin<Project> {

  private final ObjectFactory objectFactory;

  @Inject
  public PlayPlugin(ObjectFactory objectFactory) {
    this.objectFactory = objectFactory;
  }

  @Override
  public void apply(final Project project) {}

  /** Get Play version from Gradle Plugin MANIFEST.MF */
  private String getDefaultPlayVersion() {
    return System.getProperty("play.version", getClass().getPackage().getImplementationVersion());
  }

  static boolean isGradleVersionLessThan(String gradleVersion) {
    return GradleVersion.current().compareTo(GradleVersion.version(gradleVersion)) < 0;
  }

  static JavaPluginExtension javaPluginExtension(Project project) {
    return extensionOf(project, JavaPluginExtension.class);
  }

  private static <T> T extensionOf(ExtensionAware extensionAware, Class<T> type) {
    return extensionAware.getExtensions().getByType(type);
  }
}
