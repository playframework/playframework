/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test.junit4;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;

/** Tests WithApplication functionality. */
public class WithApplicationOverrideTest extends WithApplication {

  @Override
  protected Application provideApplication() {
    return new GuiceApplicationBuilder().configure("extraConfig", "valueForExtraConfig").build();
  }

  @Test
  public void shouldHaveAnAppInstantiated() {
    assertNotNull(app);
  }

  @Test
  public void shouldHaveAMaterializerInstantiated() {
    assertNotNull(mat);
  }

  @Test
  public void shouldHaveExtraConfiguration() {
    assertThat(app.config().getString("extraConfig"), equalTo("valueForExtraConfig"));
  }
}
