/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import play.core.Build;
import play.core.BuildLink;
import play.core.server.ReloadableServer;
import play.dev.filewatch.FileWatchService;
import play.runsupport.classloader.DelegatingClassLoader;
import play.runsupport.classloader.NamedURLClassLoader;

public final class DevServerRunner {

  private DevServerReloader reloader;

  private ReloadableServer getReloadableServer(
      ClassLoader applicationLoader, String mainClassName, DevServerSettings settings)
      throws ReflectiveOperationException {
    var httpPort = settings.getHttpPort();
    var httpsPort = settings.getHttpsPort();
    var httpAddress = settings.getHttpAddress();

    ReloadableServer server;

    var mainClass = applicationLoader.loadClass(mainClassName);

    if (settings.isHttpPortDefined() && settings.isHttpsPortDefined()) {
      var m =
          mainClass.getMethod(
              "mainDevHttpAndHttpsMode", BuildLink.class, int.class, int.class, String.class);
      server = (ReloadableServer) m.invoke(null, reloader, httpPort, httpsPort, httpAddress);
    } else if (settings.isHttpPortDefined()) {
      var m = mainClass.getMethod("mainDevHttpMode", BuildLink.class, int.class, String.class);
      server = (ReloadableServer) m.invoke(null, reloader, httpPort, httpAddress);
    } else {
      var m = mainClass.getMethod("mainDevOnlyHttpsMode", BuildLink.class, int.class, String.class);
      server = (ReloadableServer) m.invoke(null, reloader, httpsPort, httpAddress);
    }
    return server;
  }

