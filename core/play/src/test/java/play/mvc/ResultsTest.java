/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import org.junit.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

import play.mvc.Http.HeaderNames;

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
    Result result = Results.unauthorized().sendPath(file, "foo.bar");
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(
        result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"foo.bar\"");
  }

  @Test
  public void sendPathInlineWithFileName() throws IOException {
    Result result = Results.unauthorized().sendPath(file, true, "foo.bar");
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(
        result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"foo.bar\"");
  }

  @Test
  public void sendPathInlineWithoutFileName() throws IOException {
    Result result = Results.unauthorized().sendPath(file, (String) null);
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline");
  }

  @Test
  public void sendPathAsAttachmentWithoutFileName() throws IOException {
    Result result = Results.unauthorized().sendPath(file, false, (String) null);
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "attachment");
  }

  @Test
  public void sendPathWithFileNameHasSpecialChars() throws IOException {
    Result result = Results.ok().sendPath(file, true, "测 试.tmp");
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
    Result result = Results.unauthorized().sendFile(file.toFile(), "foo.bar");
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(
        result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"foo.bar\"");
  }

  @Test
  public void sendFileInlineWithFileName() throws IOException {
    Result result = Results.ok().sendFile(file.toFile(), true, "foo.bar");
    assertEquals(result.status(), Http.Status.OK);
    assertEquals(
        result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"foo.bar\"");
  }

  @Test
  public void sendFileInlineWithoutFileName() throws IOException {
    Result result = Results.ok().sendFile(file.toFile(), (String) null);
    assertEquals(result.status(), Http.Status.OK);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline");
  }

  @Test
  public void sendFileAsAttachmentWithoutFileName() throws IOException {
    Result result = Results.ok().sendFile(file.toFile(), false, (String) null);
    assertEquals(result.status(), Http.Status.OK);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "attachment");
  }

  @Test
  public void sendFileWithFileNameHasSpecialChars() throws IOException {
    Result result = Results.ok().sendFile(file.toFile(), true, "测 试.tmp");
    assertEquals(result.status(), Http.Status.OK);
    assertEquals(
        result.header(HeaderNames.CONTENT_DISPOSITION).get(),
        "inline; filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp");
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
  public void redirectShouldReturnTheSameUrlIfTheQueryStringParamsAreEmpty() {
    Map queryStringParameters = new HashMap<>();
    String url = "/somewhere";
    Result result = Results.redirect(url, queryStringParameters);
    assertTrue(result.redirectLocation().isPresent());
    assertEquals(url, result.redirectLocation().get());
  }

  @Test
  public void redirectShouldAppendGivenQueryStringParamsToTheUrlIfUrlContainsQuestionMark() {
    Map.Entry<String, List<String>> entry1 = Map.entry("param1" , Arrays.asList("value1"));
    Map queryStringParameters = Map.ofEntries(entry1);
    String url = "/somewhere?param2=value2";

    String expectedRedirectUrl = url + "&" + URLEncoder.encode(entry1.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(entry1.getValue().get(0), StandardCharsets.UTF_8);

    Result result = Results.redirect(url, queryStringParameters);
    assertTrue(result.redirectLocation().isPresent());
    assertEquals(expectedRedirectUrl, result.redirectLocation().get());
  }

  @Test
  public void redirectShouldAddQueryStringParamsToTheRedirectUrl() {
    Map.Entry<String, List<String>> entry1 = Map.entry("param1" , Arrays.asList("value1"));
    Map.Entry<String, List<String>> entry2 = Map.entry("param2" , Arrays.asList("value2"));
    Map queryStringParameters = Map.ofEntries(entry1, entry2);
    String url = "/somewhere";

    String expectedParam1Encoded = URLEncoder.encode(entry1.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(entry1.getValue().get(0), StandardCharsets.UTF_8);
    String expectedParam2Encoded = URLEncoder.encode(entry2.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(entry2.getValue().get(0), StandardCharsets.UTF_8);

    Result result = Results.redirect(url, queryStringParameters);
    assertTrue(result.redirectLocation().isPresent());
    assertTrue(result.redirectLocation().get().contains(expectedParam1Encoded));
    assertTrue(result.redirectLocation().get().contains(expectedParam2Encoded));
  }
}
