package play.core.router

import org.specs2.mutable.Specification
import play.core.Router

object RouterSpec extends Specification {
  "Router queryString builder" should {
    "build a query string" in {
      Router.queryString(List(Some("a"), Some("b"))) must_== "?a&b"
    }
    "ignore none values" in {
      Router.queryString(List(Some("a"), None, Some("b"))) must_== "?a&b"
      Router.queryString(List(None, Some("a"), None)) must_== "?a"
    }
    "ignore empty values" in {
      Router.queryString(List(Some("a"), Some(""), Some("b"))) must_== "?a&b"
      Router.queryString(List(Some(""), Some("a"), Some(""))) must_== "?a"
    }
    "produce nothing if no values" in {
      Router.queryString(List(None, Some(""))) must_== ""
      Router.queryString(List()) must_== ""
    }
  }
}
