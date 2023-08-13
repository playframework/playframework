/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.Mockito.*;
import static play.mvc.Http.HeaderNames.*;
import static play.mvc.Http.MimeTypes.*;
import static play.mvc.Http.Status.*;

import akka.actor.ActorSystem;
import akka.stream.IOResult;
import akka.stream.Materializer;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.jdk.javaapi.FutureConverters;

class RangeResultsTest {

  private static Path path;

  @BeforeAll
  static void createFile() throws IOException {
    path = Paths.get("test.tmp");
    Files.createFile(path);
    Files.write(path, "Some content for the file".getBytes(), StandardOpenOption.APPEND);
  }

  @AfterAll
  static void deleteFile() throws IOException {
    Files.deleteIfExists(path);
  }

  // -- InputStreams

  @Test
  void shouldNotReturnRangeResultForInputStreamWhenHeaderIsNotPresent() throws IOException {
    Http.Request req = mockRegularRequest();
    try (InputStream stream = Files.newInputStream(path)) {
      Result result = RangeResults.ofStream(req, stream);
      assertEquals(OK, result.status());
      assertEquals(BINARY, result.body().contentType().orElse(""));
    }
  }

  @Test
  void shouldReturnRangeResultForInputStreamWhenHeaderIsPresentAndContentTypeWasSpecified()
      throws IOException {
    Http.Request req = mockRangeRequest();
    try (InputStream stream = Files.newInputStream(path)) {
      Result result = RangeResults.ofStream(req, stream, Files.size(path), "file.txt", HTML);
      assertEquals(PARTIAL_CONTENT, result.status());
      assertEquals(HTML, result.body().contentType().orElse(""));
    }
  }

