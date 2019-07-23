/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.components;

import akka.actor.typed.javadsl.Adapter;
import akka.actor.ActorSystem;
import akka.actor.CoordinatedShutdown;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import scala.concurrent.ExecutionContext;

/** Akka and Akka Streams components. */
public interface AkkaComponents {

  ActorSystem actorSystem();

  default akka.actor.typed.ActorSystem<Void> typedActorSystem() {
    return Adapter.toTyped(actorSystem());
  }

  default Materializer materializer() {
    return ActorMaterializer.create(actorSystem());
  }

  CoordinatedShutdown coordinatedShutdown();

  default ExecutionContext executionContext() {
    return actorSystem().dispatcher();
  }
}
