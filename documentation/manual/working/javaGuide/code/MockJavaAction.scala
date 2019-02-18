/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.testhelpers {

  import java.util.concurrent.CompletableFuture
  import java.util.concurrent.CompletionStage

  import play.api.mvc.Action
  import play.api.mvc.Request
  import play.core.j._
  import play.mvc.Controller
  import play.mvc.Http
  import play.mvc.Result
  import play.api.test.Helpers
  import java.lang.reflect.Method

  import akka.stream.Materializer

  import scala.concurrent.ExecutionContext

  abstract class MockJavaAction(handlerComponents: JavaHandlerComponents)
      extends Controller
      with Action[Http.RequestBody] {
    self =>

    private lazy val action = new JavaAction(handlerComponents) {
      val annotations =
        new JavaActionAnnotations(controller, method, handlerComponents.httpConfiguration.actionComposition)

      def parser = {
        play.HandlerInvokerFactoryAccessor.javaBodyParserToScala(
          handlerComponents.getBodyParser(annotations.parser)
        )
      }

      def invocation = self.invocation
    }

    def parser = action.parser

    def apply(request: Request[Http.RequestBody]) = action.apply(request)

    private val controller = this.getClass
    private val method     = MockJavaActionJavaMocker.findActionMethod(this)

    def executionContext: ExecutionContext = handlerComponents.executionContext

    def invocation = {
      method.invoke(this) match {
        case r: Result             => CompletableFuture.completedFuture(r)
        case f: CompletionStage[_] => f.asInstanceOf[CompletionStage[Result]]
      }
    }
  }

  object MockJavaActionHelper {

    import Helpers.defaultAwaitTimeout

    def call(action: Action[Http.RequestBody], requestBuilder: play.mvc.Http.RequestBuilder)(
        implicit mat: Materializer
    ): Result = {
      Helpers
        .await(requestBuilder.body() match {
          case null =>
            action.apply(requestBuilder.build().asScala)
          case other =>
            Helpers.call(action, requestBuilder.build().asScala, other.asBytes())
        })
        .asJava
    }

    def callWithStringBody(
        action: Action[Http.RequestBody],
        requestBuilder: play.mvc.Http.RequestBuilder,
        body: String
    )(implicit mat: Materializer): Result = {
      Helpers.await(Helpers.call(action, requestBuilder.build().asScala, body)).asJava
    }

    def setContext(request: play.mvc.Http.RequestBuilder, contextComponents: JavaContextComponents): Unit = {
      Http.Context.current.set(JavaHelpers.createJavaContext(request.build().asScala, contextComponents))
    }

    def removeContext: Unit = Http.Context.current.remove()
  }

  /**
   * Java should be mocked.
   *
   * This object exists because if you put its implementation in the MockJavaAction, then when other things go
   *
   * import static MockJavaAction.*;
   *
   * They get a compile error from javac, and it seems to be because javac is trying ot import a synthetic method
   * that it shouldn't.  Hence, this object mocks java.
   */
  object MockJavaActionJavaMocker {
    def findActionMethod(obj: AnyRef): Method = {
      val maybeMethod = obj.getClass.getDeclaredMethods.find { method =>
        !method.isSynthetic && method.getParameterCount == 0
      }
      val theMethod = maybeMethod.getOrElse(
        throw new RuntimeException("MockJavaAction must declare at least one non synthetic method")
      )
      theMethod.setAccessible(true)
      theMethod
    }
  }

}

/**
 * javaBodyParserToScala is private to play
 */
package play {

  object HandlerInvokerFactoryAccessor {
    val javaBodyParserToScala = play.core.routing.HandlerInvokerFactory.javaBodyParserToScala _
  }

}
