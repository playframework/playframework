/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.mvc;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import org.junit.Test;
import play.api.libs.iteratee.TestChannel;
import play.libs.F;
import play.mvc.Results.Chunks;
import play.mvc.Results.StringChunks;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;

public class StringChunksTest {

    @Test
    public void testStringChunks() throws Throwable {
        final TestChannel<String> testChannel = new TestChannel<String>();
        final Chunks.Out<String> out = new Chunks.Out<String>(testChannel, new ArrayList<F.Callback0>());
        Chunks<String> chunks = new StringChunks() {
            @Override
            public void onReady(Chunks.Out<String> out) {
                out.write("a");
                out.write("b");
                out.close();
            }
        };
        chunks.onReady(out);

        testChannel.expect("a");
        testChannel.expect("b");
        testChannel.expectEOF();
        testChannel.expectEnd();
        testChannel.expectEmpty();
    }

    @Test
    public void testWhenReadyFactory() throws Exception {
        final CountDownLatch ready = new CountDownLatch(1);

        Chunks<String> chunks = StringChunks.whenReady(new F.Callback<Chunks.Out<String>>() {
            @Override
            public void invoke(Chunks.Out<String> out) {
                ready.countDown();
            }
        });

        chunks.onReady(null);

        assertTrue("StringChunks.onReady callback was not invoked", ready.await(1, SECONDS));
    }
}
