/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

// ###replace: package tasks
package scalaguide.scheduling

// format: off
import scala.concurrent.duration._
// format: on

//#custom-task-execution-context
import javax.inject.Inject

import akka.actor.ActorSystem
import play.api.libs.concurrent.CustomExecutionContext

class TasksCustomExecutionContext @Inject() (actorSystem: ActorSystem)
    extends CustomExecutionContext(actorSystem, "tasks-dispatcher")
//#custom-task-execution-context

//#task-using-custom-execution-context
class SomeTask @Inject() (actorSystem: ActorSystem, executor: TasksCustomExecutionContext) {
  actorSystem.scheduler.scheduleAtFixedRate(initialDelay = 10.seconds, interval = 1.minute) { () =>
    actorSystem.log.info("Executing something...")
  }(executor) // using the custom execution context
}
//#task-using-custom-execution-context
