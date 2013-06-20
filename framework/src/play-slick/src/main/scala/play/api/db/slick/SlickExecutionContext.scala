package play.api.db.slick

import akka.actor.ActorSystem

object SlickExecutionContext {
  val executionContext = ActorSystem("slick-plugin-system").dispatchers.lookup("slick.execution-context")
}