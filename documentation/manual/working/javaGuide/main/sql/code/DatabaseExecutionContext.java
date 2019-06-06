/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.sql;

import akka.actor.ActorSystem;
import play.libs.concurrent.CustomExecutionContext;

public class DatabaseExecutionContext extends CustomExecutionContext {

  @javax.inject.Inject
  public DatabaseExecutionContext(ActorSystem actorSystem) {
    // uses a custom thread pool defined in application.conf
    super(actorSystem, "database.dispatcher");
  }
}
