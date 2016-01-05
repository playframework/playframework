/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.async;

import javaguide.testhelpers.MockJavaAction;
import javaguide.testhelpers.MockJavaActionHelper;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import play.mvc.Results.Chunks;
import play.test.WithApplication;

import java.io.*;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class JavaStream extends WithApplication {

    @Test
    public void byDefault() {
        assertThat(contentAsString(MockJavaActionHelper.call(new Controller1(), fakeRequest(), mat)), equalTo("Hello World"));
    }

    public static class Controller1 extends MockJavaAction {
        //#by-default
        public Result index() {
            return ok("Hello World");
        }
        //#by-default
    }

    @Test
    public void serveFile() throws Exception {
        File file = new File("/tmp/fileToServe.pdf");
        file.deleteOnExit();
        try (OutputStream os = new FileOutputStream(file)) {
            IOUtils.write("hi", os);
        }
        Result result = MockJavaActionHelper.call(new Controller2(), fakeRequest(), mat);
        assertThat(contentAsString(result, mat), equalTo("hi"));
        assertThat(result.body().contentLength(), equalTo(Optional.of(2L)));
        file.delete();
    }

    public static class Controller2 extends MockJavaAction {
        //#serve-file
        public Result index() {
            return ok(new java.io.File("/tmp/fileToServe.pdf"));
        }
        //#serve-file
    }

    @Test
    public void inputStream() {
        String content = contentAsString(MockJavaActionHelper.call(new Controller3(), fakeRequest(), mat), mat);
        // Wait until results refactoring is merged, then this will work
        // assertThat(content, containsString("hello"));
    }

    private static InputStream getDynamicStreamSomewhere() {
        return new ByteArrayInputStream("hello".getBytes());
    }

    public static class Controller3 extends MockJavaAction {
        //#input-stream
        public Result index() {
            InputStream is = getDynamicStreamSomewhere();
            return ok(is);
        }
        //#input-stream
    }

    @Test
    public void chunked() {
        String content = contentAsString(MockJavaActionHelper.call(new Controller4(), fakeRequest(), mat), mat);
        assertThat(content, equalTo("kikifoobar"));
    }

    public static class Controller4 extends MockJavaAction {
        //#chunked
        public Result index() {
            // Prepare a chunked text stream
            Chunks<String> chunks = StringChunks.whenReady(
                    JavaStream::registerOutChannelSomewhere
            );

            // Serves this stream with 200 OK
            return ok(chunks);
        }
        //#chunked
    }

    //#register-out-channel
    public static void registerOutChannelSomewhere(Chunks.Out<String> out) {
        out.write("kiki");
        out.write("foo");
        out.write("bar");
        out.close();
    }
    //#register-out-channel

}
