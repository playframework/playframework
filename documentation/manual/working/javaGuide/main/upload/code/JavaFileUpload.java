/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
import akka.stream.IOResult;
import akka.stream.Materializer;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import org.junit.Test;
import play.api.http.HttpErrorHandler;
import play.core.j.JavaHandlerComponents;
import play.core.parsers.Multipart;
import play.libs.streams.Accumulator;
import play.mvc.BodyParser;
import play.mvc.Controller;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import play.mvc.Http;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import play.test.WithApplication;

import javax.inject.Inject;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;

import static javaguide.testhelpers.MockJavaActionHelper.*;

public class JavaFileUpload extends WithApplication {

  static class SyncUpload extends Controller {
    // #syncUpload
    public Result upload() {
      Http.MultipartFormData<File> body = request().body().asMultipartFormData();
      Http.MultipartFormData.FilePart<File> picture = body.getFile("picture");
      if (picture != null) {
        String fileName = picture.getFilename();
        String contentType = picture.getContentType();
        File file = picture.getFile();
        return ok("File uploaded");
      } else {
        flash("error", "Missing file");
        return badRequest();
      }
    }
    // #syncUpload
  }

  static class AsyncUpload extends Controller {
    // #asyncUpload
    public Result upload() {
      File file = request().body().asRaw().asFile();
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
      super(materializer, config.parser().maxDiskBuffer(), errorHandler);
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

        final Sink<ByteString, CompletionStage<IOResult>> sink = FileIO.toFile(file);
        return Accumulator.fromSink(
            sink.mapMaterializedValue(
                completionStage ->
                    completionStage.thenApplyAsync(
                        results ->
                            new Http.MultipartFormData.FilePart<>(
                                partname, filename, contentType, file))));
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
    Source source = FileIO.fromPath(Files.createTempFile("temp", "txt"));
    Http.MultipartFormData.FilePart dp =
        new Http.MultipartFormData.FilePart<Source>("name", "filename", "text/plain", source);
    assertThat(
        contentAsString(
            call(
                new javaguide.testhelpers.MockJavaAction(instanceOf(JavaHandlerComponents.class)) {
                  @BodyParser.Of(MultipartFormDataWithFileBodyParser.class)
                  public Result uploadCustomMultiPart() throws Exception {
                    final Http.MultipartFormData<File> formData =
                        request().body().asMultipartFormData();
                    final Http.MultipartFormData.FilePart<File> filePart = formData.getFile("name");
                    final File file = filePart.getFile();
                    final long size = Files.size(file.toPath());
                    Files.deleteIfExists(file.toPath());
                    return ok("Got: file size = " + size + "");
                  }
                },
                fakeRequest("POST", "/").bodyMultipart(Collections.singletonList(dp), tfc, mat),
                mat)),
        equalTo("Got: file size = 0"));
  }
}
