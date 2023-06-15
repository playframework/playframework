/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

// ###replace: package tasks
package scalaguide.scheduling

import javax.inject.Inject
import javax.inject.Named

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import org.apache.pekko.actor.ActorRef
import org.apache.pekko.actor.ActorSystem

class MyActorTask @Inject() (actorSystem: ActorSystem, @Named("some-actor") someActor: ActorRef)(
    implicit executionContext: ExecutionContext
) {
  actorSystem.scheduler.scheduleAtFixedRate(
    initialDelay = 0.microseconds,
    interval = 30.seconds,
    receiver = someActor,
    message = "tick"
  )
}
