package play.api.http

import org.specs2.mutable._
import java.net.URLEncoder

object MediaRangeSpec extends Specification {

  "A MediaRange" should {

    def parseSingleMediaRange(mediaRange: String): MediaRange = {
      val parsed = MediaRange.parse(mediaRange)
      parsed.length must_== 1
      parsed.head
    }

    def parseInvalidMediaRange(mediaRange: String) = {
      MediaRange.parse(mediaRange) must beEmpty
    }

    "accept all media types" in {
      val mediaRange = parseSingleMediaRange("*/*")
      mediaRange.accepts("text/html") must beTrue
      mediaRange.accepts("application/json") must beTrue
      mediaRange.accepts("foo/bar") must beTrue
    }
    "accept a range of media types" in {
      val mediaRange = parseSingleMediaRange("text/*")
      mediaRange.accepts("text/html") must beTrue
      mediaRange.accepts("text/plain") must beTrue
      mediaRange.accepts("application/json") must beFalse
    }
    "accept a media type" in {
      val mediaRange = parseSingleMediaRange("text/html")
      mediaRange.accepts("text/html") must beTrue
      mediaRange.accepts("text/plain") must beFalse
      mediaRange.accepts("application/json") must beFalse
    }
    "allow media types with slashes in the parameters" in {
      val MediaType.parse(mediaType) = "application/ld+json;profile=\"http://www.w3.org/ns/json-ld#compacted\""
      mediaType.mediaType must_== "application"
      mediaType.mediaSubType must_== "ld+json"
      mediaType.parameters must_== Seq("profile" -> Some("http://www.w3.org/ns/json-ld#compacted"))
    }
    "not choke on invalid media types" in {
      MediaType.parse("foo") must beNone
    }
    "allow anything in a quoted string" in {
      MediaRange.parse("""foo/bar, foo2/bar2; p="v,/\"\\vv"; p2=v2""") must_== Seq(
        new MediaRange("foo", "bar", Nil, None, Nil),
        new MediaRange("foo2", "bar2", Seq("p" -> Some("""v,/"\vv"""), "p2" -> Some("v2")), None, Nil)
      )
    }
    "allow valueless parameters" in {
      MediaType.parse("foo/bar;param") must beSome(MediaType("foo", "bar", Seq("param" -> None)))
    }
    "extract the qvalue from the parameters" in {
      parseSingleMediaRange("foo/bar;q=0.25") must_== new MediaRange("foo", "bar", Nil, Some(0.25f), Nil)
    }
    "differentiate between media type parameters and accept extensions" in {
      parseSingleMediaRange("foo/bar;p1;q=0.25;p2") must_==
        new MediaRange("foo", "bar", Seq("p1" -> None), Some(0.25f), Seq("p2" -> None))
    }
    "support non spec compliant everything media ranges" in {
      parseSingleMediaRange("*") must_== new MediaRange("*", "*", Nil, None, Nil)
    }
    "maintain the original order of media ranges in the accept header" in {
      MediaRange.parse("foo1/bar1, foo3/bar3, foo2/bar2") must contain(exactly(
        new MediaRange("foo1", "bar1", Nil, None, Nil),
        new MediaRange("foo3", "bar3", Nil, None, Nil),
        new MediaRange("foo2", "bar2", Nil, None, Nil)
      ).inOrder)
    }
    "order by q value" in {
      MediaRange.parse("foo1/bar1;q=0.25, foo3/bar3, foo2/bar2;q=0.5") must contain(exactly(
        new MediaRange("foo3", "bar3", Nil, None, Nil),
        new MediaRange("foo2", "bar2", Nil, Some(0.5f), Nil),
        new MediaRange("foo1", "bar1", Nil, Some(0.25f), Nil)
      ).inOrder)
    }
    "order by specificity" in {
      MediaRange.parse("*/*, foo/*, foo/bar") must contain(exactly(
        new MediaRange("foo", "bar", Nil, None, Nil),
        new MediaRange("foo", "*", Nil, None, Nil),
        new MediaRange("*", "*", Nil, None, Nil)
      ).inOrder)
    }
    "order by parameters" in {
      MediaRange.parse("foo/bar, foo/bar;p1=v1;p2=v2, foo/bar;p1=v1") must contain(exactly(
        new MediaRange("foo", "bar", Seq("p1" -> Some("v1"), "p2" -> Some("v2")), None, Nil),
        new MediaRange("foo", "bar", Seq("p1" -> Some("v1")), None, Nil),
        new MediaRange("foo", "bar", Nil, None, Nil)
      ).inOrder)
    }
    "just order it all damn it" in {
      MediaRange.parse("foo/bar1;q=0.25, */*;q=0.25, foo/*;q=0.25, foo/bar2, foo/bar3;q=0.5, foo/*, foo/bar4") must contain(exactly(
        new MediaRange("foo", "bar2", Nil, None, Nil),
        new MediaRange("foo", "bar4", Nil, None, Nil),
        new MediaRange("foo", "*", Nil, None, Nil),
        new MediaRange("foo", "bar3", Nil, Some(0.5f), Nil),
        new MediaRange("foo", "bar1", Nil, Some(0.25f), Nil),
        new MediaRange("foo", "*", Nil, Some(0.25f), Nil),
        new MediaRange("*", "*", Nil, Some(0.25f), Nil)
      ).inOrder)
    }
    "be able to be convert back to a string" in {
      new MediaType("foo", "bar", Nil).toString must_== "foo/bar"
      new MediaType("foo", "bar", Seq("p1" -> Some("v1"), "p2" -> Some(""" v\"v"""), "p3" -> None)).toString must_==
        """foo/bar; p1=v1; p2=" v\\\"v"; p3"""
      new MediaRange("foo", "bar", Nil, None, Nil).toString must_== "foo/bar"
      new MediaRange("foo", "bar", Nil, Some(0.25f), Nil).toString must_== "foo/bar; q=0.25"
      new MediaRange("foo", "bar", Seq("p1" -> Some("v1")), Some(0.25f), Seq("p2" -> Some("v2"))).toString must_==
        "foo/bar; p1=v1; q=0.25; p2=v2"
    }
    // Rich tests
    "gracefully handle empty parts" in {
      parseInvalidMediaRange("text/")
      parseInvalidMediaRange("text/;foo")
    }
    "gracefully handle invalid characters in tokens" in {
      for {
        c <- "\u0000\u007F (){}\\\"".toSeq
        format <- Seq(
          "fo%so/bar, text/plain;charset=utf-8",
          "foo/ba%sr, text/plain;charset=utf-8",
          "text/plain;pa%sram;charset=utf-8",
          "text/plain;param=va%slue;charset=utf-8"
        )
      } yield {
        // Use URL encoder so we can see which ctl character it's using
        def description = "Media type format: '" + format + "' Invalid character: " + c.toInt
        val parsed = MediaRange.parse(format.format(c))

        parsed aka description must haveSize(1)
        parsed.head aka description must_==
          new MediaRange("text", "plain", Seq("charset" -> Some("utf-8")), None, Nil)
      }
      success
    }
    "gracefully handle invalid q values" in {
      parseSingleMediaRange("foo/bar;q=a") must_== new MediaRange("foo", "bar", Nil, None, Nil)
      parseSingleMediaRange("foo/bar;q=1.01") must_== new MediaRange("foo", "bar", Nil, None, Nil)
    }
  }

}