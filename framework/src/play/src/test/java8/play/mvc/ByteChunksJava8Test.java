/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.mvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import org.junit.Test;
import play.api.libs.iteratee.TestChannel;
import play.libs.F;
import play.mvc.Results.ByteChunks;
import play.mvc.Results.Chunks;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;

public class ByteChunksJava8Test {

    @Test
    public void testWhenReadyFactory() throws Exception {
        final TestChannel<byte[]> testChannel = new TestChannel<byte[]>();
        final Chunks.Out<byte[]> testOut = new Chunks.Out<byte[]>(testChannel, new ArrayList<F.Callback0>());
        final CountDownLatch ready = new CountDownLatch(1);
        Chunks<byte[]> chunks = ByteChunks.whenReady(out -> {
            ready.countDown();
            out.write("a".getBytes());
            out.write("b".getBytes());
            out.close();
        });
        chunks.onReady(testOut);

        assertTrue("ByteChunks.onReady callback was not invoked", ready.await(1, SECONDS));
        testChannel.expect("a".getBytes(), Arrays::equals);
        testChannel.expect("b".getBytes(), Arrays::equals);
        testChannel.expectEOF();
        testChannel.expectEnd();
        testChannel.expectEmpty();
    }
}
