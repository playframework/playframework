package play.api.http

import org.specs2.mutable._

object MediaRangeSpec extends Specification {

  "A MediaRange" should {
    "accept all media types" in {
      val mediaRange = MediaRange("*/*")
      mediaRange.accepts("text/html") must beTrue
      mediaRange.accepts("application/json") must beTrue
      mediaRange.accepts("foo/bar") must beTrue
    }
    "accept a range of media types" in {
      val mediaRange = MediaRange("text/*")
      mediaRange.accepts("text/html") must beTrue
      mediaRange.accepts("text/plain") must beTrue
      mediaRange.accepts("application/json") must beFalse
    }
    "accept a media type" in {
      val mediaRange = MediaRange("text/html")
      mediaRange.accepts("text/html") must beTrue
      mediaRange.accepts("text/plain") must beFalse
      mediaRange.accepts("application/json") must beFalse
    }
  }

}