/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import play.dev.filewatch.FileWatchService;

public class DevServerReloaderTest {

  @Test
  public void shouldInvokeChangeCallbackUnderReloadLock() {
    var listener = new AtomicReference<Runnable>();
    FileWatchService watchService =
        (files, onChange) -> {
          listener.set(onChange);
          return () -> {};
        };
    var reloadLock = new Object();
    var callbackCount = new int[1];

    var reloader =
        new DevServerReloader(
            new File("."),
            getClass().getClassLoader(),
            () -> new CompileResult.CompileSuccess(Map.of(), List.of()),
            Map.of(),
            null,
            List.of(new File(".")),
            watchService,
            Map.of(),
            reloadLock,
            () -> {
              assertTrue(Thread.holdsLock(reloadLock));
              callbackCount[0]++;
            });

    listener.get().run();

    assertEquals(1, callbackCount[0]);
    reloader.close();
  }
}
