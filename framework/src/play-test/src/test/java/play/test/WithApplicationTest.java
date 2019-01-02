/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test;

import org.junit.Test;
import play.i18n.MessagesApi;

import static org.junit.Assert.assertNotNull;

/**
 * Tests WithApplication functionality.
 */
public class WithApplicationTest extends WithApplication {

    @Test
    public void shouldHaveAnAppInstantiated() {
        assertNotNull(app);
    }

    @Test
    public void shouldHaveAMaterializerInstantiated() {
        assertNotNull(mat);
    }

    @Test
    public void withInstanceOf() {
        MessagesApi messagesApi = instanceOf(MessagesApi.class);
        assertNotNull(messagesApi);
    }
}
