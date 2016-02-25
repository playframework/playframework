/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.async;

import akka.actor.Status;
import akka.util.ByteString;
import akka.stream.javadsl.Source;
import akka.stream.OverflowStrategy;
import akka.NotUsed;

import javaguide.testhelpers.MockJavaAction;
import javaguide.testhelpers.MockJavaActionHelper;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
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
            Source<ByteString, ?> source = Source.<ByteString>actorRef(256, OverflowStrategy.dropNew())
                .mapMaterializedValue(sourceActor -> {
                    sourceActor.tell(ByteString.fromString("kiki"), null);
                    sourceActor.tell(ByteString.fromString("foo"), null);
                    sourceActor.tell(ByteString.fromString("bar"), null);
                    sourceActor.tell(new Status.Success(NotUsed.getInstance()), null);
                    return null;
                });
            // Serves this stream with 200 OK
            return ok().chunked(source);
        }
        //#chunked
    }

}
