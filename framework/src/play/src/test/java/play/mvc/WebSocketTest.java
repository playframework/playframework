/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.mvc;

import java.util.concurrent.CountDownLatch;
import org.junit.Test;
import play.api.libs.iteratee.TestChannel;
import play.libs.F;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;

public class WebSocketTest {

    @Test
    public void testWebSocket() throws Throwable {
        final CountDownLatch ready = new CountDownLatch(1);
        final CountDownLatch message = new CountDownLatch(1);
        final CountDownLatch close = new CountDownLatch(1);

        final TestChannel<String> testChannel = new TestChannel<>();
        final WebSocket.Out<String> wsOut = new WebSocket.Out<String>() {
            @Override
            public void write(String frame) {
                testChannel.push(frame);
            }
            @Override
            public void close() {
                testChannel.eofAndEnd();
            }
        };

        final WebSocket.In<String> wsIn = new WebSocket.In<>();

        WebSocket<String> webSocket = WebSocket.whenReady((in, out) -> {
            ready.countDown();
            in.onMessage(m -> message.countDown());
            in.onClose(close::countDown);
            out.write("message");
            out.close();
        });

        webSocket.onReady(wsIn, wsOut);

        assertTrue("WebSocket.onReady callback was not invoked", ready.await(1, SECONDS));

        for (F.Callback<String> callback : wsIn.callbacks) {
            callback.invoke("message");
        }

        assertTrue("WebSocket.In.onMessage callback was not invoked", message.await(1, SECONDS));

        for (F.Callback0 callback : wsIn.closeCallbacks) {
            callback.invoke();
        }

        assertTrue("WebSocket.In.onClose callback was not invoked", close.await(1, SECONDS));

        testChannel.expect("message");
        testChannel.expectEOF();
        testChannel.expectEnd();
        testChannel.expectEmpty();
    }

    @Test
    public void testWhenReadyFactory() throws Exception {
        final CountDownLatch ready = new CountDownLatch(1);

        WebSocket<String> webSocket = WebSocket.whenReady((in, out) -> ready.countDown());

        webSocket.onReady(null, null);

        assertTrue("WebSocket.onReady callback was not invoked", ready.await(1, SECONDS));
    }
}
