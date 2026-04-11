/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.util.ByteString;

/**
 * This class provides an easy way to use Server Sent Events (SSE) as a chunked encoding, using an
 * Pekko Source.
 *
 * <p>Please see the <a
 * href="https://html.spec.whatwg.org/multipage/server-sent-events.html">Server-Sent Events
 * specification</a> for details.
 *
 * <p>Example implementation of EventSource in a Controller:
 *
 * <p>{{{ //import org.apache.pekko.stream.javadsl.Source; //import play.mvc.*; //import
 * play.libs.*; //import java.time.ZonedDateTime; //import java.time.format.*; //import
 * scala.concurrent.duration.Duration; //import static java.util.concurrent.TimeUnit.*; //import
 * static play.libs.EventSource.Event.event; //private final DateTimeFormatter df =
 * DateTimeFormatter.ofPattern("HH mm ss");
 *
 * <p>public Result liveClock() { Source&lt;String, ?&gt; tickSource = Source.tick(Duration.Zero(),
 * Duration.create(100, MILLISECONDS), "TICK"); Source&lt;EventSource.Event, ?&gt; eventSource =
 * tickSource.map((tick) -&gt; EventSource.Event.event(df.format(ZonedDateTime.now()))); return
 * ok().chunked(eventSource.via(EventSource.flow())).as(Http.MimeTypes.EVENT_STREAM); } }}}
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
   * @return a flow of keep-alive messages.
   */
  public static Flow<EventSource.Event, EventSource.Event, ?> keepAlive(Duration duration) {
    var keepAliveEvent = new EventSource.Event(null, null, null, "");
    return Flow.of(Event.class).keepAlive(duration, () -> keepAliveEvent);
  }

  /** Utility class to build events. */
  public static class Event {

    private final String name;
    private final String id;
    private final String data;
    private final String comment;

    public Event(String data, String id, String name, String comment) {
      this.name = name;
      this.id = id;
      this.data = data;
      this.comment = comment;
    }

    public Event(String data, String id, String name) {
      this(data, id, name, null);
    }

    /**
     * @param name Event name
     * @return A copy of this event, with name {@code name}
     */
    public Event withName(String name) {
      return new Event(this.data, this.id, name, this.comment);
    }

    /**
     * @param id Event id
     * @return A copy of this event, with id {@code id}.
     */
    public Event withId(String id) {
      return new Event(this.data, id, this.name, this.comment);
    }

    /**
     * @param comment Event comment
     * @return A copy of this event, with comment {@code comment}
     */
    public Event withComment(String comment) {
      return new Event(this.data, this.id, this.name, comment);
    }

    /**
     * @return This event formatted according to the EventSource protocol.
     */
    public String formatted() {
      return new play.api.libs.EventSource.Event(
              Scala.Option(data), Scala.Option(id), Scala.Option(name), Scala.Option(comment))
          .formatted();
    }

    /**
     * @param data Event content
     * @return An event with {@code data} as content
     */
    public static Event event(String data) {
      return new Event(data, null, null, null);
    }

    /**
     * @param json Json value to use
     * @return An event with a string representation of {@code json} as content
     */
    public static Event event(JsonNode json) {
      return new Event(Json.stringify(json), null, null, null);
    }
  }
}
