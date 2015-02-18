/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import org.junit.Test;
import play.api.libs.iteratee.TestChannel;
import play.mvc.Results.Chunks;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.fest.assertions.Assertions.assertThat;

public class CometTest {

    @Test
    public void testOnReady() throws Exception {
        final TestChannel<String> testChannel = new TestChannel<>();
        final Chunks.Out<String> out = new Chunks.Out<>(testChannel, new ArrayList<>());
        CountDownLatch invoked = new CountDownLatch(1);
        Comet comet = Comet.whenConnected("test", c -> invoked.countDown());
        comet.onReady(out);

        assertThat(invoked.await(1, SECONDS)).isTrue();
        testChannel.expect(comet.initialBuffer());
        testChannel.expectEmpty();
    }

    @Test
    public void testMessageSends() {
        final TestChannel<String> testChannel = new TestChannel<>();
        final Chunks.Out<String> out = new Chunks.Out<>(testChannel, new ArrayList<>());
        Comet comet = Comet.whenConnected("test", c -> {});
        comet.onReady(out);

        String message = "test";
        JsonNode json = Json.newObject().put("test", "data");
        String jsonMessage = Json.stringify(json);

        comet.sendMessage(message);
        comet.sendMessage(json);

        testChannel.expect(comet.initialBuffer());
        testChannel.expect("<script type=\"text/javascript\">test('" + message + "');</script>");
        testChannel.expect("<script type=\"text/javascript\">test(" + jsonMessage + ");</script>");
        testChannel.expectEmpty();
    }

    @Test
    public void testClose() throws Exception {
        final TestChannel<String> testChannel = new TestChannel<>();
        final Chunks.Out<String> out = new Chunks.Out<>(testChannel, new ArrayList<>());
        Comet comet = Comet.whenConnected("test", c -> {});
        comet.onReady(out);

        comet.close();

        testChannel.expect(comet.initialBuffer());
        testChannel.expectEOF();
        testChannel.expectEnd();
        testChannel.expectEmpty();
    }
}
