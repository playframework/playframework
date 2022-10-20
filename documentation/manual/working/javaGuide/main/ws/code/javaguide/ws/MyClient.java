/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.ws;

// #ws-controller
import javax.inject.Inject;
import play.libs.ws.*;
import play.mvc.*;

public class MyClient implements WSBodyReadables, WSBodyWritables {
  private final WSClient ws;

  @Inject
  public MyClient(WSClient ws) {
    this.ws = ws;
  }
  // ...
}
// #ws-controller
