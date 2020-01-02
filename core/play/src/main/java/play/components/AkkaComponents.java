/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.components;

import akka.actor.ActorSystem;
import akka.actor.CoordinatedShutdown;
import akka.stream.Materializer;
import scala.concurrent.ExecutionContext;

/** Akka and Akka Streams components. */
public interface AkkaComponents {

  ActorSystem actorSystem();

  default Materializer materializer() {
    return Materializer.matFromSystem(actorSystem());
  }

  CoordinatedShutdown coordinatedShutdown();

  default ExecutionContext executionContext() {
    return actorSystem().dispatcher();
  }
}
