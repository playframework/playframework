/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.sql;

import org.apache.pekko.actor.ActorSystem;
import play.libs.concurrent.CustomExecutionContext;

public class DatabaseExecutionContext extends CustomExecutionContext {

  @javax.inject.Inject
  public DatabaseExecutionContext(ActorSystem actorSystem) {
    // uses a custom thread pool defined in application.conf
    super(actorSystem, "database.dispatcher");
  }
}
