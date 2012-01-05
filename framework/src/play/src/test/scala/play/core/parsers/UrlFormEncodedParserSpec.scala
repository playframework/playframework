package play.core.parsers

import org.specs2.mutable.Specification

object UrlFormEncodedParserSpec extends Specification {

  "UrlfFormEncodedParser" should {
    "url decode keys and values correctly" in {
      UrlFormEncodedParser.parse("key%5B1%5D=value%5B1%5D") must_== Map("key[1]" -> Seq("value[1]"))
    }
    "handle multiple values of same key" in {
      UrlFormEncodedParser.parse("key=value1&key=value2") must_== Map("key" -> Seq("value1", "value2"))
    }
  }
}