package play.libs

import java.util.concurrent.Callable
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.{ MILLISECONDS, SECONDS }
import org.specs2.mutable._
import play.api.libs.iteratee.ExecutionSpecification
import scala.concurrent.{ Future, Promise }

object AkkaSpec extends Specification
  with ExecutionSpecification {

  "Akka.asPromise" should {

    "create an F.Promise from a Future" in {
      Akka.asPromise(Future.successful(2)).get(5, SECONDS) must equalTo(2)
    }
  }

  "Akka.future" should {

    "create an F.Promise" in {
      Akka.future(new Callable[Int] {
        def call() = 2
      }).get(5, SECONDS) must equalTo(2)
    }
  }

  "Akka.timeout" should {

    "create an F.Promise after a timeout" in {
      Akka.timeout(new Callable[Int] {
        def call() = 2
      }, 1, MILLISECONDS).get(5, SECONDS) must equalTo(2)
    }

  }

}