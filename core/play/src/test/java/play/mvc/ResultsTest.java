/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import static org.junit.jupiter.api.Assertions.*;

import akka.actor.ActorSystem;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import play.mvc.Http.HeaderNames;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.jdk.javaapi.FutureConverters;

class ResultsTest {

  private static Path file;
  private static final boolean INLINE_FILE = true;
  private static final boolean ATTACHMENT_FILE = false;

  @BeforeAll
  static void createFile() throws Exception {
    file = Paths.get("test.tmp");
    Files.createFile(file);
    Files.write(file, "Some content for the file".getBytes(), StandardOpenOption.APPEND);
  }

  @AfterAll
  static void deleteFile() throws IOException {
    Files.deleteIfExists(file);
  }

  @Test
  void shouldCopyFlashWhenCallingResultAs() {
    Map<String, String> flash = new HashMap<>();
    flash.put("flash.message", "flash message value");
    Result result = Results.redirect("/somewhere").withFlash(flash);

    Result as = result.as(Http.MimeTypes.HTML);
    assertNotNull(as.flash());
    assertTrue(as.flash().get("flash.message").isPresent());
    assertEquals("flash message value", as.flash().get("flash.message").get());
  }

  @Test
  void shouldCopySessionWhenCallingResultAs() {
    Map<String, String> session = new HashMap<>();
    session.put("session.message", "session message value");
    Result result = Results.ok("Result test body").withSession(session);

    Result as = result.as(Http.MimeTypes.HTML);
    assertNotNull(as.session());
    assertTrue(as.session().get("session.message").isPresent());
    assertEquals("session message value", as.session().get("session.message").get());
  }

  @Test
  void shouldCopyHeadersWhenCallingResultAs() {
    Result result = Results.ok("Result test body").withHeader("X-Header", "header value");
    Result as = result.as(Http.MimeTypes.HTML);
    assertEquals("header value", as.header("X-Header").get());
  }

  @Test
  void shouldCopyCookiesWhenCallingResultAs() {
    Result result =
        Results.ok("Result test body")
            .withCookies(Http.Cookie.builder("cookie-name", "cookie value").build())
            .as(Http.MimeTypes.HTML);

    assertEquals("cookie value", result.cookie("cookie-name").get().value());
  }

  // -- Path tests

  @Test()
  void shouldThrowNullPointerExceptionIfPathIsNull() {
    assertThrowsExactly(NullPointerException.class, () -> Results.ok().sendPath(null));
  }

  @Test
  void sendPathWithOKStatus() {
    Result result = Results.ok().sendPath(file);
    assertEquals(Http.Status.OK, result.status());
    assertEquals(
        "inline; filename=\"test.tmp\"", result.header(HeaderNames.CONTENT_DISPOSITION).get());
  }

  @Test
  void sendPathWithUnauthorizedStatus() {
    Result result = Results.unauthorized().sendPath(file);
    assertEquals(Http.Status.UNAUTHORIZED, result.status());
    assertEquals(
        "inline; filename=\"test.tmp\"", result.header(HeaderNames.CONTENT_DISPOSITION).get());
  }

  @Test
  void sendPathAsAttachmentWithUnauthorizedStatus() {
    Result result = Results.unauthorized().sendPath(file, ATTACHMENT_FILE);
    assertEquals(Http.Status.UNAUTHORIZED, result.status());
    assertEquals(
        "attachment; filename=\"test.tmp\"", result.header(HeaderNames.CONTENT_DISPOSITION).get());
  }

  @Test
  void sendPathAsAttachmentWithOkStatus() {
    Result result = Results.ok().sendPath(file, ATTACHMENT_FILE);
    assertEquals(Http.Status.OK, result.status());
    assertEquals(
        "attachment; filename=\"test.tmp\"", result.header(HeaderNames.CONTENT_DISPOSITION).get());
  }

  @Test
  void sendPathWithFileName() {
    Result result = Results.unauthorized().sendPath(file, Optional.of("foo.bar"));
    assertEquals(Http.Status.UNAUTHORIZED, result.status());
    assertEquals(
        "inline; filename=\"foo.bar\"", result.header(HeaderNames.CONTENT_DISPOSITION).get());
  }

  @Test
  void sendPathInlineWithFileName() {
    Result result = Results.unauthorized().sendPath(file, INLINE_FILE, Optional.of("foo.bar"));
    assertEquals(Http.Status.UNAUTHORIZED, result.status());
    assertEquals(
        "inline; filename=\"foo.bar\"", result.header(HeaderNames.CONTENT_DISPOSITION).get());
  }

  @Test
  void sendPathInlineWithoutFileName() {
    Result result = Results.unauthorized().sendPath(file, Optional.empty());
    assertEquals(Http.Status.UNAUTHORIZED, result.status());
    assertEquals(Optional.empty(), result.header(HeaderNames.CONTENT_DISPOSITION));
  }

