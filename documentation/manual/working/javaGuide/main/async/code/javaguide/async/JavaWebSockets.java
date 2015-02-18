/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
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
            return WebSocket.withActor(MyWebSocketActor::props);
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
        public WebSocket<String> socket() {
            if (session().get("user") != null) {
                return WebSocket.withActor(MyWebSocketActor::props);
            } else {
                return WebSocket.reject(forbidden());
            }
        }
        //#actor-reject
    }

    public static class ActorController4 extends Controller {
        //#actor-json
        public WebSocket<JsonNode> socket() {
            return WebSocket.withActor(MyWebSocketActor::props);
        }
        //#actor-json
    }

    // No simple way to test websockets yet

    public static class Controller1 {
        //#websocket
        public WebSocket<String> socket() {
            return WebSocket.whenReady((in, out) -> {
                // For each event received on the socket,
                in.onMessage(System.out::println);

                // When the socket is closed.
                in.onClose(() -> System.out.println("Disconnected"));

                // Send a single 'Hello!' message
                out.write("Hello!");
            });
        }
        //#websocket
    }

    public static class Controller2 {
        //#discard-input
        public WebSocket<String> socket() {
            return WebSocket.whenReady((in, out) -> {
                out.write("Hello!");
                out.close();
            });
        }
        //#discard-input
    }
}
