package play;

import com.ning.http.client.AsyncHttpClient;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

public class WS {

    private static AsyncHttpClient client = play.api.WS.client();

    public static WSRequest url(String url) {
        return new WSRequest().setUrl(url);
    }

    public static class WSRequest extends play.api.WS.WSRequestBase<WSRequest, Response> {

        public WSRequest() {
            super(WSRequest.class);
        }

        @Override
        public Response wrapResponse(com.ning.http.client.Response ahcResponse) {
            return new Response(ahcResponse);
        }

    }

    public static class Response extends play.api.ws.WSResponse {

        public Response(com.ning.http.client.Response ahcResponse) {
            super(ahcResponse);
        }

        /**
         * Get the response body as a string
         */
        public String getBody() {
            try {
                return getAHCResponse().getResponseBody();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Get the response body as a {@link Document DOM document}
         * @return a DOM document
         */
        public Document asXml() {
            try {
                return play.libs.XML.fromInputStream(getAHCResponse().getResponseBodyAsStream());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Get the response body as a {@link org.codehaus.jackson.JsonNode}
         * @return the json response
         */
        public JsonNode asJson() {
            String json = getBody();
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.readValue(json, JsonNode.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private static Method getMethod(Class<?> clazz, String name) {
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals(name)) {
                    return m;
                }
            }
            return null;
        }
    }

}



