/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
//###replace: package tasks
package scalaguide.scheduling

import scala.concurrent.duration._

//#custom-task-execution-context
import javax.inject.Inject

import akka.actor.ActorSystem
import play.api.libs.concurrent.CustomExecutionContext

class TasksCustomExecutionContext @Inject()(actorSystem: ActorSystem)
    extends CustomExecutionContext(actorSystem, "tasks-dispatcher")
//#custom-task-execution-context

//#task-using-custom-execution-context
class SomeTask @Inject()(actorSystem: ActorSystem, executor: TasksCustomExecutionContext) {

  actorSystem.scheduler.schedule(initialDelay = 10.seconds, interval = 1.minute)({
    print("Executing something...")
  })(executor) // using the custom execution context

}
//#task-using-custom-execution-context
