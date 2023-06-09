/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test.junit5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import play.inject.guice.GuiceApplicationBuilder;

class ApplicationExtensionOverrideTest {
  @RegisterExtension
  static ApplicationExtension applicationExtension =
      new ApplicationExtension(
          new GuiceApplicationBuilder().configure("extraConfig", "valueForExtraConfig").build()
      );

  @Test
  void shouldHaveAnAppInstantiated() {
    assertNotNull(applicationExtension.getApplication());
  }

  @Test
  void shouldHaveAMaterializerInstantiated() {
    assertNotNull(applicationExtension.getMaterializer());
  }

  @Test
  void shouldHaveExtraConfiguration() {
    assertEquals(
        "valueForExtraConfig",
        applicationExtension.getApplication().config().getString("extraConfig"));
  }
}
