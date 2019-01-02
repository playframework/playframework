/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test;

import org.junit.Test;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Tests WithApplication functionality.
 */
public class WithApplicationOverrideTest extends WithApplication {

    @Override
    protected Application provideApplication() {
        return new GuiceApplicationBuilder()
                .configure("extraConfig", "valueForExtraConfig")
                .build();
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
