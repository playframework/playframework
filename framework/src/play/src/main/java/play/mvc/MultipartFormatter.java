/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import akka.stream.javadsl.Source;
import akka.util.ByteString;
import play.api.mvc.MultipartFormData;
import play.core.formatters.Multipart;
import scala.Option;

import java.nio.charset.Charset;
import java.util.concurrent.ThreadLocalRandom;


public class MultipartFormatter {

    public static String randomBoundary() {
        return Multipart.randomBoundary(18, ThreadLocalRandom.current());
    }

    public static String boundaryToContentType(String boundary) {
        return "multipart/form-data; boundary=" + boundary;
    }

    public static Source<ByteString, ?> transform(Source<? super Http.MultipartFormData.Part<Source<ByteString, ?>>, ?> parts, String boundary) {
        Source<MultipartFormData.Part<akka.stream.scaladsl.Source<ByteString, ?>>, ?> source = parts.map((part) -> {
            if (part instanceof Http.MultipartFormData.DataPart) {
                Http.MultipartFormData.DataPart dp = (Http.MultipartFormData.DataPart) part;
                return (MultipartFormData.Part) new MultipartFormData.DataPart(dp.getKey(), dp.getValue());
            } else if (part instanceof Http.MultipartFormData.FilePart) {
                Http.MultipartFormData.FilePart fp = (Http.MultipartFormData.FilePart) part;
                if (fp.ref instanceof Source) {
                    Source ref = (Source) fp.ref;
                    Option<String> ct = Option.apply(fp.getContentType());
                    return (MultipartFormData.Part)new MultipartFormData.FilePart<akka.stream.scaladsl.Source<ByteString, ?>>(fp.getKey(), fp.getFilename(), ct, ref.asScala(), fp.getDispositionType());
                }
            }
            throw new UnsupportedOperationException("Unsupported Part Class");
        });

        return source.via(Multipart.format(boundary, Charset.defaultCharset(), 4096));
    }


}
