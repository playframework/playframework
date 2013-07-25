package test

import org.specs2.mutable.Specification
import play.api.libs.ws._
import controllers.TestController.Model._
import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers._

object WsSpec extends Specification {

  "WS responses" should {

    "handle the charset correctly" in {

      "respect the charset when specified" in new WithServer() {
        slave(ToReturn(headers = Map("Content-Type" -> "text/plain; charset=utf-16"),
                       body = Some("äöü!".getBytes("utf-16")))).body must_== "äöü!"
      }

      "default to ISO8859-1 for text mime subtypes when no charset specified" in new WithServer() {
        slave(ToReturn(headers = Map("Content-Type" -> "text/plain"),
          body = Some("äöü!".getBytes("iso8859-1")))).body must_== "äöü!"
      }

      "default to UTF-8 for non text mime subtypes when no charset specified" in new WithServer() {
        slave(ToReturn(headers = Map("Content-Type" -> "application/json"),
          body = Some("äöü!".getBytes("utf-8")))).body must_== "äöü!"
      }

      "default to utf-8 for when no content type specified" in new WithServer() {
        slave(ToReturn(body = Some("äöü!".getBytes("utf-8")))).body must_== "äöü!"
      }
    }

    "automatically detect the correct charset for JSON" in {
      def test(charset: String)(implicit port: Port) = {
        val json = JsObject(Seq("string" -> JsString("äöü")))
        (slave(ToReturn(body = Some(Json.stringify(json).getBytes(charset)))).json \ "string").as[String] must_== "äöü"
      }
      "utf-8" in new WithServer() { test("utf-8") }
      "utf-16" in new WithServer() { test("utf-16") }
      "utf-32" in new WithServer() { test("utf-32") }
    }

  }

  def slave(toReturn: ToReturn)(implicit port: Port): Response =
    Helpers.await(wsCall(controllers.routes.TestController.slave()).post(Json.toJson(toReturn)))

}
