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

    public WSBody<String> string(String body);

    public WSBody<JsonNode> json(JsonNode body);

    public WSBody<Source<ByteString, ?>> source(Source<ByteString, ?> body);

    public WSBody<File> file(File body);

    @Deprecated
    public WSBody<InputStream> inputStream(InputStream body);

    public WSBody<Object> empty();
}