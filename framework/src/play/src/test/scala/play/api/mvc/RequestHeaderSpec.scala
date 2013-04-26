package play.api.mvc

import org.specs2.mutable.Specification
import play.api.http.{HeaderNames, MediaRange}

class RequestHeaderSpec extends Specification {

  case class FakeHeaders(data: Seq[(String, Seq[String])]) extends Headers

  case class FakeRequestHeader(headers: Headers) extends RequestHeader {
    def id: Long = ???
    def tags: Map[String, String] = ???
    def uri: String = ???
    def path: String = ???
    def method: String = ???
    def version: String = ???
    def queryString: Map[String, Seq[String]] = ???
    def remoteAddress: String = ???
  }

  "a Request Header" should {
    "compute the accepted types sorted by preference (preferred first)" in {
      val fakeHeaders = FakeHeaders(List(HeaderNames.ACCEPT -> List("text/*;q=0.3, text/html;q=0.7, text/html;level=1, text/html;level=2;q=0.4, */*;q=0.5, text/json;q=0.3")))
      val acceptedTypes = FakeRequestHeader(fakeHeaders).acceptedTypes
      acceptedTypes must beEqualTo(List(
        new MediaRange("text", "html" ,Some("level=1")), // q=1 (default)
        new MediaRange("text", "html" ,None),            // q=0.7
        new MediaRange("*",    "*",    None),            // q=0.5
        new MediaRange("text", "html", Some("level=2")), // q=0.4
        new MediaRange("text", "json", None),            // q=0.3
        new MediaRange("text", "*" ,   None)))           // q=0.3
    }
  }

}
