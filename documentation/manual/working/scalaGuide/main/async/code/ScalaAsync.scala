/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.async.scalaasync

import javax.inject.Inject

import scala.concurrent.{Future, ExecutionContext}
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

trait MyExecutionContext extends ExecutionContext

class MyExecutionContextImpl @Inject()(system: ActorSystem)
  extends CustomExecutionContext(system, "my.executor") with MyExecutionContext

class HomeController @Inject()(myExecutionContext: MyExecutionContext) extends Controller {
  def index = Action.async {
    Future {
      // Call some blocking API
      Ok("result of blocking call")
    }(myExecutionContext)
  }
}
//#my-execution-context

class ScalaAsyncSamples @Inject() (implicit actorSystem: ActorSystem, ec: ExecutionContext) extends Controller {

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
    def intensiveComputation() = {
      Thread.sleep(t)
      10
    }
    //#timeout
    import scala.concurrent.duration._
    import akka.pattern.after

    def index = Action.async {
      val futureInt = scala.concurrent.Future { intensiveComputation() }
      val timeoutFuture = after(1.second, actorSystem.scheduler)(Future.successful("Oops"))
      Future.firstCompletedOf(Seq(futureInt, timeoutFuture)).map {
        case i: Int => Ok("Got result: " + i)
        case t: String => InternalServerError(t)
      }
    }
    //#timeout
    index
  }
}
