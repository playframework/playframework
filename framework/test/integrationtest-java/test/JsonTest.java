package test;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.Module;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.module.SimpleModule;
import org.codehaus.jackson.map.module.SimpleSerializers;
import org.junit.Test;
import play.libs.Json;
import play.test.*;

import java.io.IOException;
import java.lang.Override;
import java.lang.String;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static play.test.Helpers.*;

public class JsonTest {
    @Test
    public void noApp() {
        assertThat(Json.parse("{\"foo\":\"bar\"}").get("foo").getTextValue(), equalTo("bar"));
    }

    @Test
    public void inApp() {
        running(fakeApplication(), new Runnable() {
            public void run() {
                assertThat(Json.parse("{\"foo\":\"bar\"}").get("foo").getTextValue(), equalTo("bar"));
            }
        });
    }

    @Test
    public void inAppWithCustomObjectMapper() {
        Map<String, String> config = new HashMap<String, String>();
        config.put("json.objectMapperProvider", MockObjectMapperProvider.class.getName());
        running(fakeApplication(), new Runnable() {
            public void run() {
                assertThat(Json.toJson(new MockObject()).getTextValue(), equalTo("mockobject"));
            }
        });
    }

    public static class MockObjectMapperProvider implements Json.ObjectMapperProvider {
        @Override
        public ObjectMapper provide() {
            Module module = new Module() {
                @Override
                public String getModuleName() {
                    return "mock";
                }

                @Override
                public Version version() {
                    return null;
                }

                @Override
                public void setupModule(SetupContext context) {
                    SimpleSerializers serializers = new SimpleSerializers();
                    serializers.addSerializer(MockObject.class, new MockObjectSerializer());
                    context.addSerializers(serializers);
                }
            };
            return new ObjectMapper().withModule(module);
        }
    }

    public static class MockObjectSerializer extends JsonSerializer<MockObject> {
        @Override
        public void serialize(test.JsonTest.MockObject value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
            jgen.writeString("mockobject");
        }
    }

    public static class MockObject {
        public String foo;
    }
}