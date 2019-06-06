/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.async.websocket;

import javaguide.async.MyWebSocketActor;

// #content
import play.libs.streams.ActorFlow;
import play.mvc.*;
import akka.actor.*;
import akka.stream.*;
import javax.inject.Inject;

public class HomeController extends Controller {

  private final ActorSystem actorSystem;
  private final Materializer materializer;

  @Inject
  public HomeController(ActorSystem actorSystem, Materializer materializer) {
    this.actorSystem = actorSystem;
    this.materializer = materializer;
  }

  public WebSocket socket() {
    return WebSocket.Text.accept(
        request -> ActorFlow.actorRef(MyWebSocketActor::props, actorSystem, materializer));
  }
}
// #content
