/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.test;

import org.junit.Test;
import play.Play;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WithApplicationTest extends WithApplication {

    @Test
    public void withApplicationShouldProvideAnApplication() {
        assertNotNull(app);
        assertNotNull(Play.application());
    }

    @Test
    public void withApplicationShouldCleanUpApplication() {
        stopPlay();
        assertNull(app);
        assertTrue(play.api.Play.maybeApplication().isEmpty());
    }
}
