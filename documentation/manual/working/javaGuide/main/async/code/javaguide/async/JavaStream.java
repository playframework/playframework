/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.async;

import akka.actor.Status;
import akka.util.ByteString;
import akka.stream.javadsl.Source;
import akka.stream.OverflowStrategy;
import akka.NotUsed;

import javaguide.testhelpers.MockJavaAction;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import play.core.j.JavaHandlerComponents;
import play.mvc.Result;
import play.test.WithApplication;

import java.io.*;
import java.util.Optional;

import static javaguide.testhelpers.MockJavaActionHelper.call;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class JavaStream extends WithApplication {

    @Test
    public void byDefault() {
        assertThat(contentAsString(call(new Controller1(instanceOf(JavaHandlerComponents.class)), fakeRequest(), mat)), equalTo("Hello World"));
    }

    public static class Controller1 extends MockJavaAction {

        Controller1(JavaHandlerComponents javaHandlerComponents) {
            super(javaHandlerComponents);
        }

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
        try (OutputStream os = java.nio.file.Files.newOutputStream(file.toPath())) {
            IOUtils.write("hi", os, "UTF-8");
        }
        Result result = call(new Controller2(instanceOf(JavaHandlerComponents.class)), fakeRequest(), mat);
        assertThat(contentAsString(result, mat), equalTo("hi"));
        assertThat(result.body().contentLength(), equalTo(Optional.of(2L)));
        file.delete();
    }

    public static class Controller2 extends MockJavaAction {

        Controller2(JavaHandlerComponents javaHandlerComponents) {
            super(javaHandlerComponents);
        }

        //#serve-file
        public Result index() {
            return ok(new java.io.File("/tmp/fileToServe.pdf"));
        }
        //#serve-file
    }

    @Test
    public void inputStream() {
        String content = contentAsString(call(new Controller3(instanceOf(JavaHandlerComponents.class)), fakeRequest(), mat), mat);
        // Wait until results refactoring is merged, then this will work
        // assertThat(content, containsString("hello"));
    }

    private static InputStream getDynamicStreamSomewhere() {
        return new ByteArrayInputStream("hello".getBytes());
    }

    public static class Controller3 extends MockJavaAction {

        Controller3(JavaHandlerComponents javaHandlerComponents) {
            super(javaHandlerComponents);
        }

        //#input-stream
        public Result index() {
            InputStream is = getDynamicStreamSomewhere();
            return ok(is);
        }
        //#input-stream
    }

    @Test
    public void chunked() {
        String content = contentAsString(call(new Controller4(instanceOf(JavaHandlerComponents.class)), fakeRequest(), mat), mat);
        assertThat(content, equalTo("kikifoobar"));
    }

    public static class Controller4 extends MockJavaAction {

        Controller4(JavaHandlerComponents javaHandlerComponents) {
            super(javaHandlerComponents);
        }

        //#chunked
        public Result index() {
            // Prepare a chunked text stream
            Source<ByteString, ?> source = Source.<ByteString>actorRef(256, OverflowStrategy.dropNew())
                .mapMaterializedValue(sourceActor -> {
                    sourceActor.tell(ByteString.fromString("kiki"), null);
                    sourceActor.tell(ByteString.fromString("foo"), null);
                    sourceActor.tell(ByteString.fromString("bar"), null);
                    sourceActor.tell(new Status.Success(NotUsed.getInstance()), null);
                    return NotUsed.getInstance();
                });
            // Serves this stream with 200 OK
            return ok().chunked(source);
        }
        //#chunked
    }

}
