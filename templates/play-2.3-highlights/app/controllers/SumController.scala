package controllers

import play.api.mvc._
import play.api.Play.current
import actors._
import scala.concurrent.Future

object SumController extends Controller {

  def socket(password: String) = WebSocket.tryAcceptWithActor[SumActor.Sum, SumActor.SumResult] { request =>
    Future.successful(
      if (password == "secret") {
        Right(SumActor.props(_))
      } else {
        Left(Forbidden)
      }
    )
  }
}