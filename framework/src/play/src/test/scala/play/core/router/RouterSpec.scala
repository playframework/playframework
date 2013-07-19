package play.core.router

import org.specs2.mutable.Specification
import play.core.{Router, PathPattern, DynamicPart, StaticPart}

object RouterSpec extends Specification {

  "Router dynamic string builder" should {
    "handle empty parts" in {
      Router.dynamicString("") must_== ""
    }
    "handle simple parts" in {
      Router.dynamicString("xyz") must_== "xyz"
    }
    "handle parts containing backslashes" in {
      Router.dynamicString("x/y") must_== "x%2Fy"
    }
    "handle parts containing spaces" in {
      Router.dynamicString("x y") must_== "x%20y"
    }
    "handle parts containing pluses" in {
      Router.dynamicString("x+y") must_== "x+y"
    }
    "handle parts with unicode characters" in {
      Router.dynamicString("ℛat") must_== "%E2%84%9Bat"
    }
  }

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

  "PathPattern" should {
    val pathPattern = PathPattern(Seq(StaticPart("/path/"), StaticPart("to/"), DynamicPart("foo", "[^/]+", true)))
    val pathString = "/path/to/some%20file"
    val pathNonEncodedString1 = "/path/to/bar:baz"
    val pathNonEncodedString2 = "/path/to/bar:%20baz"
    val pathStringInvalid = "/path/to/invalide%2"

    "Bind Path string as string" in {
      pathPattern(pathString).get("foo") must beEqualTo(Right("some file"))
    }
    "Bind Path with incorrectly encoded string as string" in {
      pathPattern(pathNonEncodedString1).get("foo") must beEqualTo(Right("bar:baz"))
    }
    "Bind Path with incorrectly encoded string as string" in {
      pathPattern(pathNonEncodedString2).get("foo") must beEqualTo(Right("bar: baz"))
    }
    "Fail on unparseable Path string" in {
      val Left(e) = pathPattern(pathStringInvalid).get("foo")
      e.getMessage must beEqualTo("Malformed escape pair at index 9: /invalide%2")
    }

    "multipart path is not decoded" in {
      val pathPattern = PathPattern(Seq(StaticPart("/path/"), StaticPart("to/"), DynamicPart("foo", ".+", false)))
      val pathString = "/path/to/this/is/some%20file/with/id"
      pathPattern(pathString).get("foo") must beEqualTo(Right("this/is/some%20file/with/id"))

    }
  }
}
