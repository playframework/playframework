/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.Test;
import play.api.PlayException;
import play.dev.filewatch.FileWatchService;

public class DevServerReloaderTest {
  private final AtomicReference<Runnable> onChange = new AtomicReference<>();
  private final Object reloadLock = new Object();

  private DevServerReloader reloader(Supplier<CompileResult> compile) {
    FileWatchService watchService =
        (files, listener) -> {
          onChange.set(listener);
          return () -> {};
        };
    return new DevServerReloader(
        new File("."),
        getClass().getClassLoader(),
        compile,
        Map.of(),
        null,
        List.of(new File(".")),
        watchService,
        Map.of(),
        reloadLock);
  }

  private CompileResult success() {
    return new CompileResult.CompileSuccess(Map.of(), List.of());
  }

  @Test
  public void shouldCompileWithoutLoadingAndReuseResultOnRequest() {
    var compilations = new AtomicInteger();
    try (var reloader =
        reloader(
            () -> {
              assertTrue(Thread.holdsLock(reloadLock));
              assertSame(
                  DevServerReloader.class.getClassLoader(),
                  Thread.currentThread().getContextClassLoader());
              compilations.incrementAndGet();
              return success();
            })) {
      var thread = Thread.currentThread();
      var originalLoader = thread.getContextClassLoader();
      var callerLoader = new ClassLoader(originalLoader) {};
      try {
        thread.setContextClassLoader(callerLoader);
        reloader.compile();
        assertSame(callerLoader, thread.getContextClassLoader());
      } finally {
        thread.setContextClassLoader(originalLoader);
      }
      assertEquals(1, compilations.get());
      assertNull(reloader.getClassLoader());

      assertTrue(reloader.reload() instanceof ClassLoader);
      assertEquals(1, compilations.get());
      assertNull(reloader.reload());
      assertEquals(1, compilations.get());
    }
  }

  @Test
  public void shouldNotReloadWhenEagerCompilationDoesNotChangeTheClasspath() {
    var compilations = new AtomicInteger();
    try (var reloader =
        reloader(
            () -> {
              compilations.incrementAndGet();
              return success();
            })) {
      var loader = reloader.reload();
      reloader.compile();
      assertNull(reloader.reload());
      assertSame(loader, reloader.getClassLoader());
      assertEquals(2, compilations.get());
    }
  }

  @Test
  public void shouldDiscardEagerResultIfAnotherFileChanges() {
    var compilations = new AtomicInteger();
    try (var reloader =
        reloader(
            () -> {
              compilations.incrementAndGet();
              return success();
            })) {
      reloader.compile();
      onChange.get().run();
      assertTrue(reloader.reload() instanceof ClassLoader);
      assertEquals(2, compilations.get());
    }
  }

  @Test
  public void shouldCompileAgainOnRequestIfEagerCompilationThrows() {
    var compilations = new AtomicInteger();
    try (var reloader =
        reloader(
            () -> {
              if (compilations.incrementAndGet() == 2)
                throw new IllegalStateException("Compilation was cancelled");
              return success();
            })) {
      assertTrue(reloader.reload() instanceof ClassLoader);
      assertThrows(IllegalStateException.class, reloader::compile);
      reloader.reload();
      assertEquals(3, compilations.get());
    }
  }

  @Test
  public void shouldCompileAgainOnRequestIfEagerCompilationThrowsACheckedException() {
    var compilations = new AtomicInteger();
    try (var reloader =
        reloader(
            () -> {
              if (compilations.incrementAndGet() == 2)
                throw DevServerReloaderTest.<RuntimeException>sneakyThrow(
                    new InterruptedException("Compilation was interrupted"));
              return success();
            })) {
      assertTrue(reloader.reload() instanceof ClassLoader);
      assertThrows(InterruptedException.class, reloader::compile);
      reloader.reload();
      assertEquals(3, compilations.get());
    }
  }

  /** Throws a checked exception without declaring it, as Scala code can. */
  @SuppressWarnings("unchecked")
  private static <T extends Throwable> RuntimeException sneakyThrow(Throwable t) throws T {
    throw (T) t;
  }

  @Test
  public void shouldNoticeChangesDuringEagerCompilation() {
    var compilations = new AtomicInteger();
    try (var reloader =
        reloader(
            () -> {
              if (compilations.incrementAndGet() == 1) onChange.get().run();
              return success();
            })) {
      reloader.compile();
      assertTrue(reloader.reload() instanceof ClassLoader);
      assertEquals(2, compilations.get());
    }
  }

  @Test
  public void shouldRenderEagerCompilationFailureAndRecover() {
    var error = new PlayException("Compilation failed", "broken source");
    var result = new AtomicReference<CompileResult>(new CompileResult.CompileFailure(error));
    var compilations = new AtomicInteger();
    try (var reloader =
        reloader(
            () -> {
              compilations.incrementAndGet();
              return result.get();
            })) {
      reloader.compile();
      assertSame(error, reloader.reload());
      assertEquals(1, compilations.get());
      assertNull(reloader.getClassLoader());

      result.set(success());
      reloader.compile();
      assertTrue(reloader.reload() instanceof ClassLoader);
      assertEquals(2, compilations.get());
    }
  }

  @Test
  public void shouldKeepTheLoadedApplicationOnFailureAndRetryOnTheNextRequest() {
    var error = new PlayException("Compilation failed", "broken source");
    var result = new AtomicReference<CompileResult>(success());
    var compilations = new AtomicInteger();
    try (var reloader =
        reloader(
            () -> {
              compilations.incrementAndGet();
              return result.get();
            })) {
      var loader = reloader.reload();
      result.set(new CompileResult.CompileFailure(error));
      reloader.compile();
      assertSame(error, reloader.reload());
      assertSame(loader, reloader.getClassLoader());

      result.set(success());
      assertTrue(reloader.reload() instanceof ClassLoader);
      assertNotSame(loader, reloader.getClassLoader());
      assertEquals(3, compilations.get());
    }
  }
}
