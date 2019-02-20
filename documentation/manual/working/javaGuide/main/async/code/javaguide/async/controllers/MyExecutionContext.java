/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.async.controllers;

import akka.actor.ActorSystem;
import play.libs.concurrent.CustomExecutionContext;

import javax.inject.Inject;

// #custom-execution-context
public class MyExecutionContext extends CustomExecutionContext {

  @Inject
  public MyExecutionContext(ActorSystem actorSystem) {
    // uses a custom thread pool defined in application.conf
    super(actorSystem, "my.dispatcher");
  }
}
// #custom-execution-context
