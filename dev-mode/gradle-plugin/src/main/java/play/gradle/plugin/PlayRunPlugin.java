/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.gradle.plugin;

import static org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE;
import static org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE;
import static org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE;
import static org.gradle.api.plugins.ApplicationPlugin.APPLICATION_GROUP;
import static org.gradle.api.plugins.JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME;
import static play.gradle.internal.Utils.isProjectComponent;
import static play.gradle.internal.Utils.mainSourceSet;
import static play.gradle.internal.Utils.playExtension;
import static play.gradle.plugin.PlayAssetsPlugin.ASSETS_SOURCE_NAME;
import static play.gradle.plugin.PlayAssetsPlugin.PLAY_ASSETS_USAGE;
import static play.gradle.plugin.PlayAssetsPlugin.PROCESS_ASSETS_TASK_NAME;
import static play.gradle.plugin.PlayAssetsPlugin.PUBLIC_SOURCE_NAME;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.gradle.api.Incubating;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import play.gradle.task.PlayRun;

@Incubating
public abstract class PlayRunPlugin implements Plugin<Project> {

  public static final int DEFAULT_HTTP_PORT = 9000;

  public static final String PLAY_RUN_TASK_NAME = "playRun";

  @Inject
  protected abstract ObjectFactory getObjectFactory();

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

  private FileCollection childProjectsClasspath(Configuration runtime, String libraryElements) {
    return runtime
        .getIncoming()
        .artifactView(
            view -> {
              view.setLenient(true);
              view.componentFilter(component -> isProjectComponent(component));
              view.getAttributes()
                  .attribute(
                      LIBRARY_ELEMENTS_ATTRIBUTE,
                      getObjectFactory().named(LibraryElements.class, libraryElements));
            })
        .getFiles();
  }

  private Configuration createAssetsPathConfiguration(final Project project) {
    var mainSourceSet = mainSourceSet(project);
    Configuration conf = project.getConfigurations().create("playAssetsPath");
    conf.setDescription("Assets directories of the projects running together in DEV-mode.");
    conf.setVisible(false);
    conf.setCanBeConsumed(false);
    conf.setCanBeResolved(true);
    conf.extendsFrom(
        project.getConfigurations().getByName(mainSourceSet.getImplementationConfigurationName()),
        project.getConfigurations().getByName(mainSourceSet.getRuntimeOnlyConfigurationName()));
    conf.getAttributes()
        .attribute(USAGE_ATTRIBUTE, getObjectFactory().named(Usage.class, PLAY_ASSETS_USAGE))
        .attribute(CATEGORY_ATTRIBUTE, getObjectFactory().named(Category.class, Category.LIBRARY));
    return conf;
  }

  private FileCollection childProjectsAssetsDirs(Configuration assetsPath) {
    return assetsPath
        .getIncoming()
        .artifactView(
            view -> {
              view.setLenient(true);
              view.componentFilter(component -> isProjectComponent(component));
            })
        .getFiles();
  }

  private List<DirectoryProperty> assetsDirs(final Project project) {
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
    Configuration assetsPath = createAssetsPathConfiguration(project);
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
              playRun.getClasses().from(mainSourceSet(project).getOutput());
              playRun.getAssetsDirs().from(assetsDirs(project));
              playRun.getAssetsDirs().from(childProjectsAssetsDirs(assetsPath));
              playRun.getAssetsPath().convention(playExtension(project).getAssets().getPath());
              playRun.getHttpPort().convention(DEFAULT_HTTP_PORT);
              playRun
                  .getDevSettings()
                  .convention(project.getObjects().mapProperty(String.class, String.class));

              var runtime =
                  project.getConfigurations().getByName(RUNTIME_CLASSPATH_CONFIGURATION_NAME);
              playRun.getRuntimeClasspath().from(filterNonChangingArtifacts(runtime));
              playRun.getClasses().from(childProjectsClasspath(runtime, LibraryElements.CLASSES));
              playRun.getClasses().from(childProjectsClasspath(runtime, LibraryElements.RESOURCES));

              playRun.dependsOn(project.getTasks().findByName(PROCESS_ASSETS_TASK_NAME));
            });
  }
}
