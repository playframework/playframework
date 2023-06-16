/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import java.nio.charset.Charset;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.ByteString;
import play.api.mvc.MultipartFormData;
import play.core.formatters.Multipart;
import scala.Option;
import scala.jdk.javaapi.OptionConverters;

public class MultipartFormatter {

  public static String randomBoundary() {
    return Multipart.randomBoundary(18, ThreadLocalRandom.current());
  }

  public static String boundaryToContentType(String boundary) {
    return "multipart/form-data; boundary=" + boundary;
  }

  public static Source<ByteString, ?> transform(
      Source<? super Http.MultipartFormData.Part<Source<ByteString, ?>>, ?> parts,
      String boundary) {
    @SuppressWarnings("unchecked")
    Source<MultipartFormData.Part<org.apache.pekko.stream.scaladsl.Source<ByteString, ?>>, ?>
        source =
            parts.map(
                part -> {
                  if (part instanceof Http.MultipartFormData.DataPart) {
                    Http.MultipartFormData.DataPart dp = (Http.MultipartFormData.DataPart) part;
                    return (MultipartFormData.Part)
                        new MultipartFormData.DataPart(dp.getKey(), dp.getValue());
                  } else if (part instanceof Http.MultipartFormData.FilePart) {
                    if (((Http.MultipartFormData.FilePart) part).ref instanceof Source) {
                      @SuppressWarnings("unchecked")
                      Http.MultipartFormData.FilePart<Source<ByteString, ?>> fp =
                          (Http.MultipartFormData.FilePart<Source<ByteString, ?>>) part;
                      Option<String> ct = Option.apply(fp.getContentType());
                      return new MultipartFormData.FilePart<
                          org.apache.pekko.stream.scaladsl.Source<ByteString, ?>>(
                          fp.getKey(),
                          fp.getFilename(),
                          ct,
                          fp.ref.asScala(),
                          fp.getFileSize(),
                          fp.getDispositionType(),
                          byteSource ->
                              OptionConverters.toScala(fp.refToBytes.apply(byteSource.asJava())));
                    }
                  }
                  throw new UnsupportedOperationException("Unsupported Part Class");
                });

    return source.via(Multipart.format(boundary, Charset.defaultCharset(), 4096));
  }
}
