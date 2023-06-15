/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import org.apache.pekko.stream.IOResult;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.stream.javadsl.FileIO;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.util.ByteString;
import play.core.parsers.Multipart;
import play.http.HttpErrorHandler;
import play.libs.streams.Accumulator;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class DummyDelegatingMultipartFormDataBodyParser
        extends BodyParser.DelegatingMultipartFormDataBodyParser<File> {

  @Inject
  public DummyDelegatingMultipartFormDataBodyParser(
          Materializer materializer,
          long maxMemoryBufferSize,
          long maxLength,
          boolean allowEmptyFiles,
          HttpErrorHandler errorHandler) {
    super(materializer, maxMemoryBufferSize, maxLength, allowEmptyFiles, errorHandler);
  }

  @Override
  public Function<Multipart.FileInfo, Accumulator<ByteString, Http.MultipartFormData.FilePart<File>>>
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

  private File generateTempFile() {
    try {
      final Path path = Files.createTempFile("multipartBody", "tempFile");
      return path.toFile();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
