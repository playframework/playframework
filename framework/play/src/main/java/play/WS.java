package play;

import com.ning.http.client.AsyncHttpClient;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

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
         * parse and get the response body as a {@link Document DOM document}
         * @param encoding xml charset encoding
         * @return a DOM document
         */
        public Document xml() {
            try {
                return play.libs.XML.fromInputStream(getAHCResponse().getResponseBodyAsStream());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

}



