/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs;

import com.fasterxml.jackson.databind.JsonNode;
import play.mvc.Http.MimeTypes;
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
 * The response will have content type "application/javascript" and will look like the following:
 * <pre>
 *   foo({...});
 * </pre>
 */
public class Jsonp implements Content {

    public Jsonp(String padding, JsonNode json) {
        this.padding = padding;
        this.json = json;
    }

    @Override
    public String body() {
        return padding + "(" + Json.stringify(json) + ");";
    }

    @Override
    public String contentType() {
        return MimeTypes.JAVASCRIPT;
    }

    private final String padding;
    private final JsonNode json;

    /**
     * @param padding Name of the callback
     * @param json Json content
     * @return A JSONP Content using padding and json.
     */
    public static Jsonp jsonp(String padding, JsonNode json) {
        return new Jsonp(padding, json);
    }

}
