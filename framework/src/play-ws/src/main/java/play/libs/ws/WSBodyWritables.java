/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.ws;

import akka.stream.javadsl.Source;
import akka.util.ByteString;
import play.mvc.Http;
import play.mvc.MultipartFormatter;

/**
 * JSON, XML and Multipart Form Data Writables used for Play-WS bodies.
 */
public interface WSBodyWritables extends DefaultBodyWritables, XMLBodyWritables, JsonBodyWritables {

    default SourceBodyWritable multipartBody(Source<? super Http.MultipartFormData.Part<Source<ByteString, ?>>, ?> body) {
        String boundary = MultipartFormatter.randomBoundary();
        Source<ByteString, ?> source = MultipartFormatter.transform(body, boundary);
        String contentType = "multipart/form-data; boundary=" + boundary;
        return new SourceBodyWritable(source, contentType);
    }

}
