/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import org.junit.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import play.mvc.Http.HeaderNames;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import static org.junit.Assert.*;

public class ResultsTest {

  private static Path file;

  @BeforeClass
  public static void createFile() throws Exception {
    file = Paths.get("test.tmp");
    Files.createFile(file);
    Files.write(file, "Some content for the file".getBytes(), StandardOpenOption.APPEND);
  }

  @AfterClass
  public static void deleteFile() throws IOException {
    Files.deleteIfExists(file);
  }

  @Test
  public void shouldCopyFlashWhenCallingResultAs() {
    Map<String, String> flash = new HashMap<>();
    flash.put("flash.message", "flash message value");
    Result result = Results.redirect("/somewhere").withFlash(flash);

    Result as = result.as(Http.MimeTypes.HTML);
    assertNotNull(as.flash());
    assertTrue(as.flash().getOptional("flash.message").isPresent());
    assertEquals(as.flash().getOptional("flash.message").get(), "flash message value");
  }

  @Test
  public void shouldCopySessionWhenCallingResultAs() {
    Map<String, String> session = new HashMap<>();
    session.put("session.message", "session message value");
    Result result = Results.ok("Result test body").withSession(session);

    Result as = result.as(Http.MimeTypes.HTML);
    assertNotNull(as.session());
    assertTrue(as.session().getOptional("session.message").isPresent());
    assertEquals(as.session().getOptional("session.message").get(), "session message value");
  }

  @Test
  public void shouldCopyHeadersWhenCallingResultAs() {
    Result result = Results.ok("Result test body").withHeader("X-Header", "header value");
    Result as = result.as(Http.MimeTypes.HTML);
    assertEquals("header value", as.header("X-Header").get());
  }

  @Test
  public void shouldCopyCookiesWhenCallingResultAs() {
    Result result =
        Results.ok("Result test body")
            .withCookies(Http.Cookie.builder("cookie-name", "cookie value").build())
            .as(Http.MimeTypes.HTML);

    assertEquals("cookie value", result.cookie("cookie-name").get().value());
  }

  // -- Path tests

  @Test(expected = NullPointerException.class)
  public void shouldThrowNullPointerExceptionIfPathIsNull() throws IOException {
    Results.ok().sendPath(null);
  }

  @Test
  public void sendPathWithOKStatus() throws IOException {
    Result result = Results.ok().sendPath(file);
    assertEquals(result.status(), Http.Status.OK);
    assertEquals(
        result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"test.tmp\"");
  }

  @Test
  public void sendPathWithUnauthorizedStatus() throws IOException {
    Result result = Results.unauthorized().sendPath(file);
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(
        result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"test.tmp\"");
  }

  @Test
  public void sendPathAsAttachmentWithUnauthorizedStatus() throws IOException {
    Result result = Results.unauthorized().sendPath(file, /*inline*/ false);
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(
        result.header(HeaderNames.CONTENT_DISPOSITION).get(), "attachment; filename=\"test.tmp\"");
  }

  @Test
  public void sendPathAsAttachmentWithOkStatus() throws IOException {
    Result result = Results.ok().sendPath(file, /* inline */ false);
    assertEquals(result.status(), Http.Status.OK);
    assertEquals(
        result.header(HeaderNames.CONTENT_DISPOSITION).get(), "attachment; filename=\"test.tmp\"");
  }

  @Test
  public void sendPathWithFileName() throws IOException {
    Result result = Results.unauthorized().sendPath(file, Optional.of("foo.bar"));
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(
        result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"foo.bar\"");
  }

  @Test
  public void sendPathInlineWithFileName() throws IOException {
    Result result = Results.unauthorized().sendPath(file, true, Optional.of("foo.bar"));
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(
        result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"foo.bar\"");
  }

  @Test
  public void sendPathInlineWithoutFileName() throws IOException {
    Result result = Results.unauthorized().sendPath(file, Optional.empty());
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION), Optional.empty());
  }

  @Test
  public void sendPathAsAttachmentWithoutFileName() throws IOException {
    Result result = Results.unauthorized().sendPath(file, false, Optional.empty());
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "attachment");
  }

  @Test
  public void sendPathWithFileNameHasSpecialChars() throws IOException {
    Result result = Results.ok().sendPath(file, true, Optional.of("测 试.tmp"));
    assertEquals(result.status(), Http.Status.OK);
    assertEquals(
        result.header(HeaderNames.CONTENT_DISPOSITION).get(),
        "inline; filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp");
  }

  // -- File tests

  @Test(expected = NullPointerException.class)
  public void shouldThrowNullPointerExceptionIfFileIsNull() throws IOException {
    Results.ok().sendFile(null);
  }

  @Test
  public void sendFileWithOKStatus() throws IOException {
    Result result = Results.ok().sendFile(file.toFile());
    assertEquals(result.status(), Http.Status.OK);
    assertEquals(
        result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"test.tmp\"");
  }

  @Test
  public void sendFileWithUnauthorizedStatus() throws IOException {
    Result result = Results.unauthorized().sendFile(file.toFile());
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(
        result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"test.tmp\"");
  }

  @Test
  public void sendFileAsAttachmentWithUnauthorizedStatus() throws IOException {
    Result result = Results.unauthorized().sendFile(file.toFile(), /* inline */ false);
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(
        result.header(HeaderNames.CONTENT_DISPOSITION).get(), "attachment; filename=\"test.tmp\"");
  }

  @Test
  public void sendFileAsAttachmentWithOkStatus() throws IOException {
    Result result = Results.ok().sendFile(file.toFile(), /* inline */ false);
    assertEquals(result.status(), Http.Status.OK);
    assertEquals(
        result.header(HeaderNames.CONTENT_DISPOSITION).get(), "attachment; filename=\"test.tmp\"");
  }

  @Test
  public void sendFileWithFileName() throws IOException {
    Result result = Results.unauthorized().sendFile(file.toFile(), Optional.of("foo.bar"));
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(
        result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"foo.bar\"");
  }

  @Test
  public void sendFileInlineWithFileName() throws IOException {
    Result result = Results.ok().sendFile(file.toFile(), true, Optional.of("foo.bar"));
    assertEquals(result.status(), Http.Status.OK);
    assertEquals(
        result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"foo.bar\"");
  }

  @Test
  public void sendFileInlineWithoutFileName() throws IOException {
    Result result = Results.ok().sendFile(file.toFile(), Optional.empty());
    assertEquals(result.status(), Http.Status.OK);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION), Optional.empty());
  }

  @Test
  public void sendFileAsAttachmentWithoutFileName() throws IOException {
    Result result = Results.ok().sendFile(file.toFile(), false, Optional.empty());
    assertEquals(result.status(), Http.Status.OK);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "attachment");
  }

  @Test
  public void sendFileWithFileNameHasSpecialChars() throws IOException {
    Result result = Results.ok().sendFile(file.toFile(), true, Optional.of("测 试.tmp"));
    assertEquals(result.status(), Http.Status.OK);
    assertEquals(
        result.header(HeaderNames.CONTENT_DISPOSITION).get(),
        "inline; filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp");
  }

  @Test
  public void sendFileHonoringOnClose() throws TimeoutException, InterruptedException {
    ActorSystem actorSystem = ActorSystem.create("TestSystem");
    Materializer mat = ActorMaterializer.create(actorSystem);
    try {
      AtomicBoolean fileSent = new AtomicBoolean(false);
      Result result = Results.ok().sendFile(file.toFile(), () -> fileSent.set(true), null);

      // Actually we need to wait until the Stream completes
      Await.ready(
          FutureConverters.toScala(result.body().dataStream().runWith(Sink.ignore(), mat)),
          Duration.create("60s"));
      // and then we need to wait until the onClose completes
      Thread.sleep(500);

      assertTrue(fileSent.get());
      assertEquals(result.status(), Http.Status.OK);
    } finally {
      Await.ready(actorSystem.terminate(), Duration.create("60s"));
    }
  }

  @Test
  public void sendPathHonoringOnClose() throws TimeoutException, InterruptedException {
    ActorSystem actorSystem = ActorSystem.create("TestSystem");
    Materializer mat = ActorMaterializer.create(actorSystem);
    try {
      AtomicBoolean fileSent = new AtomicBoolean(false);
      Result result = Results.ok().sendPath(file, () -> fileSent.set(true), null);

      // Actually we need to wait until the Stream completes
      Await.ready(
          FutureConverters.toScala(result.body().dataStream().runWith(Sink.ignore(), mat)),
          Duration.create("60s"));
      // and then we need to wait until the onClose completes
      Thread.sleep(500);

      assertTrue(fileSent.get());
      assertEquals(result.status(), Http.Status.OK);
    } finally {
      Await.ready(actorSystem.terminate(), Duration.create("60s"));
    }
  }

  @Test
  public void sendResourceHonoringOnClose() throws TimeoutException, InterruptedException {
    ActorSystem actorSystem = ActorSystem.create("TestSystem");
    Materializer mat = ActorMaterializer.create(actorSystem);
    try {
      AtomicBoolean fileSent = new AtomicBoolean(false);
      Result result =
          Results.ok().sendResource("multipart-form-data-file.txt", () -> fileSent.set(true), null);

      // Actually we need to wait until the Stream completes
      Await.ready(
          FutureConverters.toScala(result.body().dataStream().runWith(Sink.ignore(), mat)),
          Duration.create("60s"));
      // and then we need to wait until the onClose completes
      Thread.sleep(500);

      assertTrue(fileSent.get());
      assertEquals(result.status(), Http.Status.OK);
    } finally {
      Await.ready(actorSystem.terminate(), Duration.create("60s"));
    }
  }

  @Test
  public void sendInputStreamHonoringOnClose() throws TimeoutException, InterruptedException {
    ActorSystem actorSystem = ActorSystem.create("TestSystem");
    Materializer mat = ActorMaterializer.create(actorSystem);
    try {
      AtomicBoolean fileSent = new AtomicBoolean(false);
      Result result =
          Results.ok()
              .sendInputStream(
                  new ByteArrayInputStream("test data".getBytes()),
                  9,
                  () -> fileSent.set(true),
                  null);

      // Actually we need to wait until the Stream completes
      Await.ready(
          FutureConverters.toScala(result.body().dataStream().runWith(Sink.ignore(), mat)),
          Duration.create("60s"));
      // and then we need to wait until the onClose completes
      Thread.sleep(500);

      assertTrue(fileSent.get());
      assertEquals(result.status(), Http.Status.OK);
    } finally {
      Await.ready(actorSystem.terminate(), Duration.create("60s"));
    }
  }

  @Test
  public void sendInputStreamChunkedHonoringOnClose()
      throws TimeoutException, InterruptedException {
    ActorSystem actorSystem = ActorSystem.create("TestSystem");
    Materializer mat = ActorMaterializer.create(actorSystem);
    try {
      AtomicBoolean fileSent = new AtomicBoolean(false);
      Result result =
          Results.ok()
              .sendInputStream(
                  new ByteArrayInputStream("test data".getBytes()), () -> fileSent.set(true), null);

      // Actually we need to wait until the Stream completes
      Await.ready(
          FutureConverters.toScala(result.body().dataStream().runWith(Sink.ignore(), mat)),
          Duration.create("60s"));
      // and then we need to wait until the onClose completes
      Thread.sleep(500);

      assertTrue(fileSent.get());
      assertEquals(result.status(), Http.Status.OK);
    } finally {
      Await.ready(actorSystem.terminate(), Duration.create("60s"));
    }
  }

  @Test
  public void getOptionalCookie() {
    Result result =
        Results.ok()
            .withCookies(new Http.Cookie("foo", "1", 1000, "/", "example.com", false, true, null));
    assertTrue(result.cookie("foo").isPresent());
    assertEquals(result.cookie("foo").get().name(), "foo");
    assertFalse(result.cookie("bar").isPresent());
  }

  @Test
  public void redirectShouldReturnTheSameUrlIfTheQueryStringParamsMapIsEmpty() {
    Map queryStringParameters = new HashMap<>();
    String url = "/somewhere";
    Result result = Results.redirect(url, queryStringParameters);
    assertTrue(result.redirectLocation().isPresent());
    assertEquals(url, result.redirectLocation().get());
  }

  @Test
  public void redirectAppendGivenQueryStringParamsToTheUrlIfUrlContainsQuestionMark() {
    Map queryStringParameters = new HashMap<String, List<String>>();
    queryStringParameters.put("param1", Arrays.asList("value1"));
    String url = "/somewhere?param2=value2";

    String expectedRedirectUrl = "/somewhere?param2=value2&param1=value1";

    Result result = Results.redirect(url, queryStringParameters);
    assertTrue(result.redirectLocation().isPresent());
    assertEquals(expectedRedirectUrl, result.redirectLocation().get());
  }

  @Test
  public void redirectShouldAddQueryStringParamsToTheUrl() {
    Map queryStringParameters = new HashMap<String, List<String>>();
    queryStringParameters.put("param1", Arrays.asList("value1"));
    queryStringParameters.put("param2", Arrays.asList("value2"));
    String url = "/somewhere";

    String expectedParam1 = "param1=value1";
    String expectedParam2 = "param2=value2";

    Result result = Results.redirect(url, queryStringParameters);
    assertTrue(result.redirectLocation().isPresent());
    assertTrue(result.redirectLocation().get().contains(expectedParam1));
    assertTrue(result.redirectLocation().get().contains(expectedParam2));
  }
}
