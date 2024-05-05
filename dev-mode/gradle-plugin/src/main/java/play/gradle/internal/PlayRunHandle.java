/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.gradle.internal;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import org.gradle.deployment.internal.Deployment;
import org.gradle.deployment.internal.DeploymentHandle;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.api.PlayException;
import play.runsupport.CompileResult;
import play.runsupport.CompileResult.CompileFailure;
import play.runsupport.CompileResult.CompileSuccess;
import play.runsupport.DevServer;
import play.runsupport.DevServerRunner;
import play.runsupport.classloader.AssetsClassLoader;

public class PlayRunHandle implements DeploymentHandle {

  public static final Logger LOGGER = LoggerFactory.getLogger(PlayRunHandle.class);

  private Deployment deployment;

  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private final PlayRunParams params;

  private DevServer devServer = null;

  @Inject
  public PlayRunHandle(PlayRunParams params) {
    this.params = params;
  }

  @Override
  public boolean isRunning() {
    lock.readLock().lock();
    try {
      return devServer != null;
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Force reload Play application before next request. */
  public void forceReload() {
    lock.writeLock().lock();
    try {
      devServer.buildLink().forceReload();
    } finally {
      lock.writeLock().unlock();
    }
  }

  private boolean isChanged() {
    lock.readLock().lock();
    try {
      return deployment.status().hasChanged();
    } finally {
      lock.readLock().unlock();
    }
  }

  private CompileResult reloadCompile() {
    lock.readLock().lock();
    try {
      Throwable failure = deployment.status().getFailure();
      if (failure != null) {
        return new CompileFailure(
            new PlayException("Gradle Build Failure", failure.getMessage(), failure));
      }
      return new CompileSuccess(Map.of(), params.getApplicationClasspath());
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      return new CompileFailure(new PlayException("Gradle Build Failure", e.getMessage(), e));
    } finally {
      lock.readLock().unlock();
    }
  }

  private ClassLoader createAssetClassLoader(ClassLoader parent) {
    return new AssetsClassLoader(
        parent,
        params.getAssetsDirectories().stream()
            .map(file -> Map.entry(params.getAssetsPrefix() + "/", file))
            .collect(Collectors.toList()));
  }

  @Override
  public void start(@NotNull Deployment deployment) {
    lock.writeLock().lock();
    this.deployment = deployment;
    try {
      devServer =
          DevServerRunner.startDevMode(
              List.of(),
              List.of(),
              ClassLoader.getPlatformClassLoader(),
              params.getDependencyClasspath(),
              this::reloadCompile,
              this::createAssetClassLoader,
              this::isChanged,
              List.of(),
              null,
              Map.of(),
              params.getHttpPort(),
              "0.0.0.0",
              params.getProjectPath(),
              params.getDevSettings(),
              List.of(),
              "play.core.server.DevServerStart",
              lock);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void stop() {
    lock.writeLock().lock();
    LOGGER.info("PlayApplication is stopping ...");
    try {
      devServer.close();
      LOGGER.info("PlayApplication stopped.");
    } catch (Exception e) {
      LOGGER.error("PlayApplication stopped with exception", e);
    } finally {
      devServer = null;
      deployment = null;
      lock.writeLock().unlock();
    }
  }
}
