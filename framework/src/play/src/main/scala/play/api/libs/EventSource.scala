/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs

import play.api.mvc._
import play.api.libs.iteratee._

import play.core.Execution.Implicits.internalContext
import play.api.libs.json.{ Json, JsValue }

object EventSource {

  case class EventDataExtractor[A](eventData: A => String)

  trait LowPriorityEventEncoder {

    implicit val stringEvents: EventDataExtractor[String] = EventDataExtractor(identity)

    implicit val jsonEvents: EventDataExtractor[JsValue] = EventDataExtractor(Json.stringify)

  }

  object EventDataExtractor extends LowPriorityEventEncoder

  case class EventNameExtractor[E](eventName: E => Option[String])

  case class EventIdExtractor[E](eventId: E => Option[String])

  trait LowPriorityEventNameExtractor {

    implicit def non[E]: EventNameExtractor[E] = EventNameExtractor[E](_ => None)

  }

  trait LowPriorityEventIdExtractor {

    implicit def non[E]: EventIdExtractor[E] = EventIdExtractor[E](_ => None)

  }

  object EventNameExtractor extends LowPriorityEventNameExtractor {

    implicit def pair[E]: EventNameExtractor[(String, E)] = EventNameExtractor[(String, E)](p => Some(p._1))

  }

  object EventIdExtractor extends LowPriorityEventIdExtractor

  def apply[E: EventDataExtractor: EventNameExtractor: EventIdExtractor]() =
    Enumeratee.map[E] { e => Event(e).formatted }

  case class Event(data: String, id: Option[String], name: Option[String]) {
    /**
     * This event, formatted according to the EventSource protocol.
     */
    lazy val formatted = {
      val sb = new StringBuilder
      name.foreach(sb.append("event: ").append(_).append('\n'))
      id.foreach(sb.append("id: ").append(_).append('\n'))
      for (line <- data.split("(\r?\n)|\r")) {
        sb.append("data: ").append(line).append('\n')
      }
      sb.append('\n')
      sb.toString()
    }
  }

  object Event {
    def apply[A](a: A)(implicit dataExtractor: EventDataExtractor[A], nameExtractor: EventNameExtractor[A], idExtractor: EventIdExtractor[A]): Event =
      Event(dataExtractor.eventData(a), idExtractor.eventId(a), nameExtractor.eventName(a))
  }

}
