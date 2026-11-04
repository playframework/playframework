/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import play.api.http.DefaultHttpErrorHandler$;
import play.api.mvc.EssentialAction;
import play.api.mvc.Handler;
import play.api.mvc.Handler$;
import play.api.mvc.RequestHeader;
import play.api.routing.HandlerDef;
import play.api.routing.Router;
import play.libs.Scala;
import play.mvc.Http;
import play.mvc.Results;
import scala.Tuple2;
import scala.Tuple3;
import scala.runtime.BoxedUnit;

/** A router written like the routes compiler generates it as Java source code. */
public class JavaGeneratedRouterTest {

  /** A handler that records the bound parameters of a route. */
  static final class Recorded implements Handler {
    final String route;
    final List<Object> params;

    Recorded(String route, Object[] params) {
      this.route = route;
      this.params = List.of(params);
    }
  }

  /** The included router, which records the prefixes it gets. */
  static final class ModuleRouter extends JavaGeneratedRouter {
    static final List<String> prefixes = new ArrayList<>();

    ModuleRouter(String prefix) {
      super(DefaultHttpErrorHandler$.MODULE$, prefix, "module");
      route("GET", "index", staticPart("index"))
          .handler("controllers.Module", "index")
          .documentation("controllers.Module.index")
          .to((Object[] p) -> new Recorded("module", p));
    }

    @Override
    public ModuleRouter withPrefix(String addPrefix) {
      var prefix = Router.concatPrefix(addPrefix, prefix());
      prefixes.add(prefix);
      return new ModuleRouter(prefix);
    }
  }

  static final class TestRouter extends JavaGeneratedRouter {
    private final ModuleRouter module;

    TestRouter(ModuleRouter module, String prefix) {
      super(DefaultHttpErrorHandler$.MODULE$, prefix, "router");
      this.module = module;
      route("GET", "")
          .handler("controllers.Application", "index")
          .documentation("controllers.Application.index")
          .to((Object[] p) -> new Recorded("index", p));
      route("GET", "items/$id<[^/]+>", staticPart("items/"), dynamicPart("id", "[^/]+", true))
          .handler("controllers.Application", "item", Long.class, String.class)
          .documentation("controllers.Application.item(id:Long, q:String ?= \"none\")")
          .comments("Shows an item")
          .modifiers("nocsrf", "api")
          .params(
              pathParam("id", JavaBinders.pathLong()),
              queryParam("q", JavaBinders.queryString(), "none"))
          .to((Object[] p) -> new Recorded("item", p));
      route("GET", "items/first", staticPart("items/first"))
          .handler("controllers.Application", "first")
          .documentation("controllers.Application.first")
          .to((Object[] p) -> new Recorded("unreachable", p));
      route("GET", "optional", staticPart("optional"))
          .handler("controllers.Application", "optional", Optional.class)
          .documentation("controllers.Application.optional(x:java.util.Optional[Integer])")
          .params(queryParam("x", JavaBinders.queryOptional(JavaBinders.queryInteger())))
          .to((Object[] p) -> new Recorded("optional", p));
      route("GET", "result", staticPart("result"))
          .handler("controllers.Application", "result")
          .documentation("controllers.Application.result")
          .to((Object[] p) -> Results.ok());
      include(module.withPrefix(prefixed("module")));
    }

    @Override
    public TestRouter withPrefix(String addPrefix) {
      return new TestRouter(module, Router.concatPrefix(addPrefix, prefix()));
    }
  }

  private static TestRouter router() {
    return new TestRouter(new ModuleRouter("/"), "/");
  }

  private static RequestHeader request(String method, String uri) {
    return new Http.RequestBuilder().method(method).uri(uri).build().asScala();
  }

  private static Tuple2<RequestHeader, Handler> handle(Router router, String method, String uri) {
    var request = request(method, uri);
    return Handler$.MODULE$.applyStages(request, router.routes().apply(request));
  }

  private static Recorded recorded(Router router, String method, String uri) {
    return (Recorded) handle(router, method, uri)._2();
  }

