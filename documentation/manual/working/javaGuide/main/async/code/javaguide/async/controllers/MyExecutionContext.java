/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.async.controllers;

import akka.actor.ActorSystem;
import javax.inject.Inject;
import play.libs.concurrent.CustomExecutionContext;

// #custom-execution-context
public class MyExecutionContext extends CustomExecutionContext {

  @Inject
  public MyExecutionContext(ActorSystem actorSystem) {
    // uses a custom thread pool defined in application.conf
    super(actorSystem, "my.dispatcher");
  }
}
// #custom-execution-context
