package play.api.http

import org.specs2.mutable._

object MediaRangeSpec extends Specification {

  "A MediaRange" should {
    "accept all media types" in {
      val mediaRange = parse("*/*")
      mediaRange.accepts("text/html") must beTrue
      mediaRange.accepts("application/json") must beTrue
      mediaRange.accepts("foo/bar") must beTrue
    }
    "accept a range of media types" in {
      val mediaRange = parse("text/*")
      mediaRange.accepts("text/html") must beTrue
      mediaRange.accepts("text/plain") must beTrue
      mediaRange.accepts("application/json") must beFalse
    }
    "accept a media type" in {
      val mediaRange = parse("text/html")
      mediaRange.accepts("text/html") must beTrue
      mediaRange.accepts("text/plain") must beFalse
      mediaRange.accepts("application/json") must beFalse
    }
    "allow media types with slashes in the parameters" in {
      val mediaRange = parse("application/ld+json;profile=\"http://www.w3.org/ns/json-ld#compacted\"")
      mediaRange.mediaType must_== "application"
      mediaRange.mediaSubType must_== "ld+json"
      mediaRange.parameters must beSome("profile=\"http://www.w3.org/ns/json-ld#compacted\"")
    }
    "not choke on invalid media ranges" in {
      MediaRange.parse("foo") must beNone
    }

    def parse(mediaRange: String): MediaRange = {
      val parsed = MediaRange.parse(mediaRange)
      parsed must beSome
      parsed.get
    }
  }

}