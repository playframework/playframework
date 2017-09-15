/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.sql;

import akka.actor.ActorSystem;
import play.libs.concurrent.CustomExecutionContext;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DatabaseExecutionContext extends CustomExecutionContext {

    @Inject
    public DatabaseExecutionContext(ActorSystem actorSystem) {
        // uses a custom thread pool defined in application.conf
        super(actorSystem, "database.dispatcher");
    }
}