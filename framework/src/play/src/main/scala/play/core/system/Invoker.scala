package play.core

import akka.actor._
import akka.actor.Actor._

import com.typesafe.config._

/**
 * provides Play's internal actor system and the corresponding actor instances
 */
object Invoker {

  val system: ActorSystem = ActorSystem("play") //TODO make sure this is configurable

  val executionContext: scala.concurrent.ExecutionContext = system.dispatcher

}
