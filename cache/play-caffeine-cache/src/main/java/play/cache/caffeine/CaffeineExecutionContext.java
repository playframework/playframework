/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cache.caffeine;

import org.apache.pekko.actor.ActorSystem;
import play.libs.concurrent.CustomExecutionContext;

public class CaffeineExecutionContext extends CustomExecutionContext {

  public CaffeineExecutionContext(final ActorSystem actorSystem, final String name) {
    super(actorSystem, name);
  }
}
