/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.gradle.plugin;

import static org.gradle.api.plugins.BasePlugin.ASSEMBLE_TASK_NAME;
import static org.gradle.api.plugins.JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;
import static play.gradle.internal.Utils.mainSourceSet;
import static play.gradle.internal.Utils.playExtension;
import static play.gradle.plugin.PlayRunPlugin.PLAY_RUN_TASK_NAME;

import jakarta.inject.Inject;
import org.gradle.api.Incubating;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.jetbrains.annotations.NotNull;

/** A Gradle plugin to work with Assets. */
@Incubating
public class PlayAssetsPlugin implements Plugin<Project> {

  public static final String ASSETS_JAR_TASK_NAME = "assetsJar";
  public static final String PROCESS_ASSETS_TASK_NAME = "processAssets";
  public static final String PROCESS_PUBLIC_TASK_NAME = "processPublicAssets";

  public static final String ASSETS_PATH_DEFAULT = "public";

  public static final String PUBLIC_SOURCE_NAME = "public";
  public static final String ASSETS_SOURCE_NAME = "assets";
  private final ObjectFactory objectFactory;

  @Inject
  public PlayAssetsPlugin(ObjectFactory objectFactory) {
    this.objectFactory = objectFactory;
  }

  @Override
  public void apply(@NotNull final Project project) {
    configureDefaultSettings(project);
    SourceDirectorySet publicSource = createPublicDirectorySet(project);
    TaskProvider<Copy> processPublic = createProcessPublicTask(project, publicSource);
    SourceDirectorySet assetsSource = createAssetsDirectorySet(project);
    TaskProvider<Task> processAssets = createProcessAssetsTask(project, processPublic);
    TaskProvider<Jar> assetsJar =
        createAssetsJarTask(project, publicSource, assetsSource, processAssets);
    project.getTasks().named(ASSEMBLE_TASK_NAME, assembleTask -> assembleTask.dependsOn(assetsJar));
    project.getDependencies().add(RUNTIME_ONLY_CONFIGURATION_NAME, project.files(assetsJar));
  }

  private void configureDefaultSettings(final Project project) {
    var playExtension = playExtension(project);
    playExtension.getAssets().getPath().convention(ASSETS_PATH_DEFAULT);
  }

  private SourceDirectorySet createAssetsDirectorySet(final Project project) {
    SourceDirectorySet assetsSource =
        objectFactory.sourceDirectorySet(ASSETS_SOURCE_NAME, "Compiled asset sources");
    assetsSource.srcDirs(
        "app/" + ASSETS_SOURCE_NAME, // Play layout
        "src/" + MAIN_SOURCE_SET_NAME + "/" + ASSETS_SOURCE_NAME); // Default layout
    assetsSource
        .getDestinationDirectory()
        .convention(
            project
                .getLayout()
                .getBuildDirectory()
                .dir("web/" + ASSETS_SOURCE_NAME + "/" + MAIN_SOURCE_SET_NAME));
    mainSourceSet(project)
        .getExtensions()
        .add(SourceDirectorySet.class, ASSETS_SOURCE_NAME, assetsSource);
    return assetsSource;
  }

  private SourceDirectorySet createPublicDirectorySet(final Project project) {
    SourceDirectorySet publicSource =
        objectFactory.sourceDirectorySet(PUBLIC_SOURCE_NAME, "Public assets");
    publicSource.srcDirs(
        PUBLIC_SOURCE_NAME, // Play layout
        "src/" + MAIN_SOURCE_SET_NAME + "/" + PUBLIC_SOURCE_NAME); // Default layout
    publicSource
        .getDestinationDirectory()
        .convention(
            project
                .getLayout()
                .getBuildDirectory()
                .dir("web/" + PUBLIC_SOURCE_NAME + "/" + MAIN_SOURCE_SET_NAME));
    mainSourceSet(project)
        .getExtensions()
        .add(SourceDirectorySet.class, PUBLIC_SOURCE_NAME, publicSource);
    return publicSource;
  }

  private TaskProvider<Copy> createProcessPublicTask(
      final Project project, SourceDirectorySet publicSource) {
    return project
        .getTasks()
        .register(
            PROCESS_PUBLIC_TASK_NAME,
            Copy.class,
            task -> {
              task.setDescription("Process public assets.");
              task.from(publicSource.getSourceDirectories());
              task.into(publicSource.getDestinationDirectory());
            });
  }

  private TaskProvider<Task> createProcessAssetsTask(
      final Project project, TaskProvider<Copy> processPublic) {
    return project
        .getTasks()
        .register(
            PROCESS_ASSETS_TASK_NAME,
            Task.class,
            task -> {
              task.setDescription("Process assets.");
              task.dependsOn(processPublic);
            });
  }

  private TaskProvider<Jar> createAssetsJarTask(
      final Project project,
      SourceDirectorySet publicSource,
      SourceDirectorySet assetsSource,
      TaskProvider<Task> processAssets) {
    return project
        .getTasks()
        .register(
            ASSETS_JAR_TASK_NAME,
            Jar.class,
            jar -> {
              jar.setDescription("Assembles the assets jar.");
              jar.setGroup(BasePlugin.BUILD_GROUP);
              jar.getArchiveClassifier().set("assets");
              jar.from(
                  publicSource.getDestinationDirectory().get(),
                  copySpec -> copySpec.into(playExtension(project).getAssets().getPath().get()));
              jar.from(
                  assetsSource.getDestinationDirectory().get(),
                  copySpec -> copySpec.into(playExtension(project).getAssets().getPath().get()));
              jar.dependsOn(processAssets);
              // Disable task for running application in DEV-Mode
              if (jar.getProject().getGradle().getStartParameter().getTaskNames().stream()
                  .anyMatch(task -> task.contains(PLAY_RUN_TASK_NAME))) {
                jar.setEnabled(false);
              }
            });
  }
}
