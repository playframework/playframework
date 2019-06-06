/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
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
