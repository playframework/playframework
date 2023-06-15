/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.components;

import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.typed.Scheduler;
import play.api.libs.concurrent.AkkaSchedulerProvider;

/** Akka Typed components. */
public interface AkkaTypedComponents {
  ActorSystem actorSystem();

  default Scheduler scheduler() {
    return new AkkaSchedulerProvider(actorSystem()).get();
  }
}
