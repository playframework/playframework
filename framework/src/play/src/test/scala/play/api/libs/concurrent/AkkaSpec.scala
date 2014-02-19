/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.concurrent

import org.specs2.mutable._
import play.api.libs._
import play.api.FakeApplication
import akka.actor.{Props, Actor}
import java.util.Date

object AkkaSpec extends Specification {
  
  // returns the time it took to stop the Akka plugin in milliseconds
  def testBlockingActor(app: play.api.Application): Long = {
    val plugin = new AkkaPlugin(app)

    val blockingActor = plugin.applicationSystem.actorOf(Props[BlockingActor])

    blockingActor ! Block

    val startTime = System.nanoTime()

    plugin.onStop()

    val endTime = System.nanoTime()

    (endTime - startTime) / 1000 / 1000
  }

  "AkkaPlugin with a blocking Actor" should {
    "by default wait until the blocking actor is terminated" in {
      val totalTime = testBlockingActor(FakeApplication())
      
      // the actor blocks shutdown for 10 seconds
      totalTime must be greaterThanOrEqualTo 10000
    }
    "be able to be restarted when the Actor is blocking if the config is provided" in {
      val app = FakeApplication(Map("play.akka.shutdown-timeout" -> "500ms"))
      val totalTime = testBlockingActor(app)

      // the actor blocks for 10 seconds but we should be back here in around 500ms
      totalTime must be between (500, 10000)
    }
  }

}

class BlockingActor extends Actor {
  
  def receive = {
    case Block =>
      context.system.log.info("BlockingActor is now blocking for 10 seconds")
      Thread.sleep(10000)
      context.system.log.info("BlockingActor is done blocking")
  }
  
}

case object Block