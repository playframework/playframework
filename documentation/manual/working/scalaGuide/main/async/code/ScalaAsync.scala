/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.async.scalaasync

import javax.inject.Inject

import scala.concurrent._
import akka.actor._
import play.api._
import play.api.mvc._
import play.api.test._

class ScalaAsyncSpec extends PlaySpecification {

  def samples(implicit app: Application): ScalaAsyncSamples = app.injector.instanceOf[ScalaAsyncSamples]

  "scala async" should {
    "allow returning a future" in new WithApplication() {
      contentAsString(samples.futureResult) must startWith("PI value computed: 3.14")
    }

    "allow dispatching an intensive computation" in new WithApplication() {
      await(samples.intensiveComp) must_== 10
    }

    "allow returning an async result" in new WithApplication() {
      contentAsString(samples.asyncResult()(FakeRequest())) must_== "Got result: 10"
    }

    "allow timing out a future" in new WithApplication() {
      status(samples.timeout(1200)(FakeRequest())) must_== INTERNAL_SERVER_ERROR
      status(samples.timeout(10)(FakeRequest())) must_== OK
    }
  }
}

//#my-execution-context
import play.api.libs.concurrent.CustomExecutionContext

// Make sure to bind the new context class to this trait using one of the custom
// binding techniques listed on the "Scala Dependency Injection" documentation page
trait MyExecutionContext extends ExecutionContext

class MyExecutionContextImpl @Inject()(system: ActorSystem)
    extends CustomExecutionContext(system, "my.executor")
    with MyExecutionContext

class HomeController @Inject()(myExecutionContext: MyExecutionContext, val controllerComponents: ControllerComponents)
    extends BaseController {
  def index = Action.async {
    Future {
      // Call some blocking API
      Ok("result of blocking call")
    }(myExecutionContext)
  }
}
//#my-execution-context

class ScalaAsyncSamples @Inject()(val controllerComponents: ControllerComponents)(
    implicit actorSystem: ActorSystem,
    ec: ExecutionContext
) extends BaseController {

  def futureResult = {
    def computePIAsynchronously() = Future.successful(3.14)
    //#future-result

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
    val futureInt: Future[Int] = scala.concurrent.Future {
      intensiveComputation()
    }
    //#intensive-computation
    futureInt
  }

  def asyncResult = {

    //#async-result
    def index = Action.async {
      val futureInt = scala.concurrent.Future { intensiveComputation() }
      futureInt.map(i => Ok("Got result: " + i))
    }
    //#async-result

    index
  }

  def timeout(t: Long) = {
    def intensiveComputation() = Future {
      Thread.sleep(t)
      10
    }

    //#timeout
    import scala.concurrent.duration._
    import play.api.libs.concurrent.Futures._

    def index = Action.async {
      // You will need an implicit Futures for withTimeout() -- you usually get
      // that by injecting it into your controller's constructor
      intensiveComputation()
        .withTimeout(1.seconds)
        .map { i =>
          Ok("Got result: " + i)
        }
        .recover {
          case e: scala.concurrent.TimeoutException =>
            InternalServerError("timeout")
        }
    }
    //#timeout
    index
  }
}
