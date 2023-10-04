/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.pekko.inject;

import javaguide.pekko.ConfiguredActorProtocol;

// #inject
import org.apache.pekko.actor.ActorRef;
import play.mvc.*;
import scala.jdk.javaapi.FutureConverters;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.CompletionStage;

import static org.apache.pekko.pattern.Patterns.ask;

public class Application extends Controller {

  private ActorRef configuredActor;

  @Inject
  public Application(@Named("configured-actor") ActorRef configuredActor) {
    this.configuredActor = configuredActor;
  }

  public CompletionStage<Result> getConfig() {
    return FutureConverters.asJava(
            ask(configuredActor, new ConfiguredActorProtocol.GetConfig(), 1000))
        .thenApply(response -> ok((String) response));
  }
}
// #inject
