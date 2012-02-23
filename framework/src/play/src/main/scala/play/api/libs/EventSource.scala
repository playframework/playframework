package play.api.libs

import play.api.mvc._
import play.api.libs.iteratee._
import play.api.templates._

import org.apache.commons.lang.{ StringEscapeUtils }

object EventSource {

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

  def apply[E]()(implicit encoder: Comet.CometMessage[E], eventNameExtractor: EventNameExtractor[E], eventIdExtractor: EventIdExtractor[E]) = Enumeratee.map[E] { chunk =>
    eventNameExtractor.eventName(chunk).map("event: " + _ + "\n").getOrElse("") +
      "data: " + encoder.toJavascriptMessage(chunk) + "\r\n\r\n"
  }

}
