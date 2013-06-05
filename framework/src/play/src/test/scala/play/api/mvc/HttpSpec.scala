package play.api.mvc

import org.specs2.mutable._

class HttpSpec extends Specification {

  val headers = new Headers {
    val data = Seq(("a", Seq("a1", "a2")), ("b", Seq("b1", "b2")))
  }

  "Headers" should {

    "return the header value associated with a" in {

      headers.get("a") must be_==(Some("a1"))
    }

    "return the header values associated with b" in {

      headers.getAll("b") must be_==(Seq("b1", "b2"))
    }

    "not return an empty sequence of values associated with an unknown key" in {

      headers.getAll("z") must be_==(Seq.empty)
    }

    "should return all keys" in {

      headers.keys must be_==(Set("a", "b"))
    }

    "should return a simple map" in {

      headers.toSimpleMap must be_==(Map("a" -> "a1", "b" -> "b1"))
    }
  }

  "RequestHeader" should {
    "parse quoted and unquoted charset" in {
      case class TestRequestHeader(headers: Headers, method: String = "GET", uri: String = "/", path: String = "", remoteAddress: String = "127.0.0.1", version: String = "HTTP/1.1", id: Long = 666, tags: Map[String, String] = Map.empty[String, String], queryString: Map[String, Seq[String]] = Map()) extends RequestHeader

      TestRequestHeader(headers = new Headers {
        val data = Seq(play.api.http.HeaderNames.CONTENT_TYPE -> Seq("""text/xml; charset="utf-8""""))
      }).charset must beSome("utf-8")

      TestRequestHeader(headers = new Headers {
        val data = Seq(play.api.http.HeaderNames.CONTENT_TYPE -> Seq("text/xml; charset=utf-8"))
      }).charset must beSome("utf-8")
    }
  }
}
