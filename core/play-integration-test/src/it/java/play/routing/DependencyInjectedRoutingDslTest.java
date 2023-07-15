/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routing;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.test.Helpers;

class DependencyInjectedRoutingDslTest extends AbstractRoutingDslTest {

  private static Application app;

  @BeforeAll
  public static void startApp() {
    app = new GuiceApplicationBuilder().configure("play.allowGlobalApplication", true).build();
    Helpers.start(app);
  }

  @Override
  Application application() {
    return app;
  }

  @Override
  RoutingDsl routingDsl() {
    return app.injector().instanceOf(RoutingDsl.class);
  }

  @AfterAll
  public static void stopApp() {
    Helpers.stop(app);
  }
}
