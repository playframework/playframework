/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.async.scalaasync

import javax.inject.Inject

import scala.concurrent._
import akka.actor._
import play.api._
import play.api.mvc._
import play.api.test._

object ScalaAsyncSpec extends PlaySpecification {

  "scala async" should {
    "allow returning a future" in new WithApplication() {
      contentAsString(ScalaAsyncSamples.futureResult) must startWith("PI value computed: 3.14")
    }

    "allow dispatching an intensive computation" in new WithApplication() {
      await(ScalaAsyncSamples.intensiveComp) must_== 10
    }

    "allow returning an async result" in new WithApplication() {
      contentAsString(ScalaAsyncSamples.asyncResult()(FakeRequest())) must_== "Got result: 10"
    }

    "allow timing out a future" in new WithApplication() {
      status(ScalaAsyncSamples.timeout(1200)(FakeRequest())) must_== INTERNAL_SERVER_ERROR
      status(ScalaAsyncSamples.timeout(10)(FakeRequest())) must_== OK
    }
  }
}

// If we want to show examples of importing the Play defaultContext, it can't be in a spec, since
// Specification already defines a field called defaultContext, and this interferes with the implicits
object ScalaAsyncSamples extends Controller {

  def futureResult = {
    def computePIAsynchronously() = Future.successful(3.14)
    //#future-result
    import play.api.libs.concurrent.Execution.Implicits.defaultContext

    val futurePIValue: Future[Double] = computePIAsynchronously()
    val futureResult: Future[Result] = futurePIValue.map { pi =>
      Ok("PI value computed: " + pi)
    }
    //#future-result
    futureResult
  }

  def intensiveComputation() = 10

  def intensiveComp = {
    //#intensive-computation
    import play.api.libs.concurrent.Execution.Implicits.defaultContext

    val futureInt: Future[Int] = scala.concurrent.Future {
      intensiveComputation()
    }
    //#intensive-computation
    futureInt
  }

  def asyncResult = {

    //#async-result
    import play.api.libs.concurrent.Execution.Implicits.defaultContext

    def index = Action.async {
      val futureInt = scala.concurrent.Future { intensiveComputation() }
      futureInt.map(i => Ok("Got result: " + i))
    }
    //#async-result

    index
  }

  def timeout(t: Long) = {
    val actorSystem = akka.actor.ActorSystem()

    //#timeout
    import play.api.libs.concurrent.Execution.Implicits.defaultContext
    import scala.concurrent.duration._
    import play.api.libs.concurrent.Timeout

    def index = Action.async {
      Timeout.timeout(actorSystem, 1.seconds) {
        intensiveComputation().map { i =>
          Ok("Got result: " + i)
        }
      }.recover {
        case e: TimeoutException =>
          InternalServerError("timeout")
      }
    }
    //#timeout

    def intensiveComputation() = Future {
      Thread.sleep(t)
      10
    }

    index
  }
}
