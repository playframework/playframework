/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
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
        this.mockRegularRequest();
        try (InputStream stream = Files.newInputStream(path)) {
            Result result = RangeResults.ofStream(stream);
            assertEquals(result.status(), OK);
            assertEquals(BINARY, result.body().contentType().orElse(""));
        }
    }

    @Test
    public void shouldReturnRangeResultForInputStreamWhenHeaderIsPresentAndContentTypeWasSpecified() throws IOException {
        this.mockRangeRequest();
        try (InputStream stream = Files.newInputStream(path)) {
            Result result = RangeResults.ofStream(stream, path.toFile().length(), "file.txt", HTML);
            assertEquals(result.status(), PARTIAL_CONTENT);
            assertEquals(HTML, result.body().contentType().orElse(""));
        }
    }

    @Test
    public void shouldReturnRangeResultForInputStreamWithCustomFilename() throws IOException {
        this.mockRangeRequest();
        try (InputStream stream = Files.newInputStream(path)) {
            Result result = RangeResults.ofStream(stream, path.toFile().length(), "file.txt");
            assertEquals(result.status(), PARTIAL_CONTENT);
            assertEquals("attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
        }
    }

    @Test
    public void shouldNotReturnRangeResultForInputStreamWhenHeaderIsNotPresentWithCustomFilename() throws IOException {
        this.mockRegularRequest();
        try (InputStream stream = Files.newInputStream(path)) {
            Result result = RangeResults.ofStream(stream, path.toFile().length(), "file.txt");
            assertEquals(result.status(), OK);
            assertEquals(BINARY, result.body().contentType().orElse(""));
            assertEquals("attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
        }
    }

    @Test
    public void shouldReturnPartialContentForInputStreamWithGivenEntityLength() throws IOException {
        this.mockRangeRequest();
        try (InputStream stream = Files.newInputStream(path)) {
            Result result = RangeResults.ofStream(stream, path.toFile().length());
            assertEquals(result.status(), PARTIAL_CONTENT);
            assertEquals(result.header(CONTENT_RANGE).get(), "bytes 0-1/" + path.toFile().length());
        }
    }

    @Test
    public void shouldReturnPartialContentForInputStreamWithGivenNameAndContentType() throws IOException {
        this.mockRangeRequest();
        try (InputStream stream = Files.newInputStream(path)) {
            Result result = RangeResults.ofStream(stream, path.toFile().length(), "file.txt", TEXT);
            assertEquals(result.status(), PARTIAL_CONTENT);
            assertEquals(TEXT, result.body().contentType().orElse(""));
            assertEquals("attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
        }
    }

    // -- Paths

    @Test
    public void shouldReturnRangeResultForPath() {
        this.mockRangeRequest();
        Result result = RangeResults.ofPath(path);

        assertEquals(result.status(), PARTIAL_CONTENT);
        assertEquals("attachment; filename=\"test.tmp\"", result.header(CONTENT_DISPOSITION).orElse(""));
    }

    @Test
    public void shouldNotReturnRangeResultForPathWhenHeaderIsNotPresent() {
        this.mockRegularRequest();

        Result result = RangeResults.ofPath(path);

        assertEquals(result.status(), OK);
        assertEquals("attachment; filename=\"test.tmp\"", result.header(CONTENT_DISPOSITION).orElse(""));
    }

    @Test
    public void shouldReturnRangeResultForPathWithCustomFilename() {
        this.mockRangeRequest();
        Result result = RangeResults.ofPath(path, "file.txt");

        assertEquals(result.status(), PARTIAL_CONTENT);
        assertEquals("attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
    }

    @Test
    public void shouldNotReturnRangeResultForPathWhenHeaderIsNotPresentWithCustomFilename() {
        this.mockRegularRequest();

        Result result = RangeResults.ofPath(path, "file.txt");

        assertEquals(result.status(), OK);
        assertEquals("attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
    }

    @Test
    public void shouldReturnRangeResultForPathWhenFilenameHasSpecialChars() {
        this.mockRangeRequest();

        Result result = RangeResults.ofPath(path, "测 试.tmp");

        assertEquals(result.status(), PARTIAL_CONTENT);
        assertEquals("attachment; filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp", result.header(CONTENT_DISPOSITION).orElse(""));
    }

    @Test
    public void shouldNotReturnRangeResultForPathWhenFilenameHasSpecialChars() {
        this.mockRegularRequest();

        Result result = RangeResults.ofPath(path, "测 试.tmp");

        assertEquals(result.status(), OK);
        assertEquals("attachment; filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp", result.header(CONTENT_DISPOSITION).orElse(""));
    }

    // -- Files

    @Test
    public void shouldReturnRangeResultForFile() {
        this.mockRangeRequest();
        Result result = RangeResults.ofFile(path.toFile());

        assertEquals(result.status(), PARTIAL_CONTENT);
        assertEquals("attachment; filename=\"test.tmp\"", result.header(CONTENT_DISPOSITION).orElse(""));
    }

    @Test
    public void shouldNotReturnRangeResultForFileWhenHeaderIsNotPresent() {
        this.mockRegularRequest();

        Result result = RangeResults.ofFile(path.toFile());

        assertEquals(result.status(), OK);
        assertEquals("attachment; filename=\"test.tmp\"", result.header(CONTENT_DISPOSITION).orElse(""));
    }

    @Test
    public void shouldReturnRangeResultForFileWithCustomFilename() {
        this.mockRangeRequest();
        Result result = RangeResults.ofFile(path.toFile(), "file.txt");

        assertEquals(result.status(), PARTIAL_CONTENT);
        assertEquals("attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
    }

    @Test
    public void shouldNotReturnRangeResultForFileWhenHeaderIsNotPresentWithCustomFilename() {
        this.mockRegularRequest();

        Result result = RangeResults.ofFile(path.toFile(), "file.txt");

        assertEquals(result.status(), OK);
        assertEquals("attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
    }

    @Test
    public void shouldReturnRangeResultForFileWhenFilenameHasSpecialChars() {
        this.mockRangeRequest();

        Result result = RangeResults.ofFile(path.toFile(), "测 试.tmp");

        assertEquals(result.status(), PARTIAL_CONTENT);
        assertEquals("attachment; filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp", result.header(CONTENT_DISPOSITION).orElse(""));
    }

    @Test
    public void shouldNotReturnRangeResultForFileWhenFilenameHasSpecialChars() {
        this.mockRegularRequest();

        Result result = RangeResults.ofFile(path.toFile(), "测 试.tmp");

        assertEquals(result.status(), OK);
        assertEquals("attachment; filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp", result.header(CONTENT_DISPOSITION).orElse(""));
    }

    // -- Sources

    @Test
    public void shouldNotReturnRangeResultForSourceWhenHeaderIsNotPresent() throws IOException {
        this.mockRegularRequest();

        Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromFile(path.toFile());
        Result result = RangeResults.ofSource(path.toFile().length(), source, path.toFile().getName(), BINARY);

        assertEquals(result.status(), OK);
        assertEquals(BINARY, result.body().contentType().orElse(""));
    }

    @Test
    public void shouldReturnRangeResultForSourceWhenHeaderIsPresentAndContentTypeWasSpecified() throws IOException {
        this.mockRangeRequest();

        Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromFile(path.toFile());
        Result result = RangeResults.ofSource(path.toFile().length(), source, path.toFile().getName(), TEXT);

        assertEquals(result.status(), PARTIAL_CONTENT);
        assertEquals(TEXT, result.body().contentType().orElse(""));
    }

    @Test
    public void shouldReturnRangeResultForSourceWithCustomFilename() throws IOException {
        this.mockRangeRequest();

        Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromFile(path.toFile());
        Result result = RangeResults.ofSource(path.toFile().length(), source, "file.txt", BINARY);

        assertEquals(result.status(), PARTIAL_CONTENT);
        assertEquals(BINARY, result.body().contentType().orElse(""));
        assertEquals("attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
    }

    @Test
    public void shouldNotReturnRangeResultForSourceWhenHeaderIsNotPresentWithCustomFilename() throws IOException {
        this.mockRegularRequest();

        Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromFile(path.toFile());
        Result result = RangeResults.ofSource(path.toFile().length(), source, "file.txt", BINARY);

        assertEquals(result.status(), OK);
        assertEquals(BINARY, result.body().contentType().orElse(""));
        assertEquals("attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
    }

    @Test
    public void shouldReturnPartialContentForSourceWithGivenEntityLength() throws IOException {
        this.mockRangeRequest();

        long entityLength = path.toFile().length();
        Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromFile(path.toFile());
        Result result = RangeResults.ofSource(entityLength, source, "file.txt", TEXT);

        assertEquals(result.status(), PARTIAL_CONTENT);
        assertEquals(TEXT, result.body().contentType().orElse(""));
        assertEquals("attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
    }

    @Test
    public void shouldNotReturnRangeResultForStreamWhenFilenameHasSpecialChars() throws IOException {
        this.mockRegularRequest();

        Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromFile(path.toFile());
        Result result = RangeResults.ofSource(path.toFile().length(), source, "测 试.tmp", BINARY);

        assertEquals(result.status(), OK);
        assertEquals(BINARY, result.body().contentType().orElse(""));
        assertEquals("attachment; filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp", result.header(CONTENT_DISPOSITION).orElse(""));
    }

    @Test
    public void shouldReturnRangeResultForStreamWhenFilenameHasSpecialChars() throws IOException {
        this.mockRangeRequest();

        long entityLength = path.toFile().length();
        Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromFile(path.toFile());
        Result result = RangeResults.ofSource(entityLength, source, "测 试.tmp", TEXT);

        assertEquals(result.status(), PARTIAL_CONTENT);
        assertEquals(TEXT, result.body().contentType().orElse(""));
        assertEquals("attachment; filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp", result.header(CONTENT_DISPOSITION).orElse(""));
    }

    private void mockRegularRequest() {
        Http.Request request = mock(Http.Request.class);
        when(request.header(RANGE)).thenReturn(Optional.empty());
        when(this.ctx.request()).thenReturn(request);
        
        mockRegularFileTypes();
    }

    private void mockRangeRequest() {
        Http.Request request = mock(Http.Request.class);
        when(request.header(RANGE)).thenReturn(Optional.of("bytes=0-1"));
        when(this.ctx.request()).thenReturn(request);

        mockRegularFileTypes();
    }

    private void mockRegularFileTypes() {
        final DefaultFileMimeTypes defaultFileMimeTypes = new DefaultFileMimeTypes(new FileMimeTypesConfiguration(Scala.asScala(Collections.emptyMap())));
        final FileMimeTypes fileMimeTypes = new FileMimeTypes(defaultFileMimeTypes);
        when(this.ctx.fileMimeTypes()).thenReturn(fileMimeTypes);
    }
}
