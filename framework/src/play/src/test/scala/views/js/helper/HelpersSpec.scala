package views.js.helper

import org.specs2.mutable.Specification

object HelpersSpec extends Specification {

  "@json" should {
    "Produce valid JavaScript strings" in {
      json("foo").toString must equalTo ("\"foo\"")
    }

    "Properly escape quotes" in {
      json("fo\"o").toString must equalTo ("\"fo\\\"o\"")
    }

    "Not escape HTML entities" in {
      json("fo&o").toString must equalTo ("\"fo&o\"")
    }

    "Produce valid JavaScript literal objects" in {
      json(Map("foo" -> "bar")).toString must equalTo ("{\"foo\":\"bar\"}")
    }

    "Produce valid JavaScript arrays" in {
      json(List("foo", "bar")).toString must equalTo ("[\"foo\",\"bar\"]")
    }
  }

}
