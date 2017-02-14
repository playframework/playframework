/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import play.twirl.api.Content;

/**
 * The JSONP Content renders a JavaScript call of a JSON object.<br>
 * Example of use, provided the following route definition:
 * <pre>
 *   GET  /my-service        Application.myService(callback: String)
 * </pre>
 * The following action definition:
 * <pre>
 *   public static Result myService(String callback) {
 *     JsonNode json = ...
 *     return ok(jsonp(callback, json));
 *   }
 * </pre>
 * And the following request:
 * <pre>
 *   GET  /my-service?callback=foo
 * </pre>
 * The response will have content type "text/javascript" and will look like the following:
 * <pre>
 *   foo({...});
 * </pre>
 */
public class Jsonp implements Content {

    // To be valid JS we use ESCAPE_NON_ASCII here
    private ObjectWriter writer = new ObjectMapper().writer().with(Feature.ESCAPE_NON_ASCII);

    private final String padding;
    private final JsonNode json;

    public Jsonp(String padding, JsonNode json) {
        this.padding = padding;
        this.json = json;
    }

    @Override
    public String body() {
        try {
            return padding + "(" + writer.writeValueAsString(json) + ");";
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error writing JSONP", e);
        }
    }

    @Override
    public String contentType() {
        return "text/javascript";
    }

    /**
     * @param padding Name of the callback
     * @param json Json content
     * @return A JSONP Content using padding and json.
     */
    public static Jsonp jsonp(String padding, JsonNode json) {
        return new Jsonp(padding, json);
    }

}
