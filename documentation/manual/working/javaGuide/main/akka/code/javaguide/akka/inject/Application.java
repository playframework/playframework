/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.akka.inject;

import javaguide.akka.ConfiguredActorProtocol;

// #inject
import akka.actor.ActorRef;
import play.mvc.*;
import scala.compat.java8.FutureConverters;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.CompletionStage;

import static akka.pattern.Patterns.ask;

public class Application extends Controller {

  private ActorRef configuredActor;

  @Inject
  public Application(@Named("configured-actor") ActorRef configuredActor) {
    this.configuredActor = configuredActor;
  }

  public CompletionStage<Result> getConfig() {
    return FutureConverters.toJava(
            ask(configuredActor, new ConfiguredActorProtocol.GetConfig(), 1000))
        .thenApply(response -> ok((String) response));
  }
}
// #inject
