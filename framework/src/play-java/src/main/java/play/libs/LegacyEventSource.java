/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs;

import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import play.mvc.Results.*;

/**
 * This class is deprecated.  Please use play.libs.EventSource with an Akka Source.
 */
@Deprecated
public abstract class LegacyEventSource extends Chunks<String> {
    private Chunks.Out<String> out;

    /**
     * Create a new LegacyEventSource socket
     *
     */
    public LegacyEventSource() {
        super(play.core.j.JavaResults.writeString("text/event-stream", play.api.mvc.Codec.javaSupported("utf-8")));
    }

    public void onReady(Chunks.Out<String> out) {
        this.out = out;
        onConnected();
    }

    /**
     * Send an event. On the client, a 'message' event listener can be setup to listen to this event.
     *
     * @param event Event content
     */
    public void send(Event event) {
        out.write(event.formatted());
    }

    /**
     * The socket is ready, you can start sending messages.
     */
    public abstract void onConnected();

    /**
     * Add a callback to be notified when the client has disconnected.
     */
    public void onDisconnected(Runnable callback) {
        out.onDisconnected(callback);
    }

    /**
     * Close the channel
     */
    public void close() {
        out.close();
    }

    /**
     * Creates an LegacyEventSource. The abstract {@code onConnected} method is
     * implemented using the specified {@code F.Callback<LegacyEventSource>} and
     * is invoked with {@code LegacyEventSource.this}.
     *
     * @param callback the callback used to implement onConnected
     * @return a new LegacyEventSource
     * @throws NullPointerException if the specified callback is null
     */
    public static LegacyEventSource whenConnected(Consumer<LegacyEventSource> callback) {
        return new WhenConnectedEventSource(callback);
    }

    /**
     * An extension of LegacyEventSource that obtains its onConnected from
     * the specified {@code F.Callback<LegacyEventSource>}.
     */
    static final class WhenConnectedEventSource extends LegacyEventSource {

        private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WhenConnectedEventSource.class);

        private final Consumer<LegacyEventSource> callback;

        WhenConnectedEventSource(Consumer<LegacyEventSource> callback) {
            super();
            if (callback == null) throw new NullPointerException("LegacyEventSource onConnected callback cannot be null");
            this.callback = callback;
        }

        @Override
        public void onConnected() {
            try {
                callback.accept(this);
            } catch (Throwable e) {
                logger.error("Exception in LegacyEventSource.onConnected", e);
            }
        }
    }

    /**
     * Utility class to build events.
     */
    public static class Event {

        private final String name;
        private final String id;
        private final String data;

        public Event(String data, String id, String name) {
            this.name = name;
            this.id = id;
            this.data = data;
        }

        /**
         * @param name Event name
         * @return A copy of this event, with name {@code name}
         */
        public Event withName(String name) {
            return new Event(this.data, this.id, name);
        }

        /**
         * @param id Event id
         * @return A copy of this event, with id {@code id}.
         */
        public Event withId(String id) {
            return new Event(this.data, id, this.name);
        }

        /**
         * @return This event formatted according to the LegacyEventSource protocol.
         */
        public String formatted() {
            return new play.api.libs.EventSource.Event(data, Scala.Option(id), Scala.Option(name)).formatted();
        }

        /**
         * @param data Event content
         * @return An event with {@code data} as content
         */
        public static Event event(String data) {
            return new Event(data, null, null);
        }

        /**
         * @param json Json value to use
         * @return An event with a string representation of {@code json} as content
         */
        public static Event event(JsonNode json) {
            return new Event(Json.stringify(json), null, null);
        }

    }

}
