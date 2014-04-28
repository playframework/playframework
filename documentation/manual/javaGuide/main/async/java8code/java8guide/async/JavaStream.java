/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package java8guide.async;

import javaguide.testhelpers.MockJavaAction;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import play.mvc.Results.Chunks;
import play.mvc.Results.StringChunks;
import play.test.WithApplication;

import java.io.*;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class JavaStream extends WithApplication {

    @Test
    public void chunked() {
        String content = contentAsString(MockJavaAction.call(new Controller1(), fakeRequest()));
        assertThat(content, equalTo(
            "4\r\n" +
            "kiki\r\n" +
            "3\r\n" +
            "foo\r\n" +
            "3\r\n" +
            "bar\r\n" +
            "0\r\n\r\n"
        ));
    }

    public static class Controller1 extends MockJavaAction {
        //#chunked
        public static Result index() {
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
