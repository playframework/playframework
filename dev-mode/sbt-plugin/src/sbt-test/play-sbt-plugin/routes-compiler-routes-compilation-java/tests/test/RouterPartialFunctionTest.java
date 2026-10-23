/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.test.Helpers.fakeRequest;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import play.api.mvc.Handler;
import play.api.mvc.RequestHeader;
import play.api.routing.Router;
import scala.Function1;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;

public class RouterPartialFunctionTest extends AbstractRoutesTest {

  private PartialFunction<RequestHeader, Handler> routes() {
    return app.injector().instanceOf(Router.class).routes();
  }

  @Test
  public void reportsWhetherRequestsMatch() {
    var routes = routes();
    assertThat(routes.isDefinedAt(fakeRequest("GET", "/method/get").build().asScala())).isTrue();
    assertThat(routes.isDefinedAt(fakeRequest("GET", "/module/index").build().asScala())).isTrue();
    assertThat(routes.isDefinedAt(fakeRequest("POST", "/method/get").build().asScala())).isFalse();
    assertThat(routes.isDefinedAt(fakeRequest("GET", "/missing-route").build().asScala()))
        .isFalse();
    assertThat(routes.isDefinedAt(fakeRequest("GET", "/module/missing-route").build().asScala()))
        .isFalse();
  }

  @Test
  public void appliesMatchedRoutesAndRejectsMissingRoutes() {
    var routes = routes();
    assertThat(routes.apply(fakeRequest("GET", "/method/get").build().asScala())).isNotNull();
    assertThat(routes.apply(fakeRequest("GET", "/module/index").build().asScala())).isNotNull();
    assertThatThrownBy(() -> routes.apply(fakeRequest("GET", "/missing-route").build().asScala()))
        .isInstanceOf(scala.MatchError.class);
  }

  @Test
  public void usesFallbackOnlyForMissingRoutes() {
    var routes = routes();
    var fallbackCalls = new AtomicInteger();
    Handler fallback = new Handler() {};
    Function1<RequestHeader, Handler> defaultFunction =
        request -> {
          fallbackCalls.incrementAndGet();
          return fallback;
        };

    var matched =
        routes.applyOrElse(fakeRequest("GET", "/method/get").build().asScala(), defaultFunction);
    assertThat(matched).isNotNull().isNotSameAs(fallback);
    assertThat(fallbackCalls.get()).isZero();
    assertThat(
            routes.applyOrElse(
                fakeRequest("GET", "/missing-route").build().asScala(), defaultFunction))
        .isSameAs(fallback);
    assertThat(fallbackCalls.get()).isEqualTo(1);
  }

  @Test
  public void runsActionOnlyForMatchedRoutes() {
    var actionCalls = new AtomicInteger();
    Function1<Handler, BoxedUnit> action =
        handler -> {
          assertThat(handler).isNotNull();
          actionCalls.incrementAndGet();
          return BoxedUnit.UNIT;
        };
    var run = routes().runWith(action);

    assertThat(run.apply(fakeRequest("GET", "/method/get").build().asScala())).isEqualTo(true);
    assertThat(actionCalls.get()).isEqualTo(1);
    assertThat(run.apply(fakeRequest("GET", "/missing-route").build().asScala())).isEqualTo(false);
    assertThat(actionCalls.get()).isEqualTo(1);
  }
}
