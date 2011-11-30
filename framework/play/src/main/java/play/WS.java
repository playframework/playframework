package play;

import com.ning.http.client.AsyncHttpClient;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonDeserializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

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
         * Get the response body as a {@link com.google.gson.JsonElement}
         * @return the json response
         */
        public JsonElement asJson() {
            String json = getBody();
            try {
                return new JsonParser().parse(json);
            } catch (Exception e) {
                Logger.error("Bad JSON: " + json, e);
                throw new RuntimeException("Cannot parse JSON (check logs)", e);
            }
        }

        /**
         * Get the response body as a Json, and to convert it in a business class object
         * @param classOfT the target class to deserialize the Json
         */
        public <T> T fromJson(Class<T> classOfT) {
            return (new Gson()).fromJson(getBody(), classOfT);
        }

        /**
         * Get the response body as a Json, and to convert it in a business class object
         * @param adapters custom deserializers to be used
         */
        public <T> T fromJson(Class<T> classOfT, JsonDeserializer<?>... adapters) {
            GsonBuilder gson = new GsonBuilder();
            for (Object adapter: adapters) {
                Type t = getMethod(adapter.getClass(), "deserialize").getParameterTypes()[0];
                gson.registerTypeAdapter(t, adapter);
            }
            return gson.create().fromJson(getBody(), classOfT);
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



