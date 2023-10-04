/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.scheduling

import javax.inject.Inject

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import org.apache.pekko.actor.ActorSystem

//#schedule-block-with-interval
class CodeBlockTask @Inject() (actorSystem: ActorSystem)(implicit executionContext: ExecutionContext) {
  actorSystem.scheduler.scheduleAtFixedRate(initialDelay = 10.seconds, interval = 1.minute) { () =>
    // the block of code that will be executed
    actorSystem.log.info("Executing something...")
  }
}
//#schedule-block-with-interval

//#schedule-block-once
class ScheduleOnceTask @Inject() (actorSystem: ActorSystem)(implicit executionContext: ExecutionContext) {
  actorSystem.scheduler.scheduleOnce(delay = 10.seconds) { () =>
    // the block of code that will be executed
    actorSystem.log.info("Executing something...")
  }
}
//#schedule-block-once
