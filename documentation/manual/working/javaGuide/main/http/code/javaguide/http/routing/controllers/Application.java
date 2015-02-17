/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.http.routing.controllers;

import play.mvc.Controller;
import play.mvc.Result;

public class Application extends Controller {

    public Result download(String path) {
        return ok("download " + path);
    }

    public Result homePage() {
        return ok("home page");
    }

    //#show-page-action
    public Result show(String page) {
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
    public Result index() {
        return redirect(controllers.routes.Application.hello("Bob"));
    }
    //#reverse-redirect

    static class controllers {
        static javaguide.http.routing.reverse.controllers.routes routes = new javaguide.http.routing.reverse.controllers.routes();
    }
}
