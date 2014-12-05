package play.libs.ws

import play.api.mvc.{ Result, Action }
import play.api.mvc.Results._
import play.api.test._

object WSSpec extends PlaySpecification {

  sequential

  val uploadApp = FakeApplication(withRoutes = {
    case ("POST", "/") =>
      Action { request =>
        request.body.asRaw.fold[Result](BadRequest) { raw =>
          val size = raw.size
          Ok(s"size=$size")
        }
      }
  })

  "WS.url().post(InputStream)" should {
    "uploads the stream" in new WithServer(app = uploadApp, port = 3333) {

      val input = this.getClass.getClassLoader.getResourceAsStream("play/libs/ws/play_full_color.png")
      val req = WS.url("http://localhost:3333").post(input).wrapped()

      val rep = await(req)

      rep.getStatus must ===(200)
      rep.getBody must ===("size=20039")
    }
  }

}