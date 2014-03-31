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

public class ByteChunksTest {

    F.Function2<byte[], byte[], Boolean> arraysEqual = new F.Function2<byte[], byte[], Boolean>() {
        @Override
        public Boolean apply(byte[] a1, byte[] a2) {
            return Arrays.equals(a1, a2);
        }
    };

    @Test
    public void testByteChunks() throws Throwable {
        final TestChannel<byte[]> testChannel = new TestChannel<byte[]>();
        final Chunks.Out<byte[]> out = new Chunks.Out<byte[]>(testChannel, new ArrayList<F.Callback0>());
        Chunks<byte[]> chunks = new ByteChunks() {
            @Override
            public void onReady(Chunks.Out<byte[]> out) {
                out.write("a".getBytes());
                out.write("b".getBytes());
                out.close();
            }
        };
        chunks.onReady(out);

        testChannel.expect("a".getBytes(), arraysEqual);
        testChannel.expect("b".getBytes(), arraysEqual);
        testChannel.expectEOF();
        testChannel.expectEnd();
        testChannel.expectEmpty();
    }

    @Test
    public void testWhenReadyFactory() throws Exception {
        final CountDownLatch ready = new CountDownLatch(1);

        Chunks<byte[]> chunks = ByteChunks.whenReady(new F.Callback<Chunks.Out<byte[]>>() {
            @Override
            public void invoke(Chunks.Out<byte[]> out) {
                ready.countDown();
            }
        });

        chunks.onReady(null);

        assertTrue("ByteChunks.onReady callback was not invoked", ready.await(1, SECONDS));
    }
}