  private DevServer run(
      List<RunHook> runHooks,
      List<String> javaOptions,
      ClassLoader commonClassLoader,
      List<File> dependencyClasspath,
      Supplier<CompileResult> reloadCompile,
      Function<ClassLoader, ClassLoader> assetsClassLoader,
      List<File> monitoredFiles,
      FileWatchService fileWatchService,
      Map<String, GeneratedSourceMapping> generatedSourceHandlers,
      int defaultHttpPort,
      String defaultHttpAddress,
      File projectPath,
      Map<String, String> devSettings,
      List<String> args,
      String mainClassName,
      Object reloadLock) {
    var settings =
        DevServerSettings.parse(
            javaOptions, args, devSettings, defaultHttpPort, defaultHttpAddress);
    if (!settings.isAnyPortDefined()) {
      throw new IllegalArgumentException(
          "You have to specify https.port when http.port is disabled");
    }
    // Set Java properties
    settings.getSystemProperties().forEach(System::setProperty);

    System.out.println();

    /*
     * We need to do a bit of classloader magic to run the application.
     *
     * There are six classloaders:
     *
     * 1. buildLoader, the classloader of sbt and the sbt plugin.
     * 2. commonLoader, a classloader that persists across calls to run.
     *    This classloader is stored inside the
     *    PlayInternalKeys.playCommonClassloader task. This classloader will
     *    load the classes for the H2 database if it finds them in the user's
     *    classpath. This allows H2's in-memory database state to survive across
     *    calls to run.
     * 3. delegatingLoader, a special classloader that overrides class loading
     *    to delegate shared classes for build link to the buildLoader, and accesses
     *    the reloader.currentApplicationClassLoader for resource loading to
     *    make user resources available to dependency classes.
     *    Has the commonLoader as its parent.
     * 4. applicationLoader, contains the application dependencies. Has the
     *    delegatingLoader as its parent. Classes from the commonLoader and
     *    the delegatingLoader are checked for loading first.
     * 5. playAssetsClassLoader, serves assets from all projects, prefixed as
     *    configured.  It does no caching, and doesn't need to be reloaded each
     *    time the assets are rebuilt.
     * 6. reloader.currentApplicationClassLoader, contains the user classes
     *    and resources. Has applicationLoader as its parent, where the
     *    application dependencies are found, and which will delegate through
     *    to the buildLoader via the delegatingLoader for the shared link.
     *    Resources are actually loaded by the delegatingLoader, where they
     *    are available to both the reloader and the applicationLoader.
     *    This classloader is recreated on reload. See PlayReloader.
     *
     * Someone working on this code in the future might want to tidy things up
     * by splitting some of the custom logic out of the URLClassLoaders and into
     * their own simpler ClassLoader implementations. The curious cycle between
     * applicationLoader and reloader.currentApplicationClassLoader could also
     * use some attention.
     */

    var buildLoader = this.getClass().getClassLoader();

    /*
     * ClassLoader that delegates loading of shared build link classes to the buildLoader. Also
     * accesses the reloader resources to make these available to the applicationLoader, creating a
     * full circle for resource loading.
     */
    ClassLoader delegatingLoader =
        new DelegatingClassLoader(
            commonClassLoader, Build.sharedClasses, buildLoader, () -> reloader.getClassLoader());

    var applicationLoader =
        new NamedURLClassLoader(
            "DependencyClassLoader", urls(dependencyClasspath), delegatingLoader);

    var assetsLoader = assetsClassLoader.apply(applicationLoader);

    reloader =
        new DevServerReloader(
            projectPath,
            assetsLoader,
            reloadCompile,
            devSettings,
            monitoredFiles,
            fileWatchService,
            generatedSourceHandlers,
            reloadLock);

    try {
      // Now we're about to start, let's call the hooks:
      RunHooksRunner.run(runHooks, RunHook::beforeStarted);

      ReloadableServer server = getReloadableServer(applicationLoader, mainClassName, settings);

      // Notify hooks
      RunHooksRunner.run(runHooks, RunHook::afterStarted);

      return new DevServer() {
        public BuildLink buildLink() {
          return reloader;
        }

        public void reload() {
          server.reload();
        }

        public void close() {
          server.stop();
          reloader.close();

          // Notify hooks
          RunHooksRunner.run(runHooks, RunHook::afterStopped);

          // Remove Java properties
          settings.getSystemProperties().forEach((key, __) -> System.clearProperty(key));
        }
      };
    } catch (Throwable e) {
      // Let hooks clean up
      runHooks.forEach(
          hook -> {
            try {
              hook.onError();
            } catch (Throwable ignore) {
              // Swallow any exceptions so that all `onError`s get called.
            }
          });
      Throwable rootCause = e;
      while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
        rootCause = rootCause.getCause();
      }
      // Convert play-server exceptions to our ServerStartException
      if (rootCause.getClass().getName().equals("play.core.server.ServerListenException")) {
        throw new ServerStartException(e);
      }
      throw new RuntimeException(e);
    }
  }

  public static URL[] urls(List<File> files) {
    return files.stream()
        .map(
            __ -> {
              try {
                return __.toURI().toURL();
              } catch (MalformedURLException e) {
                throw new RuntimeException(e);
              }
            })
        .toArray(URL[]::new);
  }

  /**
   * Start the server in DEV-mode
   *
   * @return A closeable that can be closed to stop the server
   */
  public static DevServer startDevMode(
      List<RunHook> runHooks,
      List<String> javaOptions,
      ClassLoader commonClassLoader,
      List<File> dependencyClasspath,
      Supplier<CompileResult> reloadCompile,
      Function<ClassLoader, ClassLoader> assetsClassLoader,
      List<File> monitoredFiles,
      FileWatchService fileWatchService,
      Map<String, GeneratedSourceMapping> generatedSourceHandlers,
      int defaultHttpPort,
      String defaultHttpAddress,
      File projectPath,
      Map<String, String> devSettings,
      List<String> args,
      String mainClassName,
      Object reloadLock) {
    return getInstance()
        .run(
            runHooks,
            javaOptions,
            commonClassLoader,
            dependencyClasspath,
            reloadCompile,
            assetsClassLoader,
            monitoredFiles,
            fileWatchService,
            generatedSourceHandlers,
            defaultHttpPort,
            defaultHttpAddress,
            projectPath,
            devSettings,
            args,
            mainClassName,
            reloadLock);
  }

  private DevServerRunner() {}

  private static class Holder {
    public static final DevServerRunner INSTANCE = new DevServerRunner();
  }

  public static DevServerRunner getInstance() {
    return Holder.INSTANCE;
  }
}
