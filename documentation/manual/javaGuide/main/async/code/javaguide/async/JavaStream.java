/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.async;

import javaguide.testhelpers.MockJavaAction;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import play.mvc.Results.Chunks;
import play.test.WithApplication;

import java.io.*;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class JavaStream extends WithApplication {

    @Test
    public void byDefault() {
        assertThat(contentAsString(MockJavaAction.call(new Controller1(), fakeRequest())), equalTo("Hello World"));
    }

    public static class Controller1 extends MockJavaAction {
        //#by-default
        public static Result index() {
            return ok("Hello World");
        }
        //#by-default
    }

    @Test
    public void serveFile() throws Exception {
        File file = new File("/tmp/fileToServe.pdf");
        file.deleteOnExit();
        OutputStream os = new FileOutputStream(file);
        try {
            IOUtils.write("hi", os);
        } finally {
            os.close();
        }
        Result result = MockJavaAction.call(new Controller2(), fakeRequest());
        assertThat(contentAsString(result), equalTo("hi"));
        assertThat(header(CONTENT_LENGTH, result), equalTo("2"));
        file.delete();
    }

    public static class Controller2 extends MockJavaAction {
        //#serve-file
        public static Result index() {
            return ok(new java.io.File("/tmp/fileToServe.pdf"));
        }
        //#serve-file
    }

    @Test
    public void inputStream() {
        String content = contentAsString(MockJavaAction.call(new Controller3(), fakeRequest()));
        // Wait until results refactoring is merged, then this will work
        // assertThat(content, containsString("hello"));
    }

    private static InputStream getDynamicStreamSomewhere() {
        return new ByteArrayInputStream("hello".getBytes());
    }

    public static class Controller3 extends MockJavaAction {
        //#input-stream
        public static Result index() {
            InputStream is = getDynamicStreamSomewhere();
            return ok(is);
        }
        //#input-stream
    }

    @Test
    public void chunked() {
        String content = contentAsString(MockJavaAction.call(new Controller4(), fakeRequest()));
        // Wait until results refactoring is merged, then this will work
        // assertThat(content, containsString("kikifoobar"));
    }

    public static class Controller4 extends MockJavaAction {
        //#chunked
        public static Result index() {
            // Prepare a chunked text stream
            Chunks<String> chunks = new StringChunks() {

                // Called when the stream is ready
                public void onReady(Chunks.Out<String> out) {
                    registerOutChannelSomewhere(out);
                }

            };

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