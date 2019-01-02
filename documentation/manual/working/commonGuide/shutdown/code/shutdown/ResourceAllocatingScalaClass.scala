/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package shutdown

import java.util.concurrent.CompletionStage
import javax.inject.Inject
import akka.actor.CoordinatedShutdown
import scala.concurrent.Future
import akka.Done


package scalaguide {
//#shutdown-task
class ResourceAllocatingScalaClass @Inject() (cs: CoordinatedShutdown) {

  // Some resource allocation happens here: A connection
  // pool is created, some client library is started, ...
  val resources = Resources.allocate()

  // Register a shutdown task as soon as possible.
  cs.addTask(
    CoordinatedShutdown.PhaseServiceUnbind,
    "free-some-resource"){ () =>
    resources.release()
  }

  // ... some more code
}
//#shutdown-task
  
  class Resources(name: String) {
    def release(): Future[Done] = ???
  }
  object Resources {
    def allocate(): Resources = ???
  }
}

class Resources(name: String) {
  def release(): CompletionStage[Done] = ???
}

object Resources {
  def allocate(): Resources = ???
}
