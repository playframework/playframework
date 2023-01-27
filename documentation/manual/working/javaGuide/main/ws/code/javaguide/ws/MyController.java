/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.ws;

// #ws-streams-controller
import akka.stream.Materializer;
import akka.stream.javadsl.*;
import javax.inject.Inject;
import play.libs.ws.*;
import play.mvc.*;

public class MyController extends Controller {

  @Inject WSClient ws;
  @Inject Materializer materializer;

  // ...
}
// #ws-streams-controller
