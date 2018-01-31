/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.parsers

import org.specs2.mutable.Specification

class FormUrlEncodedParserSpec extends Specification {
  "FormUrlEncodedParser" should {
    "decode forms" in {
      FormUrlEncodedParser.parse("foo1=bar1&foo2=bar2") must_== Map("foo1" -> List("bar1"), "foo2" -> List("bar2"))
    }
    "decode forms with semicolons" in {
      // http://www.w3.org/TR/1999/REC-html401-19991224/appendix/notes.html#h-B.2.2
      FormUrlEncodedParser.parse("foo1=bar1;foo2=bar2") must_== Map("foo1" -> List("bar1"), "foo2" -> List("bar2"))
    }
    "decode forms with ampersands and semicolons" in {
      FormUrlEncodedParser.parse("foo1=bar1&foo2=bar2;foo3=bar3") must_== Map("foo1" -> List("bar1"), "foo2" -> List("bar2"), "foo3" -> List("bar3"))
    }
    "decode form elements with multiple values" in {
      FormUrlEncodedParser.parse("foo=bar1&foo=bar2") must_== Map("foo" -> List("bar1", "bar2"))
    }
    "decode fields with empty names" in {
      FormUrlEncodedParser.parse("foo=bar&=") must_== Map("foo" -> List("bar"), "" -> List(""))
    }
    "decode fields with empty values" in {
      FormUrlEncodedParser.parse("foo=bar&baz=") must_== Map("foo" -> List("bar"), "baz" -> List(""))
    }
    "decode fields with no value" in {
      FormUrlEncodedParser.parse("foo=bar&baz") must_== Map("foo" -> List("bar"), "baz" -> List(""))
    }
    "decode single field with no value" in {
      FormUrlEncodedParser.parse("foo") must_== Map("foo" -> List(""))
    }
    "decode when there are no fields" in {
      FormUrlEncodedParser.parse("") must beEmpty
    }
    "ensure field order is retained, when requested" in {
      val url_encoded = "Zero=zero&One=one&Two=two&Three=three&Four=four&Five=five&Six=six&Seven=seven"
      val result: Map[String, Seq[String]] = FormUrlEncodedParser.parse(url_encoded)
      val strings = (for (k <- result.keysIterator) yield "&" + k + "=" + result(k).head).mkString
      val reconstructed = strings.substring(1)
      reconstructed must equalTo(url_encoded)
    }
  }
}
