/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.di.guice.eager;

import javaguide.di.*;

// #eager-guice-module
import jakarta.inject.*;
import play.inject.ApplicationLifecycle;
import play.Environment;
import java.util.concurrent.CompletableFuture;

// This creates an `ApplicationStart` object once at start-up.
@Singleton
public class ApplicationStart {

  // Inject the application's Environment upon start-up and register hook(s) for shut-down.
  @Inject
  public ApplicationStart(ApplicationLifecycle lifecycle, Environment environment) {
    // Shut-down hook
    lifecycle.addStopHook(
        () -> {
          return CompletableFuture.completedFuture(null);
        });
    // ...
  }
}
// #eager-guice-module
