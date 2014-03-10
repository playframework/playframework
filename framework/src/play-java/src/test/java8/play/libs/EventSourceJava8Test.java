/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs;

import java.util.concurrent.CountDownLatch;
import org.junit.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;

public class EventSourceJava8Test {

    @Test
    public void testWhenConnectedFactory() throws Exception {
        final CountDownLatch invoked = new CountDownLatch(1);
        EventSource eventSource = EventSource.whenConnected(es -> invoked.countDown());
        eventSource.onConnected();
        assertTrue(invoked.await(1, SECONDS));
    }
}
