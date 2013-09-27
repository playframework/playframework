package javaguide.ws;

import javaguide.testhelpers.MockJavaAction;

// #ws-imports
import play.libs.WS;
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
            Promise<WS.Response> homePage = WS.url("http://example.com").get();
            // #get-call
            // #get-call-and-recover
            Promise<WS.Response> callWithRecover = homePage.recover(new Function<Throwable, WS.Response>() {
                @Override
                public WS.Response apply(Throwable throwable) throws Throwable {
                    return WS.url("http://backup.example.com").get().get(timeout);
                }
            });
            // #get-call-and-recover
        }

        public static void postCall() {
            // #post-call
            Promise<WS.Response> result = WS.url("http://example.com").post("content");
            // #post-call
        }

        // #simple-call
        public static Promise<Result> index() {
            final Promise<Result> resultPromise = WS.url(feedUrl).get().map(
                    new Function<WS.Response, Result>() {
                        public Result apply(WS.Response response) {
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
                    new Function<WS.Response, Promise<Result>>() {
                        public Promise<Result> apply(WS.Response response) {
                            return WS.url(response.asJson().findPath("commentsUrl").asText()).get().map(
                                    new Function<WS.Response, Result>() {
                                        public Result apply(WS.Response response) {
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
