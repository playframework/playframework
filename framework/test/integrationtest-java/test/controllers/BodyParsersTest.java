package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import play.libs.Json;
import play.libs.WS;

import java.util.concurrent.atomic.AtomicReference;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.*;

public class BodyParsersTest {
    @Test
    public void testJson() {
        JsonNode json = createJson(100);
        WS.Response response = runJsonTest(json, "/parsers/json");
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat((Object) response.asJson()).isEqualTo(json);
    }

    @Test
    public void testJsonExceedsDefaultLength() {
        JsonNode json = createJson(110 * 1024);
        WS.Response response = runJsonTest(json, "/parsers/json");
        assertThat(response.getStatus()).isEqualTo(413);
    }

    @Test
    public void testLimitedJson() {
        JsonNode json = createJson(100);
        WS.Response response = runJsonTest(json, "/parsers/limitedjson");
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat((Object) response.asJson()).isEqualTo(json);
    }

    @Test
    public void testLimitedJsonExceedsDefaultLengthButLessThanLimit() {
        JsonNode json = createJson(110 * 1024);
        WS.Response response = runJsonTest(json, "/parsers/limitedjson");
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat((Object) response.asJson()).isEqualTo(json);
    }

    @Test
    public void testLimitedJsonExceedsLimit() {
        JsonNode json = createJson(130 * 1024);
        WS.Response response = runJsonTest(json, "/parsers/limitedjson");
        assertThat(response.getStatus()).isEqualTo(413);
    }

    private WS.Response runJsonTest(final JsonNode json, final String url) {
        final AtomicReference<WS.Response> response = new AtomicReference<WS.Response>();
        running(testServer(9001), new Runnable() {
            @Override
            public void run() {
                WS.Response r = WS.url("http://localhost:9001" + url).setHeader("Content-Type", "application/json")
                        .post(Json.stringify(json)).get();
                r.getBody();
                response.set(r);
            }
        });
        return response.get();
    }

    private static JsonNode createJson(int length) {
        StringBuilder sb = new StringBuilder(length);
        String text = "The quick brown fox jumps over the lazy dog. Why? I don't know. I guess it just uses every letter.";
        while (length > 0) {
            int toAppend = Math.min(length, text.length());
            sb.append(text.substring(0, toAppend));
            length -= toAppend;
        }
        ObjectNode json = Json.newObject();
        json.put("string", sb.toString());
        return json;
    }
}
