/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.cache.caffeine;

import akka.actor.ActorSystem;
import play.libs.concurrent.CustomExecutionContext;

public class CaffeineExecutionContext extends CustomExecutionContext {

  public CaffeineExecutionContext(final ActorSystem actorSystem, final String name) {
    super(actorSystem, name);
  }
}
