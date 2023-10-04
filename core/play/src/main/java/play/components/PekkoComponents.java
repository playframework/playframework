/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.components;

import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.CoordinatedShutdown;
import org.apache.pekko.stream.Materializer;
import scala.concurrent.ExecutionContext;

/** Pekko and Pekko Streams components. */
public interface PekkoComponents {

  ActorSystem actorSystem();

  default Materializer materializer() {
    return Materializer.matFromSystem(actorSystem());
  }

  CoordinatedShutdown coordinatedShutdown();

  default ExecutionContext executionContext() {
    return actorSystem().dispatcher();
  }
}
