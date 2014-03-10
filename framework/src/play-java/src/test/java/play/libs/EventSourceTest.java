/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs;

import java.util.concurrent.CountDownLatch;
import org.junit.Test;
import play.libs.F;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;

public class EventSourceTest {

    @Test
    public void testWhenConnectedFactory() throws Exception {
        final CountDownLatch invoked = new CountDownLatch(1);
        EventSource eventSource = EventSource.whenConnected(new F.Callback<EventSource>() {
            @Override
            public void invoke(EventSource es) {
                invoked.countDown();
            }
        });
        eventSource.onConnected();
        assertTrue(invoked.await(1, SECONDS));
    }
}
