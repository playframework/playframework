/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.async;

import static javaguide.testhelpers.MockJavaActionHelper.call;
import static org.junit.jupiter.api.Assertions.*;
import static play.test.Helpers.*;

import akka.NotUsed;
import akka.actor.Status;
import akka.stream.Materializer;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import java.io.*;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Optional;
import javaguide.testhelpers.MockJavaAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import play.Application;
import play.core.j.JavaHandlerComponents;
import play.http.HttpEntity;
import play.mvc.ResponseHeader;
import play.mvc.Result;
import play.test.junit5.ApplicationExtension;

public class JavaStream {

  @RegisterExtension
  static ApplicationExtension appExtension = new ApplicationExtension(fakeApplication());

  static Application app = appExtension.getApplication();
  static Materializer mat = appExtension.getMaterializer();

  @Test
  void byDefault() {
    assertEquals(
        "Hello World",
        contentAsString(
            call(
                new Controller1(app.injector().instanceOf(JavaHandlerComponents.class)),
                fakeRequest(),
                mat)));
  }

  public static class Controller1 extends MockJavaAction {

    Controller1(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    // #by-default
    public Result index() {
      return ok("Hello World");
    }
    // #by-default
  }

  @Test
  void byDefaultWithHttpEntity() {
    assertEquals(
        "Hello World",
        contentAsString(
            call(
                new ControllerWithHttpEntity(
                    app.injector().instanceOf(JavaHandlerComponents.class)),
                fakeRequest(),
                mat)));
  }

  public static class ControllerWithHttpEntity extends MockJavaAction {

    ControllerWithHttpEntity(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    // #by-default-http-entity
    public Result index() {
      return new Result(
          new ResponseHeader(200, Collections.emptyMap()),
          new HttpEntity.Strict(ByteString.fromString("Hello World"), Optional.of("text/plain")));
    }
    // #by-default-http-entity
  }

  public static class ControllerStreamingFile extends MockJavaAction {

    ControllerStreamingFile(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    private void createSourceFromFile() {
      // #create-source-from-file
      java.io.File file = new java.io.File("/tmp/fileToServe.pdf");
      java.nio.file.Path path = file.toPath();
      Source<ByteString, ?> source = FileIO.fromPath(path);
      // #create-source-from-file
    }

    // #streaming-http-entity
    public Result index() {
      java.io.File file = new java.io.File("/tmp/fileToServe.pdf");
      java.nio.file.Path path = file.toPath();
      Source<ByteString, ?> source = FileIO.fromPath(path);

      return new Result(
          new ResponseHeader(200, Collections.emptyMap()),
          new HttpEntity.Streamed(source, Optional.empty(), Optional.of("text/plain")));
    }
    // #streaming-http-entity
  }

  public static class ControllerStreamingFileWithContentLength extends MockJavaAction {

    ControllerStreamingFileWithContentLength(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    // #streaming-http-entity-with-content-length
    public Result index() {
      java.io.File file = new java.io.File("/tmp/fileToServe.pdf");
      java.nio.file.Path path = file.toPath();
      Source<ByteString, ?> source = FileIO.fromPath(path);

      Optional<Long> contentLength = null;
      try {
        contentLength = Optional.of(Files.size(path));
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }

      return new Result(
          new ResponseHeader(200, Collections.emptyMap()),
          new HttpEntity.Streamed(source, contentLength, Optional.of("text/plain")));
    }
    // #streaming-http-entity-with-content-length
  }

  @Test
  void serveFile() throws Exception {
    File file = new File("/tmp/fileToServe.pdf");
    file.deleteOnExit();
    try (OutputStream os = java.nio.file.Files.newOutputStream(file.toPath())) {
      os.write("hi".getBytes("UTF-8"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    Result result =
        call(
            new Controller2(app.injector().instanceOf(JavaHandlerComponents.class)),
            fakeRequest(),
            mat);
    assertEquals("hi", contentAsString(result, mat));
    assertEquals(Optional.of(2L), result.body().contentLength());
    file.delete();
  }

  public static class Controller2 extends MockJavaAction {

    Controller2(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    // #serve-file
    public Result index() {
      return ok(new java.io.File("/tmp/fileToServe.pdf"));
    }
    // #serve-file
  }

  public static class ControllerServeFileWithName extends MockJavaAction {

    ControllerServeFileWithName(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    // #serve-file-with-name
    public Result index() {
      return ok(new java.io.File("/tmp/fileToServe.pdf"), Optional.of("fileToServe.pdf"));
    }
    // #serve-file-with-name
  }

  public static class ControllerServeAttachment extends MockJavaAction {

    ControllerServeAttachment(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    // #serve-file-attachment
    public Result index() {
      return ok(new java.io.File("/tmp/fileToServe.pdf"), /*inline = */ false);
    }
    // #serve-file-attachment
  }

  @Test
  void inputStream() {
    String content =
        contentAsString(
            call(
                new Controller3(app.injector().instanceOf(JavaHandlerComponents.class)),
                fakeRequest(),
                mat),
            mat);
    assertTrue(content.contains("hello"));
  }

  private static InputStream getDynamicStreamSomewhere() {
    return new ByteArrayInputStream("hello".getBytes());
  }

  public static class Controller3 extends MockJavaAction {

    Controller3(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    // #input-stream
    public Result index() {
      InputStream is = getDynamicStreamSomewhere();
      return ok(is);
    }
    // #input-stream
  }

  @Test
  void chunked() {
    String content =
        contentAsString(
            call(
                new Controller4(app.injector().instanceOf(JavaHandlerComponents.class)),
                fakeRequest(),
                mat),
            mat);
    assertEquals("kikifoobar", content);
  }

  public static class Controller4 extends MockJavaAction {

    Controller4(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    // #chunked
    public Result index() {
      // Prepare a chunked text stream
      Source<ByteString, ?> source =
          Source.<ByteString>actorRef(256, OverflowStrategy.dropNew())
              .mapMaterializedValue(
                  sourceActor -> {
                    sourceActor.tell(ByteString.fromString("kiki"), null);
                    sourceActor.tell(ByteString.fromString("foo"), null);
                    sourceActor.tell(ByteString.fromString("bar"), null);
                    sourceActor.tell(new Status.Success(NotUsed.getInstance()), null);
                    return NotUsed.getInstance();
                  });
      // Serves this stream with 200 OK
      return ok().chunked(source);
    }
    // #chunked
  }
}
