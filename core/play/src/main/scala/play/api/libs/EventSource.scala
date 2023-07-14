/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs

import akka.stream.scaladsl.Flow
import play.api.http.ContentTypeOf
import play.api.http.ContentTypes
import play.api.http.Writeable
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc._

/**
 * This class provides an easy way to use Server Sent Events (SSE) as a chunked encoding, using an Akka Source.
 *
 * Please see the <a href="https://html.spec.whatwg.org/multipage/server-sent-events.html">Server-Sent Events specification</a> for details.
 *
 * An example of how to display an event stream:
 *
 * {{{
 *   import java.time.ZonedDateTime
 *   import java.time.format.DateTimeFormatter
 *   import javax.inject.Singleton
 *   import akka.stream.scaladsl.Source
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
 *     Ok.chunked(source via EventSource.flow).as(ContentTypes.EVENT_STREAM)
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
   *   Ok.chunked(jsonStream via EventSource.flow).as(ContentTypes.EVENT_STREAM)
   * }}}
   */
  def flow[E: EventDataExtractor: EventNameExtractor: EventIdExtractor]: Flow[E, Event, _] = {
    Flow[E].map(Event(_))
  }

  // ------------------
  // Event
  // ------------------

  /**
   * An event encoded with the SSE protocol..
   */
  case class Event(data: String, id: Option[String], name: Option[String]) {

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

    implicit def writeable(implicit codec: Codec): Writeable[Event] = {
      Writeable(event => codec.encode(event.formatted))
    }

    implicit def contentType(implicit codec: Codec): ContentTypeOf[Event] = {
      ContentTypeOf(Some(ContentTypes.EVENT_STREAM))
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
