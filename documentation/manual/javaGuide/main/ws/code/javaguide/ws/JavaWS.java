/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.ws;

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
            final int timeout = 5000;
            // #get-call
            Promise<WSResponse> homePage = WS.url("http://example.com").get();
            // #get-call
            // #get-call-and-recover
            Promise<WSResponse> callWithRecover = homePage.recover(new Function<Throwable, WSResponse>() {
                @Override
                public WSResponse apply(Throwable throwable) throws Throwable {
                    return WS.url("http://backup.example.com").get().get(timeout);
                }
            });
            // #get-call-and-recover
        }

        public static void postCall() {
            // #post-call
            Promise<WSResponse> result = WS.url("http://example.com").post("content");
            // #post-call
        }

        // #simple-call
        public static Promise<Result> index() {
            final Promise<Result> resultPromise = WS.url(feedUrl).get().map(
                    new Function<WSResponse, Result>() {
                        public Result apply(WSResponse response) {
                            return ok("Feed title:" + response.asJson().findPath("title"));
                        }
                    }
            );
            return resultPromise;
        }
        // #simple-call
    }

    public static class Controller2 extends MockJavaAction {

        // #composed-call
        public static Promise<Result> index() {
            final Promise<Result> resultPromise = WS.url(feedUrl).get().flatMap(
                    new Function<WSResponse, Promise<Result>>() {
                        public Promise<Result> apply(WSResponse response) {
                            return WS.url(response.asJson().findPath("commentsUrl").asText()).get().map(
                                    new Function<WSResponse, Result>() {
                                        public Result apply(WSResponse response) {
                                            return ok("Number of comments: " + response.asJson().findPath("count").asInt());
                                        }
                                    }
                            );
                        }
                    }
            );
            return resultPromise;
        }
        // #composed-call
    }


}
