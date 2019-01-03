/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import play.Mode;
import play.api.Configuration;
import play.api.http.DefaultFileMimeTypes;
import play.api.http.DefaultFileMimeTypesProvider;
import play.api.http.HttpConfiguration;
import play.mvc.Http.HeaderNames;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResultsTest {

  private static Path file;
  private Http.Context ctx;

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

  @Before
  public void setUpHttpContext() {
    this.ctx = mock(Http.Context.class);
    ThreadLocal<Http.Context> threadLocal = new ThreadLocal<>();
    threadLocal.set(this.ctx);
    Http.Context.current = threadLocal;
  }

  @After
  public void clearHttpContext() {
    Http.Context.current.remove();
  }

  // -- Path tests

  @Test(expected = NullPointerException.class)
  public void shouldThrowNullPointerExceptionIfPathIsNull() throws IOException {
    this.mockRegularFileTypes();
    Results.ok().sendPath(null);
  }

  @Test
  public void sendPathWithOKStatus() throws IOException {
    this.mockRegularFileTypes();
    Result result = Results.ok().sendPath(file);
    assertEquals(result.status(), Http.Status.OK);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"test.tmp\"");
  }

  @Test
  public void sendPathWithUnauthorizedStatus() throws IOException {
    this.mockRegularFileTypes();
    Result result = Results.unauthorized().sendPath(file);
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"test.tmp\"");
  }

  @Test
  public void sendPathAsAttachmentWithUnauthorizedStatus() throws IOException {
    this.mockRegularFileTypes();
    Result result = Results.unauthorized().sendPath(file, /*inline*/ false);
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "attachment; filename=\"test.tmp\"");
  }

  @Test
  public void sendPathAsAttachmentWithOkStatus() throws IOException {
    this.mockRegularFileTypes();
    Result result = Results.ok().sendPath(file, /* inline */ false);
    assertEquals(result.status(), Http.Status.OK);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "attachment; filename=\"test.tmp\"");
  }

  @Test
  public void sendPathWithFileName() throws IOException {
    this.mockRegularFileTypes();
    Result result = Results.unauthorized().sendPath(file, "foo.bar");
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"foo.bar\"");
  }

  @Test
  public void sendPathInlineWithFileName() throws IOException {
    this.mockRegularFileTypes();
    Result result = Results.unauthorized().sendPath(file, true, "foo.bar");
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"foo.bar\"");
  }

  @Test
  public void sendPathWithFileNameHasSpecialChars() throws IOException {
    this.mockRegularFileTypes();
    Result result = Results.ok().sendPath(file, true, "测 试.tmp");
    assertEquals(result.status(), Http.Status.OK);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp");
  }

  // -- File tests

  @Test(expected = NullPointerException.class)
  public void shouldThrowNullPointerExceptionIfFileIsNull() throws IOException {
    this.mockRegularFileTypes();
    Results.ok().sendFile(null);
  }

  @Test
  public void sendFileWithOKStatus() throws IOException {
    this.mockRegularFileTypes();
    Result result = Results.ok().sendFile(file.toFile());
    assertEquals(result.status(), Http.Status.OK);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"test.tmp\"");
  }

  @Test
  public void sendFileWithUnauthorizedStatus() throws IOException {
    this.mockRegularFileTypes();
    Result result = Results.unauthorized().sendFile(file.toFile());
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"test.tmp\"");
  }

  @Test
  public void sendFileAsAttachmentWithUnauthorizedStatus() throws IOException {
    this.mockRegularFileTypes();
    Result result = Results.unauthorized().sendFile(file.toFile(), /* inline */ false);
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "attachment; filename=\"test.tmp\"");
  }

  @Test
  public void sendFileAsAttachmentWithOkStatus() throws IOException {
    this.mockRegularFileTypes();
    Result result = Results.ok().sendFile(file.toFile(), /* inline */ false);
    assertEquals(result.status(), Http.Status.OK);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "attachment; filename=\"test.tmp\"");
  }

  @Test
  public void sendFileWithFileName() throws IOException {
    this.mockRegularFileTypes();
    Result result = Results.unauthorized().sendFile(file.toFile(), "foo.bar");
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"foo.bar\"");
  }

  @Test
  public void sendFileInlineWithFileName() throws IOException {
    this.mockRegularFileTypes();
    Result result = Results.ok().sendFile(file.toFile(), true, "foo.bar");
    assertEquals(result.status(), Http.Status.OK);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"foo.bar\"");
  }

  @Test
  public void sendFileWithFileNameHasSpecialChars() throws IOException {
    this.mockRegularFileTypes();
    Result result = Results.ok().sendFile(file.toFile(), true, "测 试.tmp");
    assertEquals(result.status(), Http.Status.OK);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp");
  }

  @Test
  public void getOptionalCookie() {
    Result result = Results.ok().withCookies(new Http.Cookie("foo", "1", 1000, "/", "example.com", false, true, null));
    assertTrue(result.getCookie("foo").isPresent());
    assertEquals(result.getCookie("foo").get().name(), "foo");
    assertFalse(result.getCookie("bar").isPresent());
  }

  private void mockRegularFileTypes() {
    HttpConfiguration httpConfiguration = new HttpConfiguration.HttpConfigurationProvider(Configuration.reference(), play.api.Environment.simple(new File("."), Mode.TEST.asScala())).get();
    final DefaultFileMimeTypes defaultFileMimeTypes = new DefaultFileMimeTypesProvider(httpConfiguration.fileMimeTypes()).get();
    final FileMimeTypes fileMimeTypes = new FileMimeTypes(defaultFileMimeTypes);
    when(this.ctx.fileMimeTypes()).thenReturn(fileMimeTypes);
  }

}
