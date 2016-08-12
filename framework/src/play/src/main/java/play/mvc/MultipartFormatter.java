/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.mvc;

import akka.stream.javadsl.Source;
import akka.util.ByteString;
import play.api.mvc.MultipartFormData;
import play.core.formatters.Multipart;
import scala.Option;

import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;


public class MultipartFormatter {

    public static String randomBoundary() {
        return Multipart.randomBoundary(18, ThreadLocalRandom.current());
    }

    public static String boundaryToContentType(String boundary) {
        return "multipart/form-data; boundary=" + boundary;
    }

    public static Source<ByteString, ?> transform(Source<? super Http.MultipartFormData.Part<Source<ByteString, ?>>, ?> parts, String boundary) {
        return parts.map((part) -> {
            if (part instanceof Http.MultipartFormData.DataPart) {
                Http.MultipartFormData.DataPart dp = (Http.MultipartFormData.DataPart) part;
                return new Http.MultipartFormData.SourcePart(dp.getKey(), Source.single(ByteString.fromString(dp.getValue()))).asScala();
            } else if (part instanceof Http.MultipartFormData.SourcePart) {
                return ((Http.MultipartFormData.SourcePart) part).asScala();
            } else if (part instanceof Http.MultipartFormData.FilePart) {
                Http.MultipartFormData.FilePart fp = (Http.MultipartFormData.FilePart) part;
                if (fp.file instanceof Source) {
                    Source ref = (Source) fp.file;
                    return new Http.MultipartFormData.SourcePart(fp.getKey(), ref, Optional.of(fp.filename), Optional.of(fp.getContentType())).asScala();
                }
            }
            throw new UnsupportedOperationException("Unsupported Part Class");
        }).via(Multipart.format(boundary, Charset.defaultCharset(), 4096));

    }


}
