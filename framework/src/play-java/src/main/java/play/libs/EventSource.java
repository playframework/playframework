/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs;

import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * This class provides an easy way to use Server Sent Events (SSE) as a chunked encoding, using an Akka Source.
 *
 * Please see the <a href="http://dev.w3.org/html5/eventsource/">Server-Sent Events specification</a> for details.
 *
 * Example implementation of EventSource in a Controller:
 *
 * {{{
 *     //import akka.stream.javadsl.Source;
 *     //import play.mvc.*;
 *     //import play.libs.*;
 *     //import java.time.ZonedDateTime;
 *     //import java.time.format.*;
 *     //import scala.concurrent.duration.Duration;
 *     //import static java.util.concurrent.TimeUnit.*;
 *     //import static play.libs.EventSource.Event.event;
 *
 *     public Result liveClock() {
 *         Source&lt;String, ?&gt; tickSource = Source.from(Duration.Zero(), Duration.create(100, MILLISECONDS), "TICK");
 *         Source&lt;EventSource.Event, ?&gt; eventSource = tickSource.map((tick) -&gt; event(df.format(ZonedDateTime.now())));
 *         return ok().chunked(EventSource.chunked(eventSource)).as("text/event-stream");
 *     }
 * }}}
 */
public class EventSource {

    /**
     * Maps from a Source of String to a Source of EventSource.Event.
     *
     * Useful when id and name are not required.
     *
     * @param source input event source.
     * @return a Source of EventSource.Event.
     */
    public static Source<EventSource.Event, ?> apply(Source<String, ?> source) {
        return source.map(Event::event);
    }

    /**
     * Maps from a Source of EventSource.Event to a Source of ByteString.
     *
     * @param source input event source.
     * @return a Source of ByteStream.
     */
    public static Source<ByteString, ?> chunked(Source<EventSource.Event, ?> source) {
        return source.map(event -> ByteString.fromString(event.formatted()));
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
