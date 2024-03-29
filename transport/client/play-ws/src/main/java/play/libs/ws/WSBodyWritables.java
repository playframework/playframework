/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.ws;

import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.ByteString;
import play.mvc.Http;
import play.mvc.MultipartFormatter;

/** JSON, XML and Multipart Form Data Writables used for Play-WS bodies. */
public interface WSBodyWritables extends DefaultBodyWritables, XMLBodyWritables, JsonBodyWritables {

  default SourceBodyWritable multipartBody(
      Source<? super Http.MultipartFormData.Part<Source<ByteString, ?>>, ?> body) {
    String boundary = MultipartFormatter.randomBoundary();
    Source<ByteString, ?> source = MultipartFormatter.transform(body, boundary);
    String contentType = "multipart/form-data; boundary=" + boundary;
    return new SourceBodyWritable(source, contentType);
  }
}
