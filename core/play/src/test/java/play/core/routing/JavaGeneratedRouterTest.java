/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import org.junit.Test;
import play.api.http.HttpErrorHandler;
import play.api.mvc.Handler;
import play.api.mvc.RequestHeader;
import play.api.routing.HandlerDef;
import play.api.routing.Router;
import play.libs.Scala;
import play.mvc.Result;
import play.mvc.Results;
import scala.PartialFunction;
import scala.Tuple3;
import scala.collection.immutable.Seq;

/** The runtime lookups of generated Java routers, which fail at runtime instead of compile time. */
public class JavaGeneratedRouterTest {

  /** A type that Play has no binder for. */
  public static class Unsupported {}

  public static class TestController {
    public Result result() {
      return Results.ok();
    }

    public String text() {
      return "not a result";
    }
  }

  private static class TestRouter extends JavaGeneratedRouter {
    HandlerInvoker<Object> invokerFor(String method) {
      @SuppressWarnings("unchecked")
      HandlerInvoker<Object> invoker =
          (HandlerInvoker<Object>)
              createJavaInvoker(
                  false,
                  new HandlerDef(
                      getClass().getClassLoader(),
                      "router",
                      TestController.class.getName(),
                      method,
                      Scala.asScala(List.of()),
                      "GET",
                      "/",
                      "",
                      Scala.asScala(List.of())));
      return invoker;
    }

    @Override
    public HttpErrorHandler errorHandler() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Seq<Tuple3<String, String, String>> documentation() {
      throw new UnsupportedOperationException();
    }

    @Override
    public PartialFunction<RequestHeader, Handler> routes() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Router withPrefix(String prefix) {
      throw new UnsupportedOperationException();
    }
  }

  @Test
  public void rejectPathParametersOfUnsupportedTypes() {
    assertThatThrownBy(() -> JavaGeneratedRouter.pathBindableFor(Unsupported.class))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Don't know how to bind argument of type " + Unsupported.class);
  }

  @Test
  public void rejectQueryParametersOfUnsupportedTypes() {
    assertThatThrownBy(() -> JavaGeneratedRouter.queryStringBindableFor(Unsupported.class))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Don't know how to bind argument of type " + Unsupported.class);
    assertThatThrownBy(
            () -> JavaGeneratedRouter.queryStringBindableFor(Optional.class, Unsupported.class))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Don't know how to bind argument of type " + Unsupported.class);
  }

  @Test
  public void rejectJavascriptLiteralsOfUnsupportedTypes() {
    assertThatThrownBy(() -> JavaGeneratedRouter.javascriptLiteralFor(Unsupported.class))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Don't know how to bind argument of type " + Unsupported.class);
  }

  @Test
  public void invokeActionsReturningResults() {
    var controller = new TestController();
    assertThat(new TestRouter().invokerFor("result").call(controller::result))
        .isInstanceOf(Handler.class);
  }

  @Test
  public void failOnlyWhenCallingActionsWithUnsupportedReturnTypes() {
    var controller = new TestController();
    var invoker = new TestRouter().invokerFor("text");
    assertThatThrownBy(() -> invoker.call(controller::text)).isInstanceOf(ClassCastException.class);
  }
}
