package play.core

import akka.actor._
import akka.actor.Actor._

import com.typesafe.config._

/**
 * provides Play's internal actor system and the corresponding actor instances
 */
private[play] object Invoker {

  val system: ActorSystem = ActorSystem("play") 

  val executionContext: scala.concurrent.ExecutionContext = system.dispatcher

}
