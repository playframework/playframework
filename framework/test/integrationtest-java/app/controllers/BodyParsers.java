/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package controllers;

import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

public class BodyParsers extends Controller {
    @BodyParser.Of(BodyParser.Json.class)
    public static Result json() {
        if (request().body().isMaxSizeExceeded()) {
            return status(413);
        } else {
            return ok(request().body().asJson());
        }
    }

    @BodyParser.Of(value = BodyParser.Json.class, maxLength = 120 * 1024)
    public static Result limitedJson() {
        return json();
    }

    @BodyParser.Of(BodyParser.Empty.class)
    public static Result empty() {
        Http.RequestBody body = request().body();
        String bodyConversions =
            "multipartFormData: " + body.asMultipartFormData() + ", " +
            "formUrlEncoded: " + body.asFormUrlEncoded() + ", " +
            "raw: " + body.asRaw() + ", " +
            "text: " + body.asText() + ", " +
            "xml: " + body.asXml() + ", " +
            "json: " + body.asJson();
        return ok(bodyConversions);
    }

    public static class ThreadName implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return TestBodyParsers.threadName();
        }
    }

    @BodyParser.Of(ThreadName.class)
    public static Result thread() {
        Http.RequestBody body = request().body();
        String threadName = body.asText();
        return ok(threadName);
    }

}
