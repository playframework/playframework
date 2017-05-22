package play.libs.ws.ahc;

import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.JsonNode;
import play.libs.ws.WSBody;
import play.libs.ws.WSBodyFactory;

import java.io.File;
import java.io.InputStream;

public class AhcWSBodyFactory implements WSBodyFactory {

    @Override
    public WSBody<String> string(String body) {
        return AhcWSBody.string(body);
    }

    @Override
    public WSBody<JsonNode> json(JsonNode body) {
        return AhcWSBody.json(body);
    }

    @Override
    public WSBody<Source<ByteString, ?>> source(Source<ByteString, ?> body) {
        return AhcWSBody.source(body);
    }

    @Override
    public WSBody<File> file(File body) {
        return AhcWSBody.file(body);
    }

    @Override
    public WSBody<InputStream> inputStream(InputStream body) {
        return AhcWSBody.inputStream(body);
    }

    @Override
    public WSBody<Object> empty() {
        return AhcWSBody.empty();
    }

}
