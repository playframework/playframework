/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package java8guide.async;

import play.libs.F.*;
import play.mvc.WebSocket;

public class JavaWebSockets {

    public static class Controller1 {
        //#websocket
        public static WebSocket<String> index() {
            return WebSocket.whenReady((in, out) -> {
                // For each event received on the socket
                in.onMessage(System.out::println);

                // When the socket is closed
                in.onClose(() -> System.out.println("Disconnected"));

                // Send a single 'Hello!' message
                out.write("Hello!");
            });
        }
        //#websocket
    }

    public static class Controller2 {
        //#discard-input
        public static WebSocket<String> index() {
            return WebSocket.whenReady((in, out) -> {
                out.write("Hello!");
                out.close();
            });
        }
        //#discard-input
    }
}
