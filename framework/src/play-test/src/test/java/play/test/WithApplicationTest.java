/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
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
    public void withInject() {
        MessagesApi messagesApi = inject(MessagesApi.class);
        assertNotNull(messagesApi);
    }
}
