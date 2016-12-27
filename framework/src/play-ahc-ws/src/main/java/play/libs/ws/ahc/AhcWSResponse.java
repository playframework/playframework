package play.libs.ws.ahc;

import com.fasterxml.jackson.databind.JsonNode;
import org.w3c.dom.Document;
import play.libs.ws.WSCookie;
import play.libs.ws.WSResponse;
import play.libs.ws.StandaloneWSResponse;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class AhcWSResponse implements WSResponse {

    private final StandaloneWSResponse response;

    public AhcWSResponse(StandaloneWSResponse response) {
        this.response = response;
    }

    @Override
    public Map<String, List<String>> getAllHeaders() {
        return response.getAllHeaders();
    }

    @Override
    public String getHeader(String key) {
        return response.getHeader(key);
    }

    @Override
    public Object getUnderlying() {
        return response.getUnderlying();
    }

    @Override
    public int getStatus() {
        return response.getStatus();
    }

    @Override
    public String getStatusText() {
        return response.getStatusText();
    }

    @Override
    public List<WSCookie> getCookies() {
        return response.getCookies();
    }

    @Override
    public WSCookie getCookie(String name) {
        return response.getCookie(name);
    }

    @Override
    public String getBody() {
        return response.getBody();
    }

    @Override
    public Document asXml() {
        return response.asXml();
    }

    @Override
    public JsonNode asJson() {
        return response.asJson();
    }

    @Override
    public InputStream getBodyAsStream() {
        return response.getBodyAsStream();
    }

    @Override
    public byte[] asByteArray() {
        return response.asByteArray();
    }

    @Override
    public URI getUri() {
        return response.getUri();
    }
}
