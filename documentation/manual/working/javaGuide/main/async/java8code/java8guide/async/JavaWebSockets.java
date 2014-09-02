/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package java8guide.async;

//#imports
import play.mvc.WebSocket;
//#imports

import com.fasterxml.jackson.databind.JsonNode;
import javaguide.async.MyWebSocketActor;
import play.mvc.Controller;

public class JavaWebSockets {

    public static class ActorController1 {

        //#actor-accept
        public static WebSocket<String> socket() {
            return WebSocket.withActor(MyWebSocketActor::props);
        }
        //#actor-accept
    }

    public static class ActorController2 extends Controller {
        //#actor-reject
        public static WebSocket<String> socket() {
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
        public static WebSocket<JsonNode> socket() {
            return WebSocket.withActor(MyWebSocketActor::props);
        }
        //#actor-json
    }

    // No simple way to test websockets yet

    public static class Controller1 {
        //#websocket
        public static WebSocket<String> socket() {
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
        public static WebSocket<String> socket() {
            return WebSocket.whenReady((in, out) -> {
                out.write("Hello!");
                out.close();
            });
        }
        //#discard-input
    }
}
