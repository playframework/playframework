/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.system

import org.specs2.mutable.Specification
import java.net.URL
import play.api.mvc.{SimpleResult, Handler, RequestHeader}
import scala.concurrent.Future

class HandlerExecutorSpec extends Specification {

  "handler executor discovery" should {
    "discover objects" in {
      val handlers = HandlerExecutor.discoverHandlerExecutors(new MockClassLoader("foo", resource("object.handlers")),
        "foo", Nil)
      handlers must haveSize(1)
      handlers(0) must_== ObjectHandlerExecutor
    }
    "discover classes" in {
      val handlers = HandlerExecutor.discoverHandlerExecutors(new MockClassLoader("foo", resource("class.handlers")),
        "foo", Nil)
      handlers must haveSize(1)
      handlers(0) must beAnInstanceOf[ClassHandlerExecutor]
    }
    "discover multiple handlers in one file" in {
      val handlers = HandlerExecutor.discoverHandlerExecutors(new MockClassLoader("foo", resource("multiple.handlers")),
        "foo", Nil)
      handlers must haveSize(2)
    }
    "discover handlers in multiple files" in {
      val handlers = HandlerExecutor.discoverHandlerExecutors(new MockClassLoader("foo", resource("object.handlers"),
        resource("class.handlers")), "foo", Nil)
      handlers must haveSize(2)
    }
    "only discover distinct handlers" in {
      val handlers = HandlerExecutor.discoverHandlerExecutors(new MockClassLoader("foo", resource("multiple.handlers"),
        resource("class.handlers")), "foo", Nil)
      handlers must haveSize(2)
    }
    "discover no handlers" in {
      val handlers = HandlerExecutor.discoverHandlerExecutors(new MockClassLoader("foo"), "foo", Nil)
      handlers must haveSize(0)
    }
    "append builtins to no handlers" in {
      val handlers = HandlerExecutor.discoverHandlerExecutors(new MockClassLoader("foo"), "foo", Seq(ObjectHandlerExecutor))
      handlers must haveSize(1)
    }
    "append builtins to discovered handlers" in {
      val handlers = HandlerExecutor.discoverHandlerExecutors(new MockClassLoader("foo", resource("class.handlers")),
        "foo", Seq(ObjectHandlerExecutor))
      handlers must haveSize(2)
      handlers(0) must beAnInstanceOf[ClassHandlerExecutor]
      handlers(1) must_== ObjectHandlerExecutor
    }
  }

  "handler executor execution" should {
    val context = new MockHandlerExecutorContext(Seq(ObjectHandlerExecutor, new ClassHandlerExecutor))

    "execute first handler executor" in {
      val backend = new MockBackend("object")
      context(null, backend, null) must beSome
      backend.handledBy must beSome("object")
    }
    "execute all handler executors" in {
      val backend = new MockBackend("class")
      context(null, backend, null) must beSome
      backend.handledBy must beSome("class")
    }
    "return none if no handler executor matches" in {
      val backend = new MockBackend("something else")
      context(null, backend, null) must beNone
      backend.handledBy must beNone
    }
  }

  def resource(name: String) = this.getClass.getResource(name)
}

class MockClassLoader(lookupName: String, resources: URL*) extends ClassLoader {
  import scala.collection.JavaConverters._
  override def getResources(name: String) = name match {
    case `lookupName` => new java.util.Vector(resources.toSeq.asJava).elements()
    case _ => new java.util.Vector().elements()
  }
}

class MockBackend(val handleWith: String, var handledBy: Option[String] = None)

class MockHandlerExecutorContext(val handlerExecutors: Seq[HandlerExecutor[MockBackend]]) extends HandlerExecutorContext[MockBackend] {

  def application = None

  def sendResult(request: RequestHeader, backend: MockBackend, result: Future[SimpleResult]) = {
    backend.handledBy = Some("error")
    Some(Future.successful(()))
  }
}

object ObjectHandlerExecutor extends HandlerExecutor[MockBackend] {
  def apply(context: HandlerExecutorContext[MockBackend], request: RequestHeader, backend: MockBackend, handler: Handler) = {
    backend.handleWith match {
      case "object" =>
        backend.handledBy = Some("object")
        Some(Future.successful(()))
      case _ => None
    }
  }
}

class ClassHandlerExecutor extends HandlerExecutor[MockBackend] {
  def apply(context: HandlerExecutorContext[MockBackend], request: RequestHeader, backend: MockBackend, handler: Handler) = {
    backend.handleWith match {
      case "class" =>
        backend.handledBy = Some("class")
        Some(Future.successful(()))
      case _ => None
    }
  }
}