  @Test
  void sendPathAsAttachmentWithoutFileName() {
    Result result = Results.unauthorized().sendPath(file, ATTACHMENT_FILE, Optional.empty());
    assertEquals(Http.Status.UNAUTHORIZED, result.status());
    assertEquals("attachment", result.header(HeaderNames.CONTENT_DISPOSITION).get());
  }

  @Test
  void sendPathWithFileNameHasSpecialChars() {
    Result result = Results.ok().sendPath(file, INLINE_FILE, Optional.of("测 试.tmp"));
    assertEquals(Http.Status.OK, result.status());
    assertEquals(
        "inline; filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp",
        result.header(HeaderNames.CONTENT_DISPOSITION).get());
  }

  // -- File tests

  @Test
  void shouldThrowNullPointerExceptionIfFileIsNull() {
    Assertions.assertThrowsExactly(NullPointerException.class, () -> Results.ok().sendFile(null));
  }

  @Test
  void sendFileWithOKStatus() {
    Result result = Results.ok().sendFile(file.toFile());
    assertEquals(Http.Status.OK, result.status());
    assertEquals(
        "inline; filename=\"test.tmp\"", result.header(HeaderNames.CONTENT_DISPOSITION).get());
  }

  @Test
  void sendFileWithUnauthorizedStatus() {
    Result result = Results.unauthorized().sendFile(file.toFile());
    assertEquals(Http.Status.UNAUTHORIZED, result.status());
    assertEquals(
        "inline; filename=\"test.tmp\"", result.header(HeaderNames.CONTENT_DISPOSITION).get());
  }

  @Test
  void sendFileAsAttachmentWithUnauthorizedStatus() {
    Result result = Results.unauthorized().sendFile(file.toFile(), ATTACHMENT_FILE);
    assertEquals(Http.Status.UNAUTHORIZED, result.status());
    assertEquals(
        "attachment; filename=\"test.tmp\"", result.header(HeaderNames.CONTENT_DISPOSITION).get());
  }

  @Test
  void sendFileAsAttachmentWithOkStatus() {
    Result result = Results.ok().sendFile(file.toFile(), ATTACHMENT_FILE);
    assertEquals(Http.Status.OK, result.status());
    assertEquals(
        "attachment; filename=\"test.tmp\"", result.header(HeaderNames.CONTENT_DISPOSITION).get());
  }

  @Test
  void sendFileWithFileName() {
    Result result = Results.unauthorized().sendFile(file.toFile(), Optional.of("foo.bar"));
    assertEquals(Http.Status.UNAUTHORIZED, result.status());
    assertEquals(
        "inline; filename=\"foo.bar\"", result.header(HeaderNames.CONTENT_DISPOSITION).get());
  }

  @Test
  void sendFileInlineWithFileName() {
    Result result = Results.ok().sendFile(file.toFile(), INLINE_FILE, Optional.of("foo.bar"));
    assertEquals(Http.Status.OK, result.status());
    assertEquals(
        "inline; filename=\"foo.bar\"", result.header(HeaderNames.CONTENT_DISPOSITION).get());
  }

  @Test
  void sendFileInlineWithoutFileName() {
    Result result = Results.ok().sendFile(file.toFile(), Optional.empty());
    assertEquals(Http.Status.OK, result.status());
    assertEquals(Optional.empty(), result.header(HeaderNames.CONTENT_DISPOSITION));
  }

  @Test
  void sendFileAsAttachmentWithoutFileName() {
    Result result = Results.ok().sendFile(file.toFile(), ATTACHMENT_FILE, Optional.empty());
    assertEquals(Http.Status.OK, result.status());
    assertEquals("attachment", result.header(HeaderNames.CONTENT_DISPOSITION).get());
  }

  @Test
  void sendFileWithFileNameHasSpecialChars() {
    Result result = Results.ok().sendFile(file.toFile(), INLINE_FILE, Optional.of("测 试.tmp"));
    assertEquals(Http.Status.OK, result.status());
    assertEquals(
        "inline; filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp",
        result.header(HeaderNames.CONTENT_DISPOSITION).get());
  }

  @Test
  void sendFileHonoringOnClose() throws TimeoutException, InterruptedException {
    ActorSystem actorSystem = ActorSystem.create("TestSystem");
    Materializer mat = Materializer.matFromSystem(actorSystem);
    try {
      AtomicBoolean fileSent = new AtomicBoolean(false);
      Result result = Results.ok().sendFile(file.toFile(), () -> fileSent.set(true), null);

      // Actually we need to wait until the Stream completes
      Await.ready(
          FutureConverters.asScala(result.body().dataStream().runWith(Sink.ignore(), mat)),
          Duration.create("60s"));
      // and then we need to wait until the onClose completes
      Thread.sleep(500);

      assertTrue(fileSent.get());
      assertEquals(Http.Status.OK, result.status());
    } finally {
      Await.ready(actorSystem.terminate(), Duration.create("60s"));
    }
  }

