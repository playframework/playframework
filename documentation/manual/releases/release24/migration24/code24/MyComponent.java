/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

//#components-decl
import javax.inject.Inject;
import play.inject.ApplicationLifecycle;
import play.libs.F;

public interface MyComponent {}

class MyComponentImpl implements MyComponent {
  @Inject
  public MyComponentImpl(ApplicationLifecycle lifecycle) {
    // previous contents of Plugin.onStart
    lifecycle.addStopHook( () -> {
      // previous contents of Plugin.onStop
      return F.Promise.pure(null);
    });
  }
}
//#components-decl
