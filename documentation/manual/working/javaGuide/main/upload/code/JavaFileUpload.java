/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import static javaguide.testhelpers.MockJavaActionHelper.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;

import akka.stream.IOResult;
import akka.stream.Materializer;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import javax.inject.Inject;
import org.junit.Test;
import play.core.j.JavaHandlerComponents;
import play.core.parsers.Multipart;
import play.http.HttpErrorHandler;
import play.libs.streams.Accumulator;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import play.test.WithApplication;

public class JavaFileUpload extends WithApplication {

  static class AsyncUpload extends Controller {
    // #asyncUpload
    public Result upload(Http.Request request) {
      File file = request.body().asRaw().asFile();
      return ok("File uploaded");
    }
    // #asyncUpload
  }

  // #customfileparthandler
  public static class MultipartFormDataWithFileBodyParser
      extends BodyParser.DelegatingMultipartFormDataBodyParser<File> {

    @Inject
    public MultipartFormDataWithFileBodyParser(
        Materializer materializer,
        play.api.http.HttpConfiguration config,
        HttpErrorHandler errorHandler) {
      super(
          materializer,
          config.parser().maxMemoryBuffer(), // Small buffer used for parsing the body
          config.parser().maxDiskBuffer(), // Maximum allowed length of the request body
          config.parser().allowEmptyFiles(),
          errorHandler);
    }

    /** Creates a file part handler that uses a custom accumulator. */
    @Override
    public Function<Multipart.FileInfo, Accumulator<ByteString, FilePart<File>>>
        createFilePartHandler() {
      return (Multipart.FileInfo fileInfo) -> {
        final String filename = fileInfo.fileName();
        final String partname = fileInfo.partName();
        final String contentType = fileInfo.contentType().getOrElse(null);
        final File file = generateTempFile();
        final String dispositionType = fileInfo.dispositionType();

        final Sink<ByteString, CompletionStage<IOResult>> sink = FileIO.toPath(file.toPath());
        return Accumulator.fromSink(
            sink.mapMaterializedValue(
                completionStage ->
                    completionStage.thenApplyAsync(
                        results ->
                            new Http.MultipartFormData.FilePart<>(
                                partname,
                                filename,
                                contentType,
                                file,
                                results.getCount(),
                                dispositionType))));
      };
    }

    /** Generates a temp file directly without going through TemporaryFile. */
    private File generateTempFile() {
      try {
        final Path path = Files.createTempFile("multipartBody", "tempFile");
        return path.toFile();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }
  // #customfileparthandler

  @Test
  public void testCustomMultipart() throws IOException {
    play.libs.Files.TemporaryFileCreator tfc = play.libs.Files.singletonTemporaryFileCreator();
    Path tmpFile = Files.createTempFile("temp", "txt");
    Files.write(tmpFile, "foo".getBytes());
    Source<ByteString, ?> source = FileIO.fromPath(tmpFile);
    Http.MultipartFormData.FilePart<Source<ByteString, ?>> dp =
        new Http.MultipartFormData.FilePart<>(
            "name", "filename", "text/plain", source, Files.size(tmpFile));
    assertThat(
        contentAsString(
            call(
                new javaguide.testhelpers.MockJavaAction(instanceOf(JavaHandlerComponents.class)) {
                  @BodyParser.Of(MultipartFormDataWithFileBodyParser.class)
                  public Result uploadCustomMultiPart(Http.Request request) throws Exception {
                    final Http.MultipartFormData<File> formData =
                        request.body().asMultipartFormData();
                    final Http.MultipartFormData.FilePart<File> filePart = formData.getFile("name");
                    final File file = filePart.getRef();
                    final long size = filePart.getFileSize();
                    Files.deleteIfExists(file.toPath());
                    return ok("Got: file size = " + size + "");
                  }
                },
                fakeRequest("POST", "/").bodyRaw(Collections.singletonList(dp), tfc, mat),
                mat)),
        equalTo("Got: file size = 3"));
  }
}
