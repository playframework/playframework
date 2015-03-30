/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs;

import com.fasterxml.jackson.databind.JsonNode;
import play.mvc.Results.*;

/**
 * Implementation of Server-Sent Events.
 * @see <a href="http://dev.w3.org/html5/eventsource/">Server-Sent Events specification</a>
 */
public abstract class EventSource extends Chunks<String> {
    private Chunks.Out<String> out;

    /**
     * Create a new EventSource socket
     *
     */
    public EventSource() {
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
    public void onDisconnected(F.Callback0 callback) {
        out.onDisconnected(callback);
    }

    /**
     * Close the channel
     */
    public void close() {
        out.close();
    }

    /**
     * Creates an EventSource. The abstract {@code onConnected} method is
     * implemented using the specified {@code F.Callback<EventSource>} and
     * is invoked with {@code EventSource.this}.
     *
     * @param callback the callback used to implement onConnected
     * @return a new EventSource
     * @throws NullPointerException if the specified callback is null
     */
    public static EventSource whenConnected(F.Callback<EventSource> callback) {
        return new WhenConnectedEventSource(callback);
    }

    /**
     * An extension of EventSource that obtains its onConnected from
     * the specified {@code F.Callback<EventSource>}.
     */
    static final class WhenConnectedEventSource extends EventSource {

        private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WhenConnectedEventSource.class);

        private final F.Callback<EventSource> callback;

        WhenConnectedEventSource(F.Callback<EventSource> callback) {
            super();
            if (callback == null) throw new NullPointerException("EventSource onConnected callback cannot be null");
            this.callback = callback;
        }

        @Override
        public void onConnected() {
            try {
                callback.invoke(this);
            } catch (Throwable e) {
                logger.error("Exception in EventSource.onConnected", e);
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
         * @return This event formatted according to the EventSource protocol.
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
