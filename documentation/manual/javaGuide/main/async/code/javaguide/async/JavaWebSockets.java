/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.async;

//#imports
import akka.actor.*;
import play.libs.F.*;
import play.mvc.WebSocket;
//#imports

import play.mvc.Controller;

import java.io.Closeable;
import com.fasterxml.jackson.databind.JsonNode;

public class JavaWebSockets {

    public static class ActorController1 {

        //#actor-accept
        public static WebSocket<String> socket() {
            return WebSocket.withActor(new Function<ActorRef, Props>() {
                public Props apply(ActorRef out) throws Throwable {
                    return MyWebSocketActor.props(out);
                }
            });
        }
        //#actor-accept
    }

    public static class Actor1 extends UntypedActor {
        private final Closeable someResource;

        public Actor1(Closeable someResource) {
            this.someResource = someResource;
        }

        public void onReceive(Object message) throws Exception {
        }

        //#actor-post-stop
        public void postStop() throws Exception {
            someResource.close();
        }
        //#actor-post-stop
    }

    public static class Actor2 extends UntypedActor {
        public void onReceive(Object message) throws Exception {
        }

        {
            //#actor-stop
            self().tell(PoisonPill.getInstance(), self());
            //#actor-stop
        }
    }

    public static class ActorController2 extends Controller {
        //#actor-reject
        public static WebSocket<String> socket() {
            if (session().get("user") != null) {
                return WebSocket.withActor(new Function<ActorRef, Props>() {
                    public Props apply(ActorRef out) throws Throwable {
                        return MyWebSocketActor.props(out);
                    }
                });
            } else {
                return WebSocket.reject(forbidden());
            }
        }
        //#actor-reject
    }

    public static class ActorController4 extends Controller {
        //#actor-json
        public static WebSocket<JsonNode> socket() {
            return WebSocket.withActor(new Function<ActorRef, Props>() {
                public Props apply(ActorRef out) throws Throwable {
                    return MyWebSocketActor.props(out);
                }
            });
        }
        //#actor-json
    }

    // No simple way to test websockets yet

    public static class Controller1 {
        //#websocket
        public static WebSocket<String> socket() {
            return new WebSocket<String>() {

                // Called when the Websocket Handshake is done.
                public void onReady(WebSocket.In<String> in, WebSocket.Out<String> out) {

                    // For each event received on the socket,
                    in.onMessage(new Callback<String>() {
                        public void invoke(String event) {

                            // Log events to the console
                            System.out.println(event);

                        }
                    });

                    // When the socket is closed.
                    in.onClose(new Callback0() {
                        public void invoke() {

                            System.out.println("Disconnected");

                        }
                    });

                    // Send a single 'Hello!' message
                    out.write("Hello!");

                }

            };
        }
        //#websocket
    }

    public static class Controller2 {
        //#discard-input
        public static WebSocket<String> socket() {
            return new WebSocket<String>() {

                public void onReady(WebSocket.In<String> in, WebSocket.Out<String> out) {
                    out.write("Hello!");
                    out.close();
                }

            };
        }
        //#discard-input
    }
}
