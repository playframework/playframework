/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import akka.stream.Materializer;
import akka.util.ByteString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import play.Application;
import play.libs.Files;
import play.mvc.Http;
import play.mvc.Result;
import play.test.junit5.ApplicationExtension;

import static java.nio.file.Files.write;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.POST;
import static play.test.Helpers.route;
import static play.test.Helpers.fakeApplication;


public class HomeControllerTest {

    @RegisterExtension
    static ApplicationExtension appExtension = new ApplicationExtension(fakeApplication());
    static Application app = appExtension.getApplication();
    static Materializer mat = appExtension.getMaterializer();

    @Test
    void testOnlyFormDataNoFiles() throws ExecutionException, InterruptedException, TimeoutException {
        final Map<String, String[]> postParams = new HashMap<>();
        postParams.put("key1", new String[]{"value1"});
        Http.RequestBuilder request = new Http.RequestBuilder()
                .method(POST)
                .bodyMultipart(postParams, Collections.emptyList())
                .uri("/multipart-form-data-no-files");

        Result result = route(app, request);
        String content = result.body().consumeData(mat).thenApply(bs -> bs.utf8String()).toCompletableFuture().get(5, TimeUnit.SECONDS);
        assertEquals(OK, result.status());
        assertEquals("Files: 0, Data: 1 [value1]", content);
    }

    @Test
    void testStringFilePart() throws ExecutionException, InterruptedException, TimeoutException {
        String content = "Twas brillig and the slithy Toves...";
        testTemporaryFile(List.of(new Http.MultipartFormData.FilePart<>("document", "jabberwocky.txt", "text/plain", content,
                data -> Optional.of(ByteString.fromString(data)))));
    }

    @Test
    void testStringFilePartToRefToBytesDefined() throws ExecutionException, InterruptedException, TimeoutException {
        String content = "Twas brillig and the slithy Toves...";
        assertThrows(
            RuntimeException.class,
            () -> testTemporaryFile(List.of(new Http.MultipartFormData.FilePart<>("document", "jabberwocky.txt", "text/plain", content))),
            "To be able to convert this FilePart's ref to bytes you need to define refToBytes of FilePart[java.lang.String]"
        );
    }

    @Test
    void testJavaTemporaryFile() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        Files.TemporaryFile tempFile = Files.singletonTemporaryFileCreator().create("temp", "txt");
        write(tempFile.path(), "Twas brillig and the slithy Toves...".getBytes());
        testTemporaryFile(List.of(new Http.MultipartFormData.FilePart<>("document", "jabberwocky.txt", "text/plain", tempFile)));
    }

    @Test
    void testScalaTemporaryFile() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        play.api.libs.Files.TemporaryFile tempFile = play.api.libs.Files.SingletonTemporaryFileCreator$.MODULE$.create("temp", "txt");
        write(tempFile.path(), "Twas brillig and the slithy Toves...".getBytes());
        testTemporaryFile(List.of(new Http.MultipartFormData.FilePart<>("document", "jabberwocky.txt", "text/plain", tempFile)));
    }

    @Test
    void testFile() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        play.api.libs.Files.TemporaryFile tempFile = play.api.libs.Files.SingletonTemporaryFileCreator$.MODULE$.create("temp", "txt");
        write(tempFile.path(), "Twas brillig and the slithy Toves...".getBytes());
        testTemporaryFile(List.of(new Http.MultipartFormData.FilePart<>("document", "jabberwocky.txt", "text/plain", tempFile.file())));
    }

    @Test
    void testPath() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        play.api.libs.Files.TemporaryFile tempFile = play.api.libs.Files.SingletonTemporaryFileCreator$.MODULE$.create("temp", "txt");
        write(tempFile.path(), "Twas brillig and the slithy Toves...".getBytes());
        testTemporaryFile(List.of(new Http.MultipartFormData.FilePart<>("document", "jabberwocky.txt", "text/plain", tempFile.path())));
    }

    private void testTemporaryFile(final List<Http.MultipartFormData.FilePart> files) throws ExecutionException, InterruptedException, TimeoutException {
        final Map<String, String[]> data = new HashMap<>();
        data.put("author", new String[]{"Lewis Carrol"});

        Http.RequestBuilder request = new Http.RequestBuilder()
                .method(POST)
                .bodyMultipart(data, files)
                .uri("/multipart-form-data");

        Result result = route(app, request);
        String content = result.body().consumeData(mat).thenApply(bs -> bs.utf8String()).toCompletableFuture().get(5, TimeUnit.SECONDS);
        assertEquals(OK, result.status());
        assertEquals("author: Lewis Carrol\n"
                        + "filename: jabberwocky.txt\n"
                        + "contentType: text/plain\n"
                        + "contents: Twas brillig and the slithy Toves...\n",
                content);
    }
}
