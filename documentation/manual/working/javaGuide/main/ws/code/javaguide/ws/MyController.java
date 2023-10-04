/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.ws;

// #ws-streams-controller
import javax.inject.Inject;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.stream.javadsl.*;
import play.libs.ws.*;
import play.mvc.*;

public class MyController extends Controller {

  @Inject WSClient ws;
  @Inject Materializer materializer;

  // ...
}
// #ws-streams-controller
