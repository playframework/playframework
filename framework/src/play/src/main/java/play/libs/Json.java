package play.libs;

import java.io.StringWriter;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.node.*;
import play.Application;
import play.Play;
import play.Plugin;

/**
 * Helper functions to handle JsonNode values.
 */
public class Json {

    /**
     * Convert an object to JsonNode.
     *
     * @param data Value to convert in Json.
     */
    public static JsonNode toJson(final Object data) {
        try {
            return mapper().valueToTree(data);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
   
    /**
     * Convert a JsonNode to a Java value
     *
     * @param json Json value to convert.
     * @param clazz Expected Java value type.
     */
    public static <A> A fromJson(JsonNode json, Class<A> clazz) {
        try {
            return mapper().treeToValue(json, clazz);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Creates a new empty ObjectNode.
     */ 
    public static ObjectNode newObject() {
        return mapper().createObjectNode();
    }
    
    /**
     * Convert a JsonNode to its string representation.
     */
    public static String stringify(JsonNode json) {
        return json.toString();
    }
    
    /**
     * Parse a String representing a json, and return it as a JsonNode.
     */
    public static JsonNode parse(String src) {
        try {
            return mapper().readValue(src, JsonNode.class);
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Get the ObjectMapper that Play uses to parse/generate JSON.  Note that although ObjectMapper instances are
     * mutable and can be reconfigured, it is strongly advised that you don't change the configuration of the returned
     * ObjectMapper.  If you need to change the configuration, write a {@link ObjectMapperProvider}, and define it in
     * your configuration using the <code>json.objectMapperProvider</code> property.
     *
     * @return Plays ObjectMapper
     */
    public static ObjectMapper mapper() {
        if (play.api.Play.maybeApplication().isDefined()) {
            return Play.application().plugin(JsonPlugin.class).objectMapper;
        } else {
            return defaultObjectMapper;
        }
    }

    /**
     * The default object mapper.  Used if no current running application can be found, so that Json can be used, for
     * example, in unit tests without having to start a fake application.
     */
    private static final ObjectMapper defaultObjectMapper = new ObjectMapper();

    /**
     * Provider for providing a custom ObjectMapper to be used by {@link play.libs.Json}.
     */
    public interface ObjectMapperProvider {
        /**
         * Provide an object mapper.  This will be called when the application starts up.
         *
         * @return The object mapper.
         */
        ObjectMapper provide();
    }

    public static class JsonPlugin extends Plugin {
        private final Application app;

        private volatile ObjectMapper objectMapper;

        public JsonPlugin(Application app) {
            this.app = app;
        }

        public void onStart() {
            String objectMapperProvider = app.configuration().getString("json.objectMapperProvider");
            if (objectMapperProvider == null) {
                objectMapper = defaultObjectMapper;
            } else {
                try {
                    objectMapper = app.classloader().loadClass(objectMapperProvider).asSubclass(ObjectMapperProvider.class)
                            .newInstance().provide();
                } catch (InstantiationException e) {
                    throw new IllegalArgumentException("Unable to instantiate ObjectMapperProvider", e);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("Unable to instantiate ObjectMapperProvider", e);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Configured ObjectMapperProvider not found: " + objectMapperProvider, e);
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException(
                            "Configured ObjectMapperProvider is not of type ObjectMapperProvider: " + objectMapperProvider, e);
                }
            }
        }

        @Override
        public void onStop() {
            objectMapper = null;
        }
    }
}