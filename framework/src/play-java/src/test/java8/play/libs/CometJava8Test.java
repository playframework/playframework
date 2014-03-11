/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs;

import java.util.concurrent.CountDownLatch;
import org.junit.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;

public class CometJava8Test {

    @Test
    public void testWhenConnectedFactory() throws Exception {
        final CountDownLatch invoked = new CountDownLatch(1);
        Comet comet = Comet.whenConnected("test", c -> invoked.countDown());
        comet.onConnected();
        assertTrue(invoked.await(1, SECONDS));
    }
}
