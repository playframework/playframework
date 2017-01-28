/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.async.controllers;

import akka.actor.ActorSystem;
import play.libs.concurrent.CustomExecutionContext;
import scala.concurrent.ExecutionContextExecutor;

//#custom-execution-context
interface MyExecutionContext extends ExecutionContextExecutor {}

// bind MyExecutionContext to MyExecutionContextImpl through DI
public class MyExecutionContextImpl
        extends CustomExecutionContext
        implements MyExecutionContext {

    @javax.inject.Inject
    public MyExecutionContextImpl(ActorSystem actorSystem) {
        // uses a custom thread pool defined in application.conf
        super(actorSystem, "my.dispatcher");
    }
}
//#custom-execution-context
