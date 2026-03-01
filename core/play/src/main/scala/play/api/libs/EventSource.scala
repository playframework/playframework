/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs

import scala.concurrent.duration.FiniteDuration

import org.apache.pekko.stream.scaladsl.Flow
import play.api.http.ContentTypeOf
import play.api.http.ContentTypes
import play.api.http.Writeable
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc._

/**
 * This class provides an easy way to use Server Sent Events (SSE) as a chunked encoding, using an Pekko Source.
 *
 * Please see the <a href="https://html.spec.whatwg.org/multipage/server-sent-events.html">Server-Sent Events specification</a> for details.
 *
 * An example of how to display an event stream:
 *
 * {{{
 *   import java.time.ZonedDateTime
 *   import java.time.format.DateTimeFormatter
 *   import jakarta.inject.Singleton
 *   import org.apache.pekko.stream.scaladsl.Source
 *   import play.api.http.ContentTypes
 *   import play.api.libs.EventSource
 *   import play.api.mvc._
 *
 *   import scala.concurrent.duration._
 *
 *   def liveClock() = Action {
 *     val df: DateTimeFormatter = DateTimeFormatter.ofPattern("HH mm ss")
 *     val tickSource = Source.tick(0 millis, 100 millis, "TICK")
 *     val source = tickSource.map { (tick) =&gt;
 *       df.format(ZonedDateTime.now())
 *     }
 *     Ok.chunked(source via EventSource.flow)
 *   }
 * }}}
 */
object EventSource {

  /**
   * Makes a `Flow[E, Event, _]`, given an input source.
   *
   * Usage example:
   *
   * {{{
   *   val jsonStream: Source[JsValue, Unit] = createJsonSource()
   *   Ok.chunked(jsonStream via EventSource.flow)
   * }}}
   */
  def flow[E: EventDataExtractor: EventNameExtractor: EventIdExtractor]: Flow[E, Event, ?] = {
    Flow[E].map(Event(_))
  }

  /**
   * Makes a SSE keep-alive flow.
   *
   * Usage example:
   *
   * {{{
   *   Ok.chunked(jsonStream via EventSource.flow via EventSource.keepAlive(10.seconds))
   * }}}
   */
  def keepAlive(duration: FiniteDuration): Flow[Event, AbstractEvent, ?] = {
    val keepAliveEvent = new EventBuilder().addComment("")
    Flow[Event].keepAlive(duration, () => keepAliveEvent)
  }

  /**
   * Abstract trait representing an event from an EventSource.
   */
  sealed trait AbstractEvent {
    def formatted: String
  }

  object AbstractEvent {
    implicit def writeable(implicit codec: Codec): Writeable[AbstractEvent] = {
      Writeable(event => codec.encode(event.formatted))
    }

    implicit def contentType(implicit codec: Codec): ContentTypeOf[AbstractEvent] = {
      ContentTypeOf(Some(ContentTypes.EVENT_STREAM))
    }
  }

  // ------------------
  // Event
  // ------------------

  /**
   * An event encoded with the SSE protocol..
   */
  case class Event(data: String, id: Option[String], name: Option[String]) extends AbstractEvent {

    /**
     * This event, formatted according to the EventSource protocol.
     */
    lazy val formatted = {
      val sb = new StringBuilder
      name.foreach(sb.append("event: ").append(_).append('\n'))
      id.foreach(sb.append("id: ").append(_).append('\n'))
      for (line <- data.split("(\r?\n)|\r", -1)) {
        sb.append("data: ").append(line).append('\n')
      }
      sb.append('\n')
      sb.toString()
    }

    def toBuilder: EventBuilder = {
      val withData = new EventBuilder().addData(data)
      val withId   = id.fold(withData)(withData.addId)
      name.fold(withId)(withId.addEvent)
    }
  }

  object Event {

    /**
     * Creates an event from a single input, using implicit extractors to provide raw values.
     *
     * If no extractor is available, the implicit conversion in the low priority traits will be used.
     * For the EventDataExtractor, this means `String` or `JsValue` will be automatically mapped,
     * and the nameExtractor and idExtractor will implicitly resolve to `None`.
     */
    def apply[A](a: A)(
        implicit dataExtractor: EventDataExtractor[A],
        nameExtractor: EventNameExtractor[A],
        idExtractor: EventIdExtractor[A]
    ): Event = {
      Event(dataExtractor.eventData(a), idExtractor.eventId(a), nameExtractor.eventName(a))
    }

    def writeable(implicit codec: Codec): Writeable[Event] = AbstractEvent.writeable

    def contentType(implicit codec: Codec): ContentTypeOf[Event] = AbstractEvent.contentType
  }

  /**
   * An AbstractEvent that can be built up one field at a time. If a field contains a line break, it will be split into multiple fields.
   */
  class EventBuilder private (fields: Vector[(String, String)]) extends AbstractEvent {
    def this() = this(Vector.empty)

    /**
     * Adds additional data to the event.
     *
     * @param value the data to add
     * @return an EventBuilder with the additional data
     */
    def addData(value: String): EventBuilder = {
      new EventBuilder(fields :+ ("data" -> value))
    }

    /**
     * Adds an ID to the event.
     *
     * @param value the ID to add
     * @return an EventBuilder with the additional ID
     */
    def addId(value: String): EventBuilder = {
      new EventBuilder(fields :+ ("id" -> value))
    }

    /**
     * Adds an event name to the event.
     *
     * @param value the event name to add
     * @return an EventBuilder with the additional event name
     */
    def addEvent(value: String): EventBuilder = {
      new EventBuilder(fields :+ ("event" -> value))
    }

    /**
     * Adds a retry time to the event.
     *
     * @param value the retry time to add
     * @return an EventBuilder with the additional retry time
     */
    def addRetry(value: String): EventBuilder = {
      new EventBuilder(fields :+ ("retry" -> value))
    }

    /**
     * Adds a comment to the event.
     *
     * @param value the comment to add
     * @return an EventBuilder with the additional comment
     */
    def addComment(value: String): EventBuilder = {
      new EventBuilder(fields :+ ("" -> value))
    }

    def formatted: String = {
      val sb = new StringBuilder
      for {
        (fieldName, value) <- fields
        line               <- value.split("(\r?\n)|\r", -1)
      } sb.append(fieldName).append(": ").append(line).append('\n')
      sb.append('\n')
      sb.toString()
    }
  }


  // ------------------
  // Event Data Extractor
  // ------------------

  case class EventDataExtractor[A](eventData: A => String)

  trait LowPriorityEventEncoder {
    implicit val stringEvents: EventDataExtractor[String] = EventDataExtractor(identity)

    implicit val jsonEvents: EventDataExtractor[JsValue] = EventDataExtractor(Json.stringify)
  }

  object EventDataExtractor extends LowPriorityEventEncoder

  // ------------------
  // Event ID Extractor
  // ------------------

  case class EventIdExtractor[E](eventId: E => Option[String])

  trait LowPriorityEventIdExtractor {
    implicit def non[E]: EventIdExtractor[E] = EventIdExtractor[E](_ => None)
  }

  object EventIdExtractor extends LowPriorityEventIdExtractor

  // ------------------
  // Event Name Extractor
  // ------------------

  case class EventNameExtractor[E](eventName: E => Option[String])

  trait LowPriorityEventNameExtractor {
    implicit def non[E]: EventNameExtractor[E] = EventNameExtractor[E](_ => None)
  }

  object EventNameExtractor extends LowPriorityEventNameExtractor {
    implicit def pair[E]: EventNameExtractor[(String, E)] = EventNameExtractor[(String, E)](p => Some(p._1))
  }
}
