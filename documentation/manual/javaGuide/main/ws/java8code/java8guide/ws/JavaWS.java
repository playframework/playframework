/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package java8guide.ws;

import javaguide.testhelpers.MockJavaAction;

// #ws-imports
import play.libs.ws.*;
import play.mvc.Result;

import static play.libs.F.Function;
import static play.libs.F.Promise;
// #ws-imports

public class JavaWS {
    private static String feedUrl = "http://localhost:3333/feed";

    public static class Controller1 extends MockJavaAction {

        public static void getCallAndRecover() {
            // #get-call
            Promise<WSResponse> homePage = WS.url("http://example.com").get();
            // #get-call
            // #get-call-and-recover
            Promise<WSResponse> callWithRecover = homePage.recoverWith(throwable ->
                WS.url("http://backup.example.com").get()
            );
            // #get-call-and-recover
        }

        public static void postCall() {
            // #post-call
            Promise<WSResponse> result = WS.url("http://example.com").post("content");
            // #post-call
        }

        // #simple-call
        public static Promise<Result> index() {
            return WS.url(feedUrl).get().map(response ->
                ok("Feed title: " + response.asJson().findPath("title").asText())
            );
        }
        // #simple-call
    }

    public static class Controller2 extends MockJavaAction {

        // #composed-call
        public static Promise<Result> index() {
            return WS.url(feedUrl).get()
                     .flatMap(response -> WS.url(response.asJson().findPath("commentsUrl").asText()).get())
                     .map(response -> ok("Number of comments: " + response.asJson().findPath("count").asInt()));
        }
        // #composed-call
    }


}
