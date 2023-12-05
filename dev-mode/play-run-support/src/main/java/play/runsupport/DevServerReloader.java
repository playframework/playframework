/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport;

import static java.util.stream.Collectors.joining;
import static play.runsupport.DevServerRunner.urls;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;
import play.core.BuildLink;
import play.dev.filewatch.FileWatchService;
import play.dev.filewatch.FileWatcher;
import play.runsupport.CompileResult.CompileFailure;
import play.runsupport.CompileResult.CompileSuccess;
import play.runsupport.classloader.DelegatedResourcesClassLoader;

class DevServerReloader implements BuildLink, Closeable {

  private final File projectPath;

  private static final AccessControlContext accessControlContext = AccessController.getContext();

  private final Object reloadLock;

  private final Supplier<CompileResult> compile;

  private final ClassLoader baseClassLoader;

  private final Map<String, String> settings;

  private final Map<String, ? extends GeneratedSourceMapping> generatedSourceHandlers;

  // The current classloader for the application
  private volatile URLClassLoader currentApplicationClassLoader;

  // Flag to force a reload on the next request.
  // This is set if a compile error occurs, and also by the forceReload method on BuildLink, which
  // is called for example when evolutions have been applied.
  private volatile boolean forceReloadNextTime = false;

  // Whether any source files have changed since the last request.
  private volatile boolean changed = false;

  // The last successful compile results. Used for rendering nice errors.
  private volatile Map<String, Source> currentSourceMap;

  // Last time the classpath was modified in millis. Used to determine whether anything on the
  // classpath has changed as a result of compilation, and therefore a new classloader is needed
  // and the app needs to be reloaded.
  private volatile long lastModified = 0L;

  private final FileWatcher watcher;

  private final AtomicInteger classLoaderVersion = new AtomicInteger(0);

  DevServerReloader(
      File projectPath,
      ClassLoader baseClassLoader,
      Supplier<CompileResult> compile,
      Map<String, String> settings,
      List<File> monitoredFiles,
      FileWatchService fileWatchService,
      Map<String, ? extends GeneratedSourceMapping> generatedSourceHandlers,
      Object reloadLock) {
    this.projectPath = projectPath;
    this.baseClassLoader = baseClassLoader;
    this.compile = compile;
    this.settings = settings;
    this.generatedSourceHandlers = generatedSourceHandlers;
    // Create the watcher, updates the changed boolean when a file has changed:
    this.watcher = fileWatchService.watch(monitoredFiles, () -> changed = true);
    this.reloadLock = reloadLock;
  }

  /** Execute f with context ClassLoader of Reloader */
  private static <T> T withReloaderContextClassLoader(Supplier<T> f) {
    var thread = Thread.currentThread();
    var oldLoader = thread.getContextClassLoader();
    // we use accessControlContext & AccessController to avoid a ClassLoader leak
    // (ProtectionDomain class)
    return AccessController.doPrivileged(
        (PrivilegedAction<T>)
            () -> {
              try {
                thread.setContextClassLoader(DevServerReloader.class.getClassLoader());
                return f.get();
              } finally {
                thread.setContextClassLoader(oldLoader);
              }
            },
        accessControlContext);
  }

  @Override
  public File projectPath() {
    return projectPath;
  }

  private Object reload(boolean shouldReload) {
    // Run the reload task, which will trigger everything to compile
    CompileResult compileResult = compile.get();
    if (compileResult instanceof CompileFailure) {
      var result = (CompileFailure) compileResult;
      // We force reload next time because compilation failed this time
      forceReloadNextTime = true;
      return result.getException();
    } else if (compileResult instanceof CompileSuccess) {
      var result = (CompileSuccess) compileResult;
      var cp = result.getClasspath();
      currentSourceMap = result.getSources();

      // We only want to reload if the classpath has changed.
      // Assets don't live on the classpath, so they won't trigger a reload.
      long newLastModified =
          cp.stream()
              .filter(File::exists)
              .flatMap(DevServerReloader::listRecursively)
              .mapToLong(File::lastModified)
              .max()
              .orElse(0L);
      var triggered = newLastModified > lastModified;
      lastModified = newLastModified;

      if (triggered || shouldReload || currentApplicationClassLoader == null) {
        // Create a new classloader
        currentApplicationClassLoader =
            new DelegatedResourcesClassLoader(
                "ReloadableClassLoader(v" + classLoaderVersion.incrementAndGet() + ")",
                urls(cp),
                baseClassLoader);
        return currentApplicationClassLoader;
      }
      return null; // null means nothing changed
    } else {
      return null; // null means nothing changed
    }
  }

  /**
   * Contrary to its name, this doesn't necessarily reload the app. It is invoked on every request,
   * and will only trigger a reload of the app if something has changed.
   *
   * <p>Since this communicates across classloaders, it must return only simple objects.
   *
   * @return Either<br>
   *     - {@link Throwable} - If something went wrong (eg, a compile error). <br>
   *     - {@link ClassLoader} - If the classloader has changed, and the application should be
   *     reloaded.<br>
   *     - {@code null} - If nothing changed.
   */
  @Override
  public Object reload() {
    synchronized (reloadLock) {
      if (changed
          || forceReloadNextTime
          || currentSourceMap == null
          || currentApplicationClassLoader == null) {
        var shouldReload = forceReloadNextTime;
        changed = false;
        forceReloadNextTime = false;
        // use Reloader context ClassLoader to avoid ClassLoader leaks in sbt/scala-compiler threads
        return withReloaderContextClassLoader(() -> reload(shouldReload));
      } else {
        return null; // null means nothing changed
      }
    }
  }

  private static Stream<File> listRecursively(File file) {
    Stream<Path> result;
    try {
      result = Files.walk(file.toPath()).filter(path -> !path.equals(file.toPath()));
    } catch (IOException e) {
      result = Stream.empty();
    }
    return result.map(Path::toFile);
  }

  @Override
  public Map<String, String> settings() {
    return settings;
  }

  @Override
  public void forceReload() {
    forceReloadNextTime = true;
  }

  @Override
  public Object[] findSource(String className, Integer line) {
    var topType = className.split("\\$")[0];
    if (currentSourceMap == null || !currentSourceMap.containsKey(topType)) return null;
    var source = currentSourceMap.get(topType);
    if (source.getOriginal() == null) {
      return new Object[] {source.getFile(), line};
    } else if (line != null) {
      var origFile = source.getOriginal();
      var key = Arrays.stream(origFile.getName().split("\\.")).skip(1).collect(joining("."));
      if (generatedSourceHandlers.containsKey(key)) {
        return new Object[] {
          origFile, generatedSourceHandlers.get(key).getOriginalLine(source.getFile(), line)
        };
      } else {
        return new Object[] {origFile, line};
      }
    }
    return new Object[] {source.getOriginal(), null};
  }

  public void close() {
    currentApplicationClassLoader = null;
    currentSourceMap = null;
    watcher.stop();
  }

  public URLClassLoader getClassLoader() {
    return currentApplicationClassLoader;
  }
}
