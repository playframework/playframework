/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.testhelpers {
  import java.lang.reflect.Method
  import java.util.concurrent.CompletableFuture
  import java.util.concurrent.CompletionStage

  import scala.concurrent.ExecutionContext

  import akka.stream.Materializer
  import play.api.mvc.Action
  import play.api.mvc.Request
  import play.api.test.Helpers
  import play.core.j._
  import play.mvc.Controller
  import play.mvc.Http
  import play.mvc.Result

  abstract class MockJavaAction(handlerComponents: JavaHandlerComponents)
      extends Controller
      with Action[Http.RequestBody] {
    self =>

    private lazy val action = new JavaAction(handlerComponents) {
      val annotations =
        new JavaActionAnnotations(controller, method, this.handlerComponents.httpConfiguration.actionComposition)

      def parser = {
        play.HandlerInvokerFactoryAccessor.javaBodyParserToScala(
          this.handlerComponents.getBodyParser(annotations.parser)
        )
      }

      def invocation(req: Http.Request) = self.invocation(req)
    }

    def parser = action.parser

    def apply(request: Request[Http.RequestBody]) = action.apply(request)

    private val controller = this.getClass
    private val method     = MockJavaActionJavaMocker.findActionMethod(this)

    def executionContext: ExecutionContext = handlerComponents.executionContext

    def invocation(req: Http.Request) = {
      // We can't do
      // method.invoke(this, if (method.getParameterCount == 1) { req } else { Array.empty })
      // or similar because of bugs JDK-4071957 / JDK-8046171
      (if (method.getParameterCount == 1) {
         method.invoke(this, req)
       } else {
         method.invoke(this)
       }) match {
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
        !method.isSynthetic &&
        (method.getParameterCount == 0 ||
          (method.getParameterCount == 1 && method.getParameterTypes()(0) == classOf[Http.Request]))
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
