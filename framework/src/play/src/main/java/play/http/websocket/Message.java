/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.http.websocket;

import akka.util.ByteString;

import java.util.Optional;

/**
 * A WebSocket message.
 */
public abstract class Message {

    // private constructor to seal it
    private Message() {
    }

    /**
     * A text WebSocket message
     */
    public static class Text extends Message {
        private final String data;

        public Text(String data) {
            this.data = data;
        }

        public String data() {
            return data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Text text = (Text) o;

            return data.equals(text.data);

        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "TextWebSocketMessage('" + data + "')";
        }
    }

    /**
     * A binary WebSocket message
     */
    public static class Binary extends Message {
        private final ByteString data;

        public Binary(ByteString data) {
            this.data = data;
        }

        public ByteString data() {
            return data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Binary binary = (Binary) o;

            return data.equals(binary.data);

        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "BinaryWebSocketMessage('" + data + "')";
        }
    }

    /**
     * A ping WebSocket message
     */
    public static class Ping extends Message {
        private final ByteString data;

        public Ping(ByteString data) {
            this.data = data;
        }

        public ByteString data() {
            return data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Ping ping = (Ping) o;

            return data.equals(ping.data);

        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "PingWebSocketMessage('" + data + "')";
        }
    }

    /**
     * A pong WebSocket message
     */
    public static class Pong extends Message {
        private final ByteString data;

        public Pong(ByteString data) {
            this.data = data;
        }

        public ByteString data() {
            return data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Pong pong = (Pong) o;

            return data.equals(pong.data);

        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "PongWebSocketMessage('" + data + "')";
        }
    }

    /**
     * A close WebSocket message
     */
    public static class Close extends Message {
        private final Optional<Integer> statusCode;
        private final String reason;

        public Close(int statusCode) {
            this(statusCode, "");
        }

        public Close(int statusCode, String reason) {
            this(Optional.of(statusCode), reason);
        }

        public Close(Optional<Integer> statusCode, String reason) {
            this.statusCode = statusCode;
            this.reason = reason;
        }

        public Optional<Integer> code() {
            return statusCode;
        }

        public String reason() {
            return reason;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Close close = (Close) o;

            return statusCode.equals(close.statusCode) && reason.equals(close.reason);
        }

        @Override
        public int hashCode() {
            int result = statusCode.hashCode();
            result = 31 * result + reason.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "CloseWebSocketMessage(" + statusCode + ", '" + reason + "')";
        }
    }


}
