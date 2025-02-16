/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.advanced.extending;

//#components-decl
import javax.inject.Inject;
import play.inject.ApplicationLifecycle;
import java.util.concurrent.*;
import play.libs.concurrent.Futures;

public interface MyComponent {}

class MyComponentImpl implements MyComponent {
  @Inject
  public MyComponentImpl(ApplicationLifecycle lifecycle) {
    // previous contents of Plugin.onStart
    lifecycle.addStopHook( () -> {
      // previous contents of Plugin.onStop
      return CompletableFuture.completedFuture(null);
    });
  }
}
//#components-decl
