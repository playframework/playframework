package views.html

import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

object index extends Results {

  def apply(input:String) : Future[SimpleResult] = {
    Future(
      Ok("Hello Coco") as("text/html")
    )
  }
}
