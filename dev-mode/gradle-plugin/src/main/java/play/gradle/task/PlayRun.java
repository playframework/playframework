/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.gradle.task;

import java.util.ArrayList;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.Incubating;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.deployment.internal.DeploymentHandle;
import org.gradle.deployment.internal.DeploymentRegistry;
import org.gradle.deployment.internal.DeploymentRegistry.ChangeBehavior;
import org.gradle.work.DisableCachingByDefault;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.gradle.internal.PlayRunHandle;
import play.gradle.internal.PlayRunParams;

/** Task to run a Play application in DEV-mode. */
@Incubating
@DisableCachingByDefault(because = "Application should always run")
public abstract class PlayRun extends DefaultTask {

  private static final Logger LOGGER = LoggerFactory.getLogger(PlayRun.class);

  private final DeploymentRegistry deploymentRegistry;

  @Internal
  public abstract DirectoryProperty getWorkingDir();

  @Classpath
  public abstract ConfigurableFileCollection getRuntimeClasspath();

  @Classpath
  @Incremental
  public abstract ConfigurableFileCollection getClasses();

  @InputFiles
  @Incremental
  @PathSensitive(PathSensitivity.RELATIVE)
  public abstract ConfigurableFileCollection getAssetsDirs();

  @Input
  public abstract Property<String> getAssetsPath();

  @Input
  public abstract Property<Integer> getHttpPort();

  @Input
  public abstract MapProperty<String, String> getDevSettings();

  @Inject
  public PlayRun(DeploymentRegistry deploymentRegistry) {
    this.deploymentRegistry = deploymentRegistry;
  }

  @Internal
  public boolean isUpToDate() {
    DeploymentHandle deploymentHandle = deploymentRegistry.get(getPath(), DeploymentHandle.class);
    return deploymentHandle != null;
  }

  @TaskAction
  public void run(InputChanges changes) {
    String id = getPath();
    PlayRunHandle runHandle = deploymentRegistry.get(id, PlayRunHandle.class);
    if (runHandle == null) {
      PlayRunParams params =
          new PlayRunParams(
              new ArrayList<>(getRuntimeClasspath().getFiles()),
              new ArrayList<>(getClasses().getFiles()),
              new ArrayList<>(getAssetsDirs().getFiles()),
              getAssetsPath().get(),
              getWorkingDir().get().getAsFile(),
              getHttpPort().get(),
              getDevSettings().get());
      deploymentRegistry.start(id, ChangeBehavior.BLOCK, PlayRunHandle.class, params);
    } else {
      if (!changes.isIncremental()) {
        LOGGER.info("Reload application by no incremental changes");
      } else if (changes.getFileChanges(getClasses()).iterator().hasNext()) {
        LOGGER.info("Reload application by incremental changes in application classpath");
      } else if (changes.getFileChanges(getAssetsDirs()).iterator().hasNext()) {
        LOGGER.info("Incremental changes in Assets");
      }
    }
  }
}