  @Test
  public void bindPathAndQueryParameters() {
    var item = recorded(router(), "GET", "/items/42?q=shoes");
    assertThat(item.route).isEqualTo("item");
    assertThat(item.params).containsExactly(42L, "shoes");
  }

  @Test
  public void useDefaultValuesForMissingQueryParameters() {
    assertThat(recorded(router(), "GET", "/items/42").params).containsExactly(42L, "none");
    assertThat(recorded(router(), "GET", "/optional").params).containsExactly(Optional.empty());
    assertThat(recorded(router(), "GET", "/optional?x=3").params).containsExactly(Optional.of(3));
  }

  @Test
  public void notTryLaterRoutesWhenTheParametersOfAMatchingRouteFailToBind() {
    assertThat(handle(router(), "GET", "/items/first")._2())
        .isInstanceOf(EssentialAction.class)
        .isNotInstanceOf(Recorded.class);
  }

  @Test
  public void renderABadRequestForParametersThatFailToBind() {
    assertThat(handle(router(), "GET", "/items/notanumber")._2())
        .isInstanceOf(EssentialAction.class)
        .isNotInstanceOf(Recorded.class);
  }

  @Test
  public void addTheHandlerDefinitionToTheRequest() {
    var handled = handle(router(), "GET", "/items/42");
    HandlerDef handlerDef = handled._1().attrs().apply(Router.Attrs$.MODULE$.HandlerDef());
    assertThat(handlerDef.routerPackage()).isEqualTo("router");
    assertThat(handlerDef.controller()).isEqualTo("controllers.Application");
    assertThat(handlerDef.method()).isEqualTo("item");
    assertThat(Scala.asJava(handlerDef.parameterTypes())).containsExactly(Long.class, String.class);
    assertThat(handlerDef.verb()).isEqualTo("GET");
    assertThat(handlerDef.path()).isEqualTo("/items/$id<[^/]+>");
    assertThat(handlerDef.comments()).isEqualTo("Shows an item");
    assertThat(Scala.asJava(handlerDef.modifiers())).containsExactly("nocsrf", "api");
  }

  @Test
  public void invokeJavaActions() {
    assertThat(handle(router(), "GET", "/result")._2()).isInstanceOf(play.core.j.JavaHandler.class);
  }

  @Test
  public void prefixTheIncludedRouterWhenTheRouterIsCreated() {
    ModuleRouter.prefixes.clear();
    var router = router().withPrefix("/app");
    assertThat(ModuleRouter.prefixes).containsExactly("/module", "/app/module");
    assertThat(recorded(router, "GET", "/app/module/index").route).isEqualTo("module");
  }

  @Test
  public void prefixTheRoutes() {
    var router = router().withPrefix("/app");
    assertThat(recorded(router, "GET", "/app").route).isEqualTo("index");
    assertThat(recorded(router, "GET", "/app/items/1").params).containsExactly(1L, "none");
    assertThat(router.routes().isDefinedAt(request("GET", "/items/1"))).isFalse();
  }

  @Test
  public void documentTheRoutesAndIncludes() {
    var documentation = Scala.asJava(router().withPrefix("/app").documentation());
    assertThat(documentation)
        .containsExactly(
            Tuple3.apply("GET", "/app", "controllers.Application.index"),
            Tuple3.apply(
                "GET",
                "/app/items/$id<[^/]+>",
                "controllers.Application.item(id:Long, q:String ?= \"none\")"),
            Tuple3.apply("GET", "/app/items/first", "controllers.Application.first"),
            Tuple3.apply(
                "GET",
                "/app/optional",
                "controllers.Application.optional(x:java.util.Optional[Integer])"),
            Tuple3.apply("GET", "/app/result", "controllers.Application.result"),
            Tuple3.apply("GET", "/app/module/index", "controllers.Module.index"));
  }

