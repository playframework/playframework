/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringEscapeUtils;
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
     * A single event source can generate different types events by including an event name. On the client, an event listener can be
     * setup to listen to that particular event.
     *
     * @param eventName Unique name of the event.
     * @param data data associated with event
     * @deprecated Replaced by send
     * @see #send(play.libs.EventSource.Event)
     */
    @Deprecated
    public void sendDataByName(String eventName, String data) {
        out.write("event: " + eventName + "\r\n"
                + "data: " + StringEscapeUtils.escapeEcmaScript(data) + "\r\n\r\n");
    }

    /**
     * Setting an ID lets the browser keep track of the last event fired so that if, the connection to the server is dropped,
     * a special HTTP header (Last-Event-ID) is set with the new request. This lets the browser determine which event is
     * appropriate to fire.
     *
     * @param eventId Unique event id.
     * @param data data associated with event
     * @deprecated Replaced by send
     * @see #send(play.libs.EventSource.Event)
     */
    @Deprecated
    public void sendDataById(String eventId, String data) {
        out.write("id: " + eventId + "\r\n"
                + "data: " + StringEscapeUtils.escapeEcmaScript(data) + "\r\n\r\n");

    }

    /**
     * Sending a generic event. On the client, 'message' event listener can be setup to listen to this event.
     *
     * @param data data associated with event
     * @deprecated Replaced by send
     * @see #send(play.libs.EventSource.Event)
     */
    @Deprecated
    public void sendData(String data) {
        out.write("data: " + StringEscapeUtils.escapeEcmaScript(data) + "\r\n\r\n");
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
