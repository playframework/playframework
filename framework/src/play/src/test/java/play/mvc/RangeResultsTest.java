/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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

    // -- Sources

    @Test
    public void shouldNotReturnRangeResultForStreamWhenHeaderIsNotPresent() throws IOException {
        this.mockRegularRequest();

        Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromFile(path.toFile());
        Result result = RangeResults.ofSource(path.toFile().length(), source, path.toFile().getName(), BINARY);

        assertEquals(result.status(), OK);
        assertEquals(BINARY, result.body().contentType().orElse(""));
    }

    @Test
    public void shouldReturnRangeResultForStreamWhenHeaderIsPresentAndContentTypeWasSpecified() throws IOException {
        this.mockRangeRequest();

        Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromFile(path.toFile());
        Result result = RangeResults.ofSource(path.toFile().length(), source, path.toFile().getName(), TEXT);

        assertEquals(result.status(), PARTIAL_CONTENT);
        assertEquals(TEXT, result.body().contentType().orElse(""));
    }

    @Test
    public void shouldReturnRangeResultForStreamWithCustomFilename() throws IOException {
        this.mockRangeRequest();

        Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromFile(path.toFile());
        Result result = RangeResults.ofSource(path.toFile().length(), source, "file.txt", BINARY);

        assertEquals(result.status(), PARTIAL_CONTENT);
        assertEquals(BINARY, result.body().contentType().orElse(""));
        assertEquals("attachment; filename=\"file.txt\"", result.header(CONTENT_DISPOSITION).orElse(""));
    }

    @Test
    public void shouldNotReturnRangeResultForStreamWhenHeaderIsNotPresentWithCustomFilename() throws IOException {
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

    private void mockRegularRequest() {
        Http.Request request = mock(Http.Request.class);
        when(this.ctx.request()).thenReturn(request);
    }

    private void mockRangeRequest() {
        Http.Request request = mock(Http.Request.class);
        when(request.getHeader(RANGE)).thenReturn("bytes=0-1");
        when(this.ctx.request()).thenReturn(request);
    }
}
