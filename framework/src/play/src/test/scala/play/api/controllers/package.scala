/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api

import org.specs2.specification.{BeforeExample, AfterExample}
import org.specs2.Specification
import scala.concurrent.duration._
import play.api.mvc.Headers
import play.api.mvc.RequestHeader

abstract class ControllerWithFakeApplicationSpecification extends Specification with BeforeExample with AfterExample {
  sequential

  implicit val timeout: Duration = Duration(1, SECONDS)  // can be re-used in call to Await in specs

  case class TestRequestHeader(
    headers: Headers = new Headers {
      val data = Seq()
    },
    method: String = "GET",
    uri: String = "/",
    path: String = "",
    remoteAddress: String = "127.0.0.1",
    version: String = "HTTP/1.1",
    id: Long = 666,
    tags: Map[String, String] = Map.empty[String, String],
    queryString: Map[String, Seq[String]] = Map(),
    secure: Boolean = false
    ) extends RequestHeader

  val defaultTestRequest = TestRequestHeader()

  val appManager = new FakeApplicationManager(FakeApplication(config = Map("play.akka.actor.retrieveBodyParserTimeout" -> timeout.toString)))

  def before = appManager.push()

  def after = appManager.pop()
}

class FakeApplicationManager(app: FakeApplication) {
  /**
   * No atomics or STM here, because of the read lock.
   * We need a READ lock because ONLY ONE caller reading counter==0 can start the app. Many callers could otherwise correctly read counter==0 and thus all start the app.
   * We need a WRITE lock because if a caller is reading counter==1 while another is writing counter=0, the writer is also stopping the app, which the reader thinks will be running.
   */
  private var counter = 0

  def push(): Unit = synchronized {
    if (counter == 0) {
      Play.start(app)
    }
    counter += 1
  }

  def pop(): Unit = synchronized {
    counter -= 1
    if (counter == 0) {
      Play.stop()
    }
  }
}