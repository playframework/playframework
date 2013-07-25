package scalaguide.akka {

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import scala.concurrent.duration._

import akka.actor.{Actor, Props}
import akka.pattern.ask

import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.test._
import java.io.File

@RunWith(classOf[JUnitRunner])
class ScalaAkkaSpec extends PlaySpecification {

  "A scala Akka" should {

    "myActor" in {
      running(FakeApplication()) {
        //#play-akka-myactor
        val myActor = Akka.system.actorOf(Props[MyActor], name = "myactor")
        //#play-akka-myactor

        val future = myActor.ask("Alan")(5 seconds)
        val result = await(future).asInstanceOf[String]
        result must contain("Hello, Alan")
      }
    }

    "actor scheduler" in {
      running(FakeApplication()) {
        val testActor = Akka.system.actorOf(Props[MyActor], name = "testActor")
        import scala.concurrent.ExecutionContext.Implicits.global
        //#play-akka-actor-schedule-repeat
        import play.api.libs.concurrent.Execution.Implicits._
        Akka.system.scheduler.schedule(0.microsecond, 300.microsecond, testActor, "tick")
        //#play-akka-actor-schedule-repeat
        success
      }
    }

    "actor scheduler" in {
      running(FakeApplication()) {
        val testActor = Akka.system.actorOf(Props[MyActor], name = "testActor")
        import scala.concurrent.ExecutionContext.Implicits.global
        val file = new File("/tmp/nofile")
        file.mkdirs()
        //#play-akka-actor-schedule-run-once
        import play.api.libs.concurrent.Execution.Implicits._
        Akka.system.scheduler.scheduleOnce(1000.microsecond) {
          file.delete()
        }
        //#play-akka-actor-schedule-run-once
        Thread.sleep(200)
        file.exists() must beFalse
      }
    }

  }


}

class MyActor extends Actor {
  def receive = {
    case s: String =>
      println(s)
      sender ! "Hello, " + s
  }
}

}
