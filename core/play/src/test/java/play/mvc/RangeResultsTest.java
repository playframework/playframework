/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static play.mvc.Http.HeaderNames.*;
import static play.mvc.Http.MimeTypes.*;
import static play.mvc.Http.Status.*;

import akka.stream.IOResult;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import org.junit.*;
import play.api.http.DefaultFileMimeTypes;
import play.api.http.FileMimeTypesConfiguration;
import play.libs.Scala;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class RangeResultsTest {

  private static Path path;
  private Http.Context ctx;

  @BeforeClass
  public static void createFile() throws IOException {
    path = Paths.get("test.tmp");
    Files.createFile(path);
    Files.write(path, "Some content for the file".getBytes(), StandardOpenOption.APPEND);
  }

  @AfterClass
  public static void deleteFile() throws IOException {
    Files.deleteIfExists(path);
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

  // -- InputStreams

  @Test
  public void shouldNotReturnRangeResultForInputStreamWhenHeaderIsNotPresent() throws IOException {
    Http.Request req = mockRegularRequest();
    try (InputStream stream = Files.newInputStream(path)) {
      Result result = RangeResults.ofStream(req, stream);
      assertEquals(result.status(), OK);
      assertEquals(BINARY, result.body().contentType().orElse(""));
    }
  }

  @Test
  public void shouldReturnRangeResultForInputStreamWhenHeaderIsPresentAndContentTypeWasSpecified()
      throws IOException {
    Http.Request req = mockRangeRequest();
    try (InputStream stream = Files.newInputStream(path)) {
      Result result = RangeResults.ofStream(req, stream, Files.size(path), "file.txt", HTML);
      assertEquals(result.status(), PARTIAL_CONTENT);
      assertEquals(HTML, result.body().contentType().orElse(""));
    }
  }

  @Test
  public void shouldReturnRangeResultForInputStreamWithCustomFilename() throws IOException {
    Http.Request req = mockRangeRequest();
    try (InputStream stream = Files.newInputStream(path)) {
      Result result = RangeResults.ofStream(req, stream, Files.size(path), "file.txt");
      assertEquals(result.status(), PARTIAL_CONTENT);
      assertEquals(
          "attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
    }
  }

  @Test
  public void shouldNotReturnRangeResultForInputStreamWhenHeaderIsNotPresentWithCustomFilename()
      throws IOException {
    Http.Request req = mockRegularRequest();
    try (InputStream stream = Files.newInputStream(path)) {
      Result result = RangeResults.ofStream(req, stream, Files.size(path), "file.txt");
      assertEquals(result.status(), OK);
      assertEquals(BINARY, result.body().contentType().orElse(""));
      assertEquals(
          "attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
    }
  }

  @Test
  public void shouldReturnPartialContentForInputStreamWithGivenEntityLength() throws IOException {
    Http.Request req = mockRangeRequest();
    try (InputStream stream = Files.newInputStream(path)) {
      Result result = RangeResults.ofStream(req, stream, Files.size(path));
      assertEquals(result.status(), PARTIAL_CONTENT);
      assertEquals(result.header(CONTENT_RANGE).get(), "bytes 0-1/" + Files.size(path));
    }
  }

  @Test
  public void shouldReturnPartialContentForInputStreamWithGivenNameAndContentType()
      throws IOException {
    Http.Request req = mockRangeRequest();
    try (InputStream stream = Files.newInputStream(path)) {
      Result result = RangeResults.ofStream(req, stream, Files.size(path), "file.txt", TEXT);
      assertEquals(result.status(), PARTIAL_CONTENT);
      assertEquals(TEXT, result.body().contentType().orElse(""));
      assertEquals(
          "attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
    }
  }

  // -- Paths

  @Test
  public void shouldReturnRangeResultForPath() {
    Http.Request req = mockRangeRequest();
    Result result = RangeResults.ofPath(req, path);

    assertEquals(result.status(), PARTIAL_CONTENT);
    assertEquals(
        "attachment; filename=\"test.tmp\"", result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  public void shouldNotReturnRangeResultForPathWhenHeaderIsNotPresent() {
    Http.Request req = mockRegularRequest();

    Result result = RangeResults.ofPath(req, path);

    assertEquals(result.status(), OK);
    assertEquals(
        "attachment; filename=\"test.tmp\"", result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  public void shouldReturnRangeResultForPathWithCustomFilename() {
    Http.Request req = mockRangeRequest();
    Result result = RangeResults.ofPath(req, path, "file.txt");

    assertEquals(result.status(), PARTIAL_CONTENT);
    assertEquals(
        "attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  public void shouldNotReturnRangeResultForPathWhenHeaderIsNotPresentWithCustomFilename() {
    Http.Request req = mockRegularRequest();

    Result result = RangeResults.ofPath(req, path, "file.txt");

    assertEquals(result.status(), OK);
    assertEquals(
        "attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  public void shouldReturnRangeResultForPathWhenFilenameHasSpecialChars() {
    Http.Request req = mockRangeRequest();

    Result result = RangeResults.ofPath(req, path, "测 试.tmp");

    assertEquals(result.status(), PARTIAL_CONTENT);
    assertEquals(
        "attachment; filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp",
        result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  public void shouldNotReturnRangeResultForPathWhenFilenameHasSpecialChars() {
    Http.Request req = mockRegularRequest();

    Result result = RangeResults.ofPath(req, path, "测 试.tmp");

    assertEquals(result.status(), OK);
    assertEquals(
        "attachment; filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp",
        result.header(CONTENT_DISPOSITION).orElse(""));
  }

  // -- Files

  @Test
  public void shouldReturnRangeResultForFile() {
    Http.Request req = mockRangeRequest();
    Result result = RangeResults.ofFile(req, path.toFile());

    assertEquals(result.status(), PARTIAL_CONTENT);
    assertEquals(
        "attachment; filename=\"test.tmp\"", result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  public void shouldNotReturnRangeResultForFileWhenHeaderIsNotPresent() {
    Http.Request req = mockRegularRequest();

    Result result = RangeResults.ofFile(req, path.toFile());

    assertEquals(result.status(), OK);
    assertEquals(
        "attachment; filename=\"test.tmp\"", result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  public void shouldReturnRangeResultForFileWithCustomFilename() {
    Http.Request req = mockRangeRequest();
    Result result = RangeResults.ofFile(req, path.toFile(), "file.txt");

    assertEquals(result.status(), PARTIAL_CONTENT);
    assertEquals(
        "attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  public void shouldNotReturnRangeResultForFileWhenHeaderIsNotPresentWithCustomFilename() {
    Http.Request req = mockRegularRequest();

    Result result = RangeResults.ofFile(req, path.toFile(), "file.txt");

    assertEquals(result.status(), OK);
    assertEquals(
        "attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  public void shouldReturnRangeResultForFileWhenFilenameHasSpecialChars() {
    Http.Request req = mockRangeRequest();

    Result result = RangeResults.ofFile(req, path.toFile(), "测 试.tmp");

    assertEquals(result.status(), PARTIAL_CONTENT);
    assertEquals(
        "attachment; filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp",
        result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  public void shouldNotReturnRangeResultForFileWhenFilenameHasSpecialChars() {
    Http.Request req = mockRegularRequest();

    Result result = RangeResults.ofFile(req, path.toFile(), "测 试.tmp");

    assertEquals(result.status(), OK);
    assertEquals(
        "attachment; filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp",
        result.header(CONTENT_DISPOSITION).orElse(""));
  }

  // -- Sources

  @Test
  public void shouldNotReturnRangeResultForSourceWhenHeaderIsNotPresent() throws IOException {
    Http.Request req = mockRegularRequest();

    Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromPath(path);
    Result result =
        RangeResults.ofSource(req, Files.size(path), source, path.toFile().getName(), BINARY);

    assertEquals(result.status(), OK);
    assertEquals(BINARY, result.body().contentType().orElse(""));
  }

  @Test
  public void shouldReturnRangeResultForSourceWhenHeaderIsPresentAndContentTypeWasSpecified()
      throws IOException {
    Http.Request req = mockRangeRequest();

    Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromPath(path);
    Result result =
        RangeResults.ofSource(req, Files.size(path), source, path.toFile().getName(), TEXT);

    assertEquals(result.status(), PARTIAL_CONTENT);
    assertEquals(TEXT, result.body().contentType().orElse(""));
  }

  @Test
  public void shouldReturnRangeResultForSourceWithCustomFilename() throws IOException {
    Http.Request req = mockRangeRequest();

    Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromPath(path);
    Result result = RangeResults.ofSource(req, Files.size(path), source, "file.txt", BINARY);

    assertEquals(result.status(), PARTIAL_CONTENT);
    assertEquals(BINARY, result.body().contentType().orElse(""));
    assertEquals(
        "attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  public void shouldNotReturnRangeResultForSourceWhenHeaderIsNotPresentWithCustomFilename()
      throws IOException {
    Http.Request req = mockRegularRequest();

    Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromPath(path);
    Result result = RangeResults.ofSource(req, Files.size(path), source, "file.txt", BINARY);

    assertEquals(result.status(), OK);
    assertEquals(BINARY, result.body().contentType().orElse(""));
    assertEquals(
        "attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  public void shouldReturnPartialContentForSourceWithGivenEntityLength() throws IOException {
    Http.Request req = mockRangeRequest();

    long entityLength = Files.size(path);
    Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromPath(path);
    Result result = RangeResults.ofSource(req, entityLength, source, "file.txt", TEXT);

    assertEquals(result.status(), PARTIAL_CONTENT);
    assertEquals(TEXT, result.body().contentType().orElse(""));
    assertEquals(
        "attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  public void shouldNotReturnRangeResultForStreamWhenFilenameHasSpecialChars() throws IOException {
    Http.Request req = mockRegularRequest();

    Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromPath(path);
    Result result = RangeResults.ofSource(req, Files.size(path), source, "测 试.tmp", BINARY);

    assertEquals(result.status(), OK);
    assertEquals(BINARY, result.body().contentType().orElse(""));
    assertEquals(
        "attachment; filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp",
        result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  public void shouldReturnRangeResultForStreamWhenFilenameHasSpecialChars() throws IOException {
    Http.Request req = mockRangeRequest();

    long entityLength = Files.size(path);
    Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromPath(path);
    Result result = RangeResults.ofSource(req, entityLength, source, "测 试.tmp", TEXT);

    assertEquals(result.status(), PARTIAL_CONTENT);
    assertEquals(TEXT, result.body().contentType().orElse(""));
    assertEquals(
        "attachment; filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp",
        result.header(CONTENT_DISPOSITION).orElse(""));
  }

  private Http.Request mockRegularRequest() {
    Http.Request request = mock(Http.Request.class);
    when(request.header(RANGE)).thenReturn(Optional.empty());
    when(this.ctx.request()).thenReturn(request);

    mockRegularFileTypes();
    return request;
  }

  private Http.Request mockRangeRequest() {
    Http.Request request = mock(Http.Request.class);
    when(request.header(RANGE)).thenReturn(Optional.of("bytes=0-1"));
    when(this.ctx.request()).thenReturn(request);

    mockRegularFileTypes();
    return request;
  }

  private void mockRegularFileTypes() {
    final DefaultFileMimeTypes defaultFileMimeTypes =
        new DefaultFileMimeTypes(
            new FileMimeTypesConfiguration(Scala.asScala(Collections.emptyMap())));
    final FileMimeTypes fileMimeTypes = new FileMimeTypes(defaultFileMimeTypes);
    when(this.ctx.fileMimeTypes()).thenReturn(fileMimeTypes);
  }
}
