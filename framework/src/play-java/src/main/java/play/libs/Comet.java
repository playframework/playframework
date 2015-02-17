/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs;

import play.mvc.Results.*;

import play.libs.F.*;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

/**
 * A Chunked stream sending Comet messages.
 */
public abstract class Comet extends Chunks<String> {

    private Chunks.Out<String> out;
    private String callbackMethod;

    /**
     * Create a new Comet socket
     *
     * @param callbackMethod The Javascript callback method to call on each message.
     */
    public Comet(String callbackMethod) {
        super(play.core.j.JavaResults.writeString("text/html", play.api.mvc.Codec.javaSupported("utf-8")));
        this.callbackMethod = callbackMethod;
    }

    public void onReady(Chunks.Out<String> out) {
        this.out = out;
        out.write(initialBuffer());
        onConnected();
    }

    /**
     * Initial chunk of data to send for browser compatibility (default to send 5Kb of blank data).
     */
    protected String initialBuffer() {
        char[] buffer = new char[1024 * 5];
        Arrays.fill(buffer, ' ');
        return new String(buffer);
    }

    /**
     * Send a message on this socket (will be received as String in the Javascript callback method).
     */
    public void sendMessage(String message) {
        out.write("<script type=\"text/javascript\">" + callbackMethod + "('" + org.apache.commons.lang3.StringEscapeUtils.escapeEcmaScript(message) + "');</script>");
    }

    /**
     * Send a Json message on this socket (will be received as Json in the Javascript callback method).
     */
    public void sendMessage(JsonNode message) {
        out.write("<script type=\"text/javascript\">" + callbackMethod + "(" + Json.stringify(message) + ");</script>");
    }

    /**
     * The socket is ready, you can start sending messages.
     */
    public abstract void onConnected();

    /**
     * Add a callback to be notified when the client has disconnected.
     */
    public void onDisconnected(Callback0 callback) {
        out.onDisconnected(callback);
    }

    /**
     * Close the channel
     */
    public void close() {
        out.close();
    }

    /**
     * Creates a Comet. The abstract {@code onConnected} method is
     * implemented using the specified {@code Callback<Comet>} and
     * is invoked with {@code Comet.this}.
     *
     * @param jsMethod the Javascript method to call on each message
     * @param callback the callback used to implement onConnected
     * @return a new Comet
     * @throws NullPointerException if the specified callback is null
     */
    public static Comet whenConnected(String jsMethod, Callback<Comet> callback) {
        return new WhenConnectedComet(jsMethod, callback);
    }

    /**
     * An extension of Comet that obtains its onConnected from
     * the specified {@code Callback<Comet>}.
     */
    static final class WhenConnectedComet extends Comet {

        private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Comet.class);

        private final Callback<Comet> callback;

        WhenConnectedComet(String jsMethod, Callback<Comet> callback) {
            super(jsMethod);
            if (callback == null) throw new NullPointerException("Comet onConnected callback cannot be null");
            this.callback = callback;
        }

        @Override
        public void onConnected() {
            try {
                callback.invoke(this);
            } catch (Throwable e) {
                logger.error("Exception in Comet.onConnected", e);
            }
        }
    }

}