  @Test
  void sendPathHonoringOnClose() throws TimeoutException, InterruptedException {
    ActorSystem actorSystem = ActorSystem.create("TestSystem");
    Materializer mat = Materializer.matFromSystem(actorSystem);
    try {
      AtomicBoolean fileSent = new AtomicBoolean(false);
      Result result = Results.ok().sendPath(file, () -> fileSent.set(true), null);

      // Actually we need to wait until the Stream completes
      Await.ready(
          FutureConverters.asScala(result.body().dataStream().runWith(Sink.ignore(), mat)),
          Duration.create("60s"));
      // and then we need to wait until the onClose completes
      Thread.sleep(500);

      assertTrue(fileSent.get());
      assertEquals(Http.Status.OK, result.status());
    } finally {
      Await.ready(actorSystem.terminate(), Duration.create("60s"));
    }
  }

  @Test
  void sendResourceHonoringOnClose() throws TimeoutException, InterruptedException {
    ActorSystem actorSystem = ActorSystem.create("TestSystem");
    Materializer mat = Materializer.matFromSystem(actorSystem);
    try {
      AtomicBoolean fileSent = new AtomicBoolean(false);
      Result result =
          Results.ok().sendResource("multipart-form-data-file.txt", () -> fileSent.set(true), null);

      // Actually we need to wait until the Stream completes
      Await.ready(
          FutureConverters.asScala(result.body().dataStream().runWith(Sink.ignore(), mat)),
          Duration.create("60s"));
      // and then we need to wait until the onClose completes
      Thread.sleep(500);

      assertTrue(fileSent.get());
      assertEquals(Http.Status.OK, result.status());
    } finally {
      Await.ready(actorSystem.terminate(), Duration.create("60s"));
    }
  }

  @Test
  void sendInputStreamHonoringOnClose() throws TimeoutException, InterruptedException {
    ActorSystem actorSystem = ActorSystem.create("TestSystem");
    Materializer mat = Materializer.matFromSystem(actorSystem);
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
          FutureConverters.asScala(result.body().dataStream().runWith(Sink.ignore(), mat)),
          Duration.create("60s"));
      // and then we need to wait until the onClose completes
      Thread.sleep(500);

      assertTrue(fileSent.get());
      assertEquals(Http.Status.OK, result.status());
    } finally {
      Await.ready(actorSystem.terminate(), Duration.create("60s"));
    }
  }

  @Test
  void sendInputStreamChunkedHonoringOnClose() throws TimeoutException, InterruptedException {
    ActorSystem actorSystem = ActorSystem.create("TestSystem");
    Materializer mat = Materializer.matFromSystem(actorSystem);
    try {
      AtomicBoolean fileSent = new AtomicBoolean(false);
      Result result =
          Results.ok()
              .sendInputStream(
                  new ByteArrayInputStream("test data".getBytes()), () -> fileSent.set(true), null);

      // Actually we need to wait until the Stream completes
      Await.ready(
          FutureConverters.asScala(result.body().dataStream().runWith(Sink.ignore(), mat)),
          Duration.create("60s"));
      // and then we need to wait until the onClose completes
      Thread.sleep(500);

      assertTrue(fileSent.get());
      assertEquals(Http.Status.OK, result.status());
    } finally {
      Await.ready(actorSystem.terminate(), Duration.create("60s"));
    }
  }

  @Test
  void getOptionalCookie() {
    Result result =
        Results.ok()
            .withCookies(new Http.Cookie("foo", "1", 1000, "/", "example.com", false, true, null));
    assertTrue(result.cookie("foo").isPresent());
    assertEquals("foo", result.cookie("foo").get().name());
    assertFalse(result.cookie("bar").isPresent());
  }

  @Test
  void redirectShouldReturnTheSameUrlIfTheQueryStringParamsMapIsEmpty() {
    Map<String, List<String>> queryStringParameters = new HashMap<>();
    String url = "/somewhere";
    Result result = Results.redirect(url, queryStringParameters);
    assertTrue(result.redirectLocation().isPresent());
    assertEquals(url, result.redirectLocation().get());
  }

  @Test
  void redirectAppendGivenQueryStringParamsToTheUrlIfUrlContainsQuestionMark() {
    Map<String, List<String>> queryStringParameters = new HashMap<>();
    queryStringParameters.put("param1", Arrays.asList("value1"));
    String url = "/somewhere?param2=value2";

    String expectedRedirectUrl = "/somewhere?param2=value2&param1=value1";

    Result result = Results.redirect(url, queryStringParameters);
    assertTrue(result.redirectLocation().isPresent());
    assertEquals(expectedRedirectUrl, result.redirectLocation().get());
  }

  @Test
  void redirectShouldAddQueryStringParamsToTheUrl() {
    Map<String, List<String>> queryStringParameters = new HashMap<>();
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
