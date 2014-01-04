/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.http.routing.controllers;

import play.mvc.Controller;
import play.mvc.Result;

public class Application extends Controller {

    public static Result download(String path) {
        return ok("download " + path);
    }

    public static Result homePage() {
        return ok("home page");
    }

    //#show-page-action
    public static Result show(String page) {
        String content = Page.getContentOf(page);
        response().setContentType("text/html");
        return ok(content);
    }
    //#show-page-action

    static class Page {
        static String getContentOf(String page) {
            return "showing page " + page;
        }
    }

    //#reverse-redirect
    // Redirect to /hello/Bob
    public static Result index() {
        return redirect(controllers.routes.Application.hello("Bob"));
    }
    //#reverse-redirect

    static ReverseControllers controllers = new ReverseControllers();
    static class ReverseControllers {
        javaguide.http.routing.reverse.controllers.routes routes = new javaguide.http.routing.reverse.controllers.routes();
    }
}
