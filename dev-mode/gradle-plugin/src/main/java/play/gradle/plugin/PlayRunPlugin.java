/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.gradle.plugin;

import static org.gradle.api.plugins.ApplicationPlugin.APPLICATION_GROUP;
import static org.gradle.api.plugins.JavaPlugin.COMPILE_JAVA_TASK_NAME;
import static org.gradle.api.plugins.JavaPlugin.PROCESS_RESOURCES_TASK_NAME;
import static org.gradle.api.plugins.JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME;
import static play.gradle.internal.Utils.filterProjectComponents;
import static play.gradle.internal.Utils.isPlayProject;
import static play.gradle.internal.Utils.isProjectComponent;
import static play.gradle.internal.Utils.mainSourceSet;
import static play.gradle.internal.Utils.playExtension;
import static play.gradle.plugin.PlayAssetsPlugin.ASSETS_SOURCE_NAME;
import static play.gradle.plugin.PlayAssetsPlugin.PROCESS_ASSETS_TASK_NAME;
import static play.gradle.plugin.PlayAssetsPlugin.PUBLIC_SOURCE_NAME;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.gradle.api.Incubating;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.scala.ScalaCompile;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import play.gradle.task.PlayRun;

@Incubating
public class PlayRunPlugin implements Plugin<Project> {

  public static final int DEFAULT_HTTP_PORT = 9000;

  public static final String PLAY_RUN_TASK_NAME = "playRun";

  @Override
  public void apply(@NotNull final Project project) {
    createRunTask(project);
  }

  private boolean isChangingArtifact(ComponentIdentifier component) {
    return isProjectComponent(component)
        || (component instanceof ComponentArtifactIdentifier
            && component.getDisplayName().endsWith("-assets.jar"));
  }

  private FileCollection filterNonChangingArtifacts(Configuration configuration) {
    return configuration
        .getIncoming()
        .artifactView(view -> view.componentFilter(__ -> !isChangingArtifact(__)))
        .getFiles();
  }

  private List<File> findClasspathDirectories(@Nullable Project project) {
    if (project == null) return List.of();
    return List.of(
        project
            .getTasks()
            .named(COMPILE_JAVA_TASK_NAME, JavaCompile.class)
            .get()
            .getDestinationDirectory()
            .get()
            .getAsFile(),
        project
            .getTasks()
            .named("compileScala", ScalaCompile.class)
            .get()
            .getDestinationDirectory()
            .get()
            .getAsFile(),
        project
            .getTasks()
            .named(PROCESS_RESOURCES_TASK_NAME, ProcessResources.class)
            .get()
            .getDestinationDir());
  }

  private List<DirectoryProperty> findAssetsDirectories(@Nullable Project project) {
    if (project == null) return List.of();
    var assets = new ArrayList<DirectoryProperty>();
    var publicSource =
        (SourceDirectorySet) mainSourceSet(project).getExtensions().findByName(PUBLIC_SOURCE_NAME);
    if (publicSource != null) {
      assets.add(publicSource.getDestinationDirectory());
    }
    var assetsSource =
        (SourceDirectorySet) mainSourceSet(project).getExtensions().findByName(ASSETS_SOURCE_NAME);
    if (assetsSource != null) {
      assets.add(assetsSource.getDestinationDirectory());
    }
    return assets;
  }

  private void createRunTask(final Project project) {
    project
        .getTasks()
        .register(
            PLAY_RUN_TASK_NAME,
            PlayRun.class,
            playRun -> {
              playRun.setDescription("Runs the Play application for local development.");
              playRun.setGroup(APPLICATION_GROUP);
              playRun.dependsOn(project.getTasks().findByName(JavaPlugin.CLASSES_TASK_NAME));
              playRun.getOutputs().upToDateWhen(task -> ((PlayRun) task).isUpToDate());
              playRun.getWorkingDir().convention(project.getLayout().getProjectDirectory());
              playRun.getClasses().from(findClasspathDirectories(project));
              playRun.getAssetsDirs().from(findAssetsDirectories(project));
              playRun.getAssetsPath().convention(playExtension(project).getAssets().getPath());
              playRun.getHttpPort().convention(DEFAULT_HTTP_PORT);
              playRun
                  .getDevSettings()
                  .convention(project.getObjects().mapProperty(String.class, String.class));

              var runtime =
                  project.getConfigurations().getByName(RUNTIME_CLASSPATH_CONFIGURATION_NAME);
              playRun.getRuntimeClasspath().from(filterNonChangingArtifacts(runtime));

              filterProjectComponents(runtime)
                  .forEach(
                      path -> {
                        Project child = project.findProject(path);
                        if (child == null) return;
                        playRun.getClasses().from(findClasspathDirectories(child));
                        if (isPlayProject(child)) {
                          playRun.getAssetsDirs().from(findAssetsDirectories(child));
                          playRun.dependsOn(child.getTasks().findByName(PROCESS_ASSETS_TASK_NAME));
                        }
                      });

              playRun.dependsOn(project.getTasks().findByName(PROCESS_ASSETS_TASK_NAME));
            });
  }
}
