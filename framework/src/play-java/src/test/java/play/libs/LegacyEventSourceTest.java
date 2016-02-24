/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import org.junit.Test;
import play.api.libs.iteratee.TestChannel;
import play.mvc.Results.Chunks;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.fest.assertions.Assertions.assertThat;
import static play.libs.LegacyEventSource.Event.event;

public class LegacyEventSourceTest {

    @Test
    public void testOnReady() throws Exception {
        final TestChannel<String> testChannel = new TestChannel<>();
        final Chunks.Out<String> out = new Chunks.Out<>(testChannel, new ArrayList<>());
        final CountDownLatch invoked = new CountDownLatch(1);
        LegacyEventSource eventSource = new LegacyEventSource() {
            @Override
            public void onConnected() {
                invoked.countDown();
            }
        };
        eventSource.onReady(out);

        assertThat(invoked.await(1, SECONDS)).isTrue();
        testChannel.expectEmpty();
    }

    @Test
    public void testSends() {
        final TestChannel<String> testChannel = new TestChannel<>();
        final Chunks.Out<String> out = new Chunks.Out<>(testChannel, new ArrayList<>());
        LegacyEventSource eventSource = new LegacyEventSource() {
            @Override
            public void onConnected() {}
        };
        eventSource.onReady(out);

        eventSource.send(new LegacyEventSource.Event("data1", "id1", "name1"));
        eventSource.send(event("data2"));
        eventSource.send(event("data3").withId("id3"));
        eventSource.send(event("data4").withName("name4"));
        eventSource.send(event("data5").withId("id5").withName("name5"));
        eventSource.send(event(Json.newObject().put("test", "data")));

        testChannel.expect("event: name1\nid: id1\ndata: data1\n\n");
        testChannel.expect("data: data2\n\n");
        testChannel.expect("id: id3\ndata: data3\n\n");
        testChannel.expect("event: name4\ndata: data4\n\n");
        testChannel.expect("event: name5\nid: id5\ndata: data5\n\n");
        testChannel.expect("data: {\"test\":\"data\"}\n\n");
        testChannel.expectEmpty();
    }

    @Test
    public void testClose() throws Exception {
        final TestChannel<String> testChannel = new TestChannel<>();
        final Chunks.Out<String> out = new Chunks.Out<>(testChannel, new ArrayList<>());
        LegacyEventSource eventSource = new LegacyEventSource() {
            @Override
            public void onConnected() {}
        };
        eventSource.onReady(out);

        eventSource.close();

        testChannel.expectEOF();
        testChannel.expectEnd();
        testChannel.expectEmpty();
    }

    @Test
    public void testWhenConnectedFactory() throws Exception {
        final CountDownLatch invoked = new CountDownLatch(1);
        LegacyEventSource eventSource = LegacyEventSource.whenConnected(es -> invoked.countDown());
        eventSource.onConnected();
        assertThat(invoked.await(1, SECONDS)).isTrue();
    }
}