  @Test
  public void behaveLikeAPartialFunction() {
    var routes = router().routes();
    assertThat(routes.isDefinedAt(request("GET", "/items/1"))).isTrue();
    assertThat(routes.isDefinedAt(request("GET", "/items/notanumber"))).isTrue();
    assertThat(routes.isDefinedAt(request("GET", "/module/index"))).isTrue();
    assertThat(routes.isDefinedAt(request("POST", "/items/1"))).isFalse();
    assertThat(routes.isDefinedAt(request("GET", "/module/missing"))).isFalse();
    assertThatThrownBy(() -> routes.apply(request("GET", "/missing")))
        .isInstanceOf(scala.MatchError.class);

    Handler fallback = new Handler() {};
    Handler missing = routes.applyOrElse(request("GET", "/missing"), r -> fallback);
    Handler matched = routes.applyOrElse(request("GET", "/items/1"), r -> fallback);
    assertThat(missing).isSameAs(fallback);
    assertThat(matched).isNotSameAs(fallback);

    var calls = new AtomicInteger();
    var run =
        routes.runWith(
            handler -> {
              calls.incrementAndGet();
              return BoxedUnit.UNIT;
            });
    assertThat(run.apply(request("GET", "/items/1"))).isEqualTo(true);
    assertThat(run.apply(request("GET", "/missing"))).isEqualTo(false);
    assertThat(calls.get()).isEqualTo(1);
  }

  @Test
  public void joinQueryStringItemsWithoutTheLeftOutOnes() {
    assertThat(JavaReverseRouting.queryString()).isEmpty();
    assertThat(JavaReverseRouting.queryString(null, "")).isEmpty();
    assertThat(JavaReverseRouting.queryString("a=1", null, "b=2")).isEqualTo("?a=1&b=2");
  }

  @Test
  public void compareReverseRouteArguments() {
    assertThat(JavaReverseRouting.<Long>equal(1L, 1L)).isTrue();
    assertThat(JavaReverseRouting.<String>equal(null, null)).isTrue();
    assertThat(JavaReverseRouting.<String>equal("a", null)).isFalse();
  }

  @Test
  public void createReverseRouteContexts() {
    var context = JavaReverseRouting.context("path", "/public", "page", 1);
    assertThat(Scala.asJava(context.fixedParams())).containsEntry("path", "/public");
    assertThat(Scala.asJava(context.fixedParams())).containsEntry("page", 1);
  }

  @Test
  public void describeUnmatchedReverseRouteArguments() {
    assertThat(JavaReverseRouting.noMatch("controllers.Application.show", "a", null))
        .hasMessage("No route for controllers.Application.show matches the arguments [a, null]");
  }

  @Test
  public void encodeDynamicPathParts() {
    assertThat(JavaReverseRouting.dynamicString("a b/c")).isEqualTo("a%20b%2Fc");
  }

  @Test
  public void renderJavascriptLiterals() {
    assertThat(JavaBinders.javascriptLiteral("a")).isEqualTo("\"a\"");
    assertThat(JavaBinders.javascriptLiteral(1L)).isEqualTo("1");
    assertThat(JavaBinders.javascriptLiteral(Optional.of("a"))).isEqualTo("\"a\"");
    assertThat(JavaBinders.javascriptLiteral(Optional.of(true))).isEqualTo("true");
    assertThat(JavaBinders.javascriptLiteral(Optional.empty())).isEqualTo("null");
  }

  @Test
  public void bindJavaPathBindables() {
    var binder = JavaBinders.path(Id.class);
    assertThat(binder.bind("id", "7").toOption().get().value).isEqualTo(7);
    assertThat(binder.unbind("id", new Id().bind("id", "8"))).isEqualTo("8");
  }

  /** A type that binds itself from a path parameter. */
  public static class Id implements play.mvc.PathBindable<Id> {
    int value;

    @Override
    public Id bind(String key, String txt) {
      var id = new Id();
      id.value = Integer.parseInt(txt);
      return id;
    }

    @Override
    public String unbind(String key) {
      return String.valueOf(value);
    }

    @Override
    public String javascriptUnbind() {
      return null;
    }
  }
}
