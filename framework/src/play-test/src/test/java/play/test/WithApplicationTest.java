/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
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
        assertTrue(play.api.Play.maybeApplication().nonEmpty());
    }

    @Test
    public void withApplicationShouldCleanUpApplication() {
        stopPlay();
        assertNull(app);
        assertTrue(play.api.Play.maybeApplication().isEmpty());
    }
}
