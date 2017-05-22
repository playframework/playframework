/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.ws;

import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;
import java.io.InputStream;

/**
 *
 */
public interface WSBodyFactory {

    WSBody<String> string(String body);

    WSBody<JsonNode> json(JsonNode body);

    WSBody<Source<ByteString, ?>> source(Source<ByteString, ?> body);

    WSBody<File> file(File body);

    @Deprecated
    public WSBody<InputStream> inputStream(InputStream body);

    WSBody<Object> empty();
}