  @Test
  void shouldReturnRangeResultForInputStreamWithCustomFilename() throws IOException {
    Http.Request req = mockRangeRequest();
    try (InputStream stream = Files.newInputStream(path)) {
      Result result = RangeResults.ofStream(req, stream, Files.size(path), "file.txt");
      assertEquals(PARTIAL_CONTENT, result.status());
      assertEquals(
          "attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
    }
  }

  @Test
  void shouldNotReturnRangeResultForInputStreamWhenHeaderIsNotPresentWithCustomFilename()
      throws IOException {
    Http.Request req = mockRegularRequest();
    try (InputStream stream = Files.newInputStream(path)) {
      Result result = RangeResults.ofStream(req, stream, Files.size(path), "file.txt");
      assertEquals(OK, result.status());
      assertEquals(BINARY, result.body().contentType().orElse(""));
      assertEquals(
          "attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
    }
  }

  @Test
  void shouldReturnPartialContentForInputStreamWithGivenEntityLength() throws IOException {
    Http.Request req = mockRangeRequest();
    try (InputStream stream = Files.newInputStream(path)) {
      Result result = RangeResults.ofStream(req, stream, Files.size(path));
      assertEquals(PARTIAL_CONTENT, result.status());
      assertEquals("bytes 0-1/" + Files.size(path), result.header(CONTENT_RANGE).get());
    }
  }

  @Test
  void shouldReturnPartialContentForInputStreamWithGivenNameAndContentType() throws IOException {
    Http.Request req = mockRangeRequest();
    try (InputStream stream = Files.newInputStream(path)) {
      Result result = RangeResults.ofStream(req, stream, Files.size(path), "file.txt", TEXT);
      assertEquals(PARTIAL_CONTENT, result.status());
      assertEquals(TEXT, result.body().contentType().orElse(""));
      assertEquals(
          "attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
    }
  }

  // -- Paths

  @Test
  void shouldReturnRangeResultForPath() {
    Http.Request req = mockRangeRequest();
    Result result = RangeResults.ofPath(req, path);

    assertEquals(PARTIAL_CONTENT, result.status());
    assertEquals(
        "attachment; filename=\"test.tmp\"", result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  void shouldNotReturnRangeResultForPathWhenHeaderIsNotPresent() {
    Http.Request req = mockRegularRequest();

    Result result = RangeResults.ofPath(req, path);

    assertEquals(OK, result.status());
    assertEquals(
        "attachment; filename=\"test.tmp\"", result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  void shouldReturnRangeResultForPathWithCustomFilename() {
    Http.Request req = mockRangeRequest();
    Result result = RangeResults.ofPath(req, path, "file.txt");

    assertEquals(PARTIAL_CONTENT, result.status());
    assertEquals(
        "attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  void shouldNotReturnRangeResultForPathWhenHeaderIsNotPresentWithCustomFilename() {
    Http.Request req = mockRegularRequest();

    Result result = RangeResults.ofPath(req, path, "file.txt");

    assertEquals(OK, result.status());
    assertEquals(
        "attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  void shouldReturnRangeResultForPathWhenFilenameHasSpecialChars() {
    Http.Request req = mockRangeRequest();

    Result result = RangeResults.ofPath(req, path, "测 试.tmp");

    assertEquals(PARTIAL_CONTENT, result.status());
    assertEquals(
        "attachment; filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp",
        result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  void shouldNotReturnRangeResultForPathWhenFilenameHasSpecialChars() {
    Http.Request req = mockRegularRequest();

    Result result = RangeResults.ofPath(req, path, "测 试.tmp");

    assertEquals(OK, result.status());
    assertEquals(
        "attachment; filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp",
        result.header(CONTENT_DISPOSITION).orElse(""));
  }

  // -- Files

  @Test
  void shouldReturnRangeResultForFile() {
    Http.Request req = mockRangeRequest();
    Result result = RangeResults.ofFile(req, path.toFile());

    assertEquals(PARTIAL_CONTENT, result.status());
    assertEquals(
        "attachment; filename=\"test.tmp\"", result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  void shouldNotReturnRangeResultForFileWhenHeaderIsNotPresent() {
    Http.Request req = mockRegularRequest();

    Result result = RangeResults.ofFile(req, path.toFile());

    assertEquals(OK, result.status());
    assertEquals(
        "attachment; filename=\"test.tmp\"", result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  void shouldReturnRangeResultForFileWithCustomFilename() {
    Http.Request req = mockRangeRequest();
    Result result = RangeResults.ofFile(req, path.toFile(), "file.txt");

    assertEquals(PARTIAL_CONTENT, result.status());
    assertEquals(
        "attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  void shouldNotReturnRangeResultForFileWhenHeaderIsNotPresentWithCustomFilename() {
    Http.Request req = mockRegularRequest();

    Result result = RangeResults.ofFile(req, path.toFile(), "file.txt");

    assertEquals(OK, result.status());
    assertEquals(
        "attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  void shouldReturnRangeResultForFileWhenFilenameHasSpecialChars() {
    Http.Request req = mockRangeRequest();

    Result result = RangeResults.ofFile(req, path.toFile(), "测 试.tmp");

    assertEquals(PARTIAL_CONTENT, result.status());
    assertEquals(
        "attachment; filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp",
        result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  void shouldNotReturnRangeResultForFileWhenFilenameHasSpecialChars() {
    Http.Request req = mockRegularRequest();

    Result result = RangeResults.ofFile(req, path.toFile(), "测 试.tmp");

    assertEquals(OK, result.status());
    assertEquals(
        "attachment; filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp",
        result.header(CONTENT_DISPOSITION).orElse(""));
  }

  // -- Sources

  @Test
  void shouldNotReturnRangeResultForSourceWhenHeaderIsNotPresent() throws IOException {
    Http.Request req = mockRegularRequest();

    Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromPath(path);
    Result result =
        RangeResults.ofSource(req, Files.size(path), source, path.toFile().getName(), BINARY);

    assertEquals(OK, result.status());
    assertEquals(BINARY, result.body().contentType().orElse(""));
  }

  @Test
  void shouldReturnRangeResultForSourceWhenHeaderIsPresentAndContentTypeWasSpecified()
      throws IOException {
    Http.Request req = mockRangeRequest();

    Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromPath(path);
    Result result =
        RangeResults.ofSource(req, Files.size(path), source, path.toFile().getName(), TEXT);

    assertEquals(PARTIAL_CONTENT, result.status());
    assertEquals(TEXT, result.body().contentType().orElse(""));
  }

  @Test
  void shouldReturnRangeResultForSourceWithCustomFilename() throws IOException {
    Http.Request req = mockRangeRequest();

    Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromPath(path);
    Result result = RangeResults.ofSource(req, Files.size(path), source, "file.txt", BINARY);

    assertEquals(PARTIAL_CONTENT, result.status());
    assertEquals(BINARY, result.body().contentType().orElse(""));
    assertEquals(
        "attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  void shouldNotReturnRangeResultForSourceWhenHeaderIsNotPresentWithCustomFilename()
      throws IOException {
    Http.Request req = mockRegularRequest();

    Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromPath(path);
    Result result = RangeResults.ofSource(req, Files.size(path), source, "file.txt", BINARY);

    assertEquals(OK, result.status());
    assertEquals(BINARY, result.body().contentType().orElse(""));
    assertEquals(
        "attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  void shouldReturnPartialContentForSourceWithGivenEntityLength() throws IOException {
    Http.Request req = mockRangeRequest();

    long entityLength = Files.size(path);
    Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromPath(path);
    Result result = RangeResults.ofSource(req, entityLength, source, "file.txt", TEXT);

    assertEquals(PARTIAL_CONTENT, result.status());
    assertEquals(TEXT, result.body().contentType().orElse(""));
    assertEquals(
        "attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  void shouldNotReturnRangeResultForStreamWhenFilenameHasSpecialChars() throws IOException {
    Http.Request req = mockRegularRequest();

    Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromPath(path);
    Result result = RangeResults.ofSource(req, Files.size(path), source, "测 试.tmp", BINARY);

    assertEquals(OK, result.status());
    assertEquals(BINARY, result.body().contentType().orElse(""));
    assertEquals(
        "attachment; filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp",
        result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  void shouldReturnRangeResultForStreamWhenFilenameHasSpecialChars() throws IOException {
    Http.Request req = mockRangeRequest();

    long entityLength = Files.size(path);
    Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromPath(path);
    Result result = RangeResults.ofSource(req, entityLength, source, "测 试.tmp", TEXT);

    assertEquals(PARTIAL_CONTENT, result.status());
    assertEquals(TEXT, result.body().contentType().orElse(""));
    assertEquals(
        "attachment; filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp",
        result.header(CONTENT_DISPOSITION).orElse(""));
  }

  @Test
  void shouldHandlePreSeekingSource() throws Exception {
    Http.Request req = mockRangeRequestWithOffset();
    long entityLength = Files.size(path);
    byte[] data = "abcdefghijklmnopqrstuvwxyz".getBytes();
    Result result =
        RangeResults.ofSource(req, entityLength, preSeekingSourceFunction(data), "file.tmp", TEXT);
    assertEquals("bc", getBody(result));
  }

  @Test
  void shouldHandleNoSeekingSource() throws Exception {
    Http.Request req = mockRangeRequestWithOffset();
    long entityLength = Files.size(path);
    byte[] data = "abcdefghijklmnopqrstuvwxyz".getBytes();
    Result result =
        RangeResults.ofSource(req, entityLength, noSeekingSourceFunction(data), "file.tmp", TEXT);
    assertEquals("bc", getBody(result));
  }

  @Test
  void shouldRejectBrokenSourceFunction() throws Exception {
    Http.Request req = mockRangeRequestWithOffset();
    long entityLength = Files.size(path);
    byte[] data = "abcdefghijklmnopqrstuvwxyz".getBytes();
    assertThrowsExactly(
        IllegalArgumentException.class,
        () ->
            RangeResults.ofSource(
                req, entityLength, brokenSeekingSourceFunction(data), "file.tmp", TEXT));
  }

  private RangeResults.SourceFunction preSeekingSourceFunction(byte[] data) {
    return offset -> {
      ByteString bytes = ByteString.fromArray(data).drop((int) offset);
      return new RangeResults.SourceAndOffset(offset, Source.single(bytes));
    };
  }

  private RangeResults.SourceFunction noSeekingSourceFunction(byte[] data) {
    return offset -> {
      ByteString bytes = ByteString.fromArray(data);
      return new RangeResults.SourceAndOffset(0, Source.single(bytes));
    };
  }

  /** A SourceFunction that seeks past the request offset - a bug. */
  private RangeResults.SourceFunction brokenSeekingSourceFunction(byte[] data) {
    return offset -> {
      ByteString bytes = ByteString.fromArray(data).drop((int) offset + 1);
      return new RangeResults.SourceAndOffset(offset + 1, Source.single(bytes));
    };
  }

  private Http.Request mockRegularRequest() {
    Http.Request request = mock(Http.Request.class);
    when(request.header(RANGE)).thenReturn(Optional.empty());
    return request;
  }

  private Http.Request mockRangeRequest() {
    Http.Request request = mock(Http.Request.class);
    when(request.header(RANGE)).thenReturn(Optional.of("bytes=0-1"));
    return request;
  }

  private Http.Request mockRangeRequestWithOffset() {
    Http.Request request = mock(Http.Request.class);
    when(request.header(RANGE)).thenReturn(Optional.of("bytes=1-2"));
    return request;
  }

  private String getBody(Result result) throws Exception {
    ActorSystem actorSystem = ActorSystem.create("TestSystem");
    Materializer mat = Materializer.matFromSystem(actorSystem);
    ByteString bs =
        Await.result(
            FutureConverters.asScala(result.body().consumeData(mat)), Duration.create("60s"));
    return bs.utf8String();
  }
}
