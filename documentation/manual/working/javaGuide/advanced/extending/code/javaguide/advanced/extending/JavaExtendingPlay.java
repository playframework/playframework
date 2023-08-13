/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.advanced.extending;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.ws.WSClient;

public class JavaExtendingPlay {

  @Test
  void testModule() throws Exception {
    // #module-class-binding
    Application application = new GuiceApplicationBuilder().bindings(new MyModule()).build();
    // #module-class-binding
    MyApi api = application.injector().instanceOf(MyApi.class);

    assertNotNull(api);
  }

  @Test
  void testOverride() throws Exception {
    // #builtin-module-overrides
    Application application = new GuiceApplicationBuilder().overrides(new MyWSModule()).build();
    // #builtin-module-overrides
    WSClient wsClient = application.injector().instanceOf(WSClient.class);

    assertInstanceOf(MyWSClient.class, wsClient);
  }
}
