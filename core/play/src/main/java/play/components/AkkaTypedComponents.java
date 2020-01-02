/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.components;

import akka.actor.ActorSystem;
import akka.actor.typed.Scheduler;
import play.api.libs.concurrent.AkkaSchedulerProvider;

/** Akka Typed components. */
public interface AkkaTypedComponents {
  ActorSystem actorSystem();

  default Scheduler scheduler() {
    return new AkkaSchedulerProvider(actorSystem()).get();
  }
}
