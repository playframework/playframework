/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.scheduling

import javax.inject.Inject

import akka.actor.ActorSystem

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

//#schedule-block-with-interval
class CodeBlockTask @Inject()(actorSystem: ActorSystem)(implicit executionContext: ExecutionContext) {

  actorSystem.scheduler.schedule(initialDelay = 10.seconds, interval = 1.minute) {
    // the block of code that will be executed
    print("Executing something...")
  }
}
//#schedule-block-with-interval

//#schedule-block-once
class ScheduleOnceTask @Inject()(actorSystem: ActorSystem)(implicit executionContext: ExecutionContext) {

  actorSystem.scheduler.scheduleOnce(delay = 10.seconds) {
    // the block of code that will be executed
    print("Executing something...")
  }

}
//#schedule-block-once
