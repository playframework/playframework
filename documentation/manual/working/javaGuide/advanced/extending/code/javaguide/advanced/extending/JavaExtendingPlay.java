/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.advanced.extending;

import org.junit.Test;

import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.ws.WSClient;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class JavaExtendingPlay {

    @Test
    public void testModule() throws Exception {
        // #module-class-binding
        Application application = new GuiceApplicationBuilder()
                .bindings(new MyModule())
                .build();
        // #module-class-binding
        MyApi api = application.injector().instanceOf(MyApi.class);

        assertNotNull(api);
    }

    @Test
    public void testOverride() throws Exception {
        // #builtin-module-overrides
        Application application = new GuiceApplicationBuilder()
                .overrides(new MyWSModule())
                .build();
        // #builtin-module-overrides
        WSClient wsClient = application.injector().instanceOf(WSClient.class);

        assertThat(wsClient, instanceOf(MyWSClient.class));
    }

}
