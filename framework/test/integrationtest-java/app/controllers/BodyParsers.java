package controllers;

import play.mvc.BodyParser;
import play.mvc.Controller;
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
}
