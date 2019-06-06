/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
//###replace: package tasks
package scalaguide.scheduling

import javax.inject.Inject
import javax.inject.Named

import akka.actor.ActorRef
import akka.actor.ActorSystem

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class MyActorTask @Inject()(actorSystem: ActorSystem, @Named("some-actor") someActor: ActorRef)(
    implicit executionContext: ExecutionContext
) {

  actorSystem.scheduler.schedule(
    initialDelay = 0.microseconds,
    interval = 30.seconds,
    receiver = someActor,
    message = "tick"
  )

}
