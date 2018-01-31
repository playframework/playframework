/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
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
 *     //private final DateTimeFormatter df = DateTimeFormatter.ofPattern("HH mm ss");
 *
 *     public Result liveClock() {
 *         Source&lt;String, ?&gt; tickSource = Source.tick(Duration.Zero(), Duration.create(100, MILLISECONDS), "TICK");
 *         Source&lt;EventSource.Event, ?&gt; eventSource = tickSource.map((tick) -&gt; EventSource.Event.event(df.format(ZonedDateTime.now())));
 *         return ok().chunked(eventSource.via(EventSource.flow())).as(Http.MimeTypes.EVENT_STREAM);
 *     }
 * }}}
 */
public class EventSource {

    /**
     * @return a flow of EventSource.Event to ByteString.
     */
    public static Flow<EventSource.Event, ByteString, ?> flow() {
        Flow<Event, Event, NotUsed> flow = Flow.of(Event.class);
        return flow.map((EventSource.Event event) -> ByteString.fromString(event.formatted()));
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
