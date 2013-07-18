package play.api.libs

import scala.language.reflectiveCalls

import play.api.mvc._
import play.api.libs.iteratee._
import play.api.templates._

import play.core.Execution.Implicits.internalContext

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
    eventNameExtractor.eventName(chunk).map("event: " + _ + "\r\n").getOrElse("") +
      eventIdExtractor.eventId(chunk).map("id: " + _ + "\r\n").getOrElse("") +
      "data: " + encoder.toJavascriptMessage(chunk) + "\r\n\r\n"
  }

}
