package javaguide.async;

import play.libs.F.*;
import play.mvc.WebSocket;

public class JavaWebSockets {

    // No simple way to test websockets yet

    public static class Controller1 {
        //#websocket
        public static WebSocket<String> index() {
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
        public static WebSocket<String> index() {
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
