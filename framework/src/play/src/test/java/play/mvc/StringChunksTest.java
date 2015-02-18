/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.mvc;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import org.junit.Test;
import play.api.libs.iteratee.TestChannel;
import play.mvc.Results.Chunks;
import play.mvc.Results.StringChunks;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;

public class StringChunksTest {

    @Test
    public void testStringChunks() throws Exception {
        final TestChannel<String> testChannel = new TestChannel<>();
        final Chunks.Out<String> testOut = new Chunks.Out<>(testChannel, new ArrayList<>());
        final CountDownLatch ready = new CountDownLatch(1);
        Chunks<String> chunks = StringChunks.whenReady(out -> {
            ready.countDown();
            out.write("a");
            out.write("b");
            out.close();
        });
        chunks.onReady(testOut);

        assertTrue("StringChunks.onReady callback was not invoked", ready.await(1, SECONDS));
        testChannel.expect("a");
        testChannel.expect("b");
        testChannel.expectEOF();
        testChannel.expectEnd();
        testChannel.expectEmpty();
    }
}
