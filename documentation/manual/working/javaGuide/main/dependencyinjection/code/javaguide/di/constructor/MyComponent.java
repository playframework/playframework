/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.di.constructor;

// #constructor
import javax.inject.*;
import play.libs.ws.*;

public class MyComponent {
  private final WSClient ws;

  @Inject
  public MyComponent(WSClient ws) {
    this.ws = ws;
  }

  // ...
}
// #constructor
