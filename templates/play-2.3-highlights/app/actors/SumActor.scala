package actors

import akka.actor._
import play.api.libs.json._
import play.api.mvc.WebSocket.FrameFormatter

object SumActor {

  def props(out: ActorRef) = Props(new SumActor(out))

  /**
   * Sum the given values.
   */
  case class Sum(values: Seq[Int])

  object Sum {
    implicit val sumFormat = Json.format[Sum]
    implicit val sumFrameFormatter = FrameFormatter.jsonFrame[Sum]
  }

  /**
   * The result of summing the values
   */
  case class SumResult(sum: Int)

  object SumResult {
    implicit val sumResultFormat = Json.format[SumResult]
    implicit val sumResultFrameFormatter = FrameFormatter.jsonFrame[SumResult]
  }
}

/**
 * An actor that sums sequences of numbers
 */
class SumActor(out: ActorRef) extends Actor {

  import SumActor._

  def receive = {
    case Sum(values) => out ! SumResult(values.fold(0)(_ + _))
  }

}