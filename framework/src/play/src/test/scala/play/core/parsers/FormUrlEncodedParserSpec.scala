/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.parsers

import org.specs2.mutable.Specification

object FormUrlEncodedParserSpec extends Specification {
  "FormUrlEncodedParser" should {
    "decode forms" in {
      FormUrlEncodedParser.parse("foo1=bar1&foo2=bar2") must_== Map("foo1" -> List("bar1"), "foo2" -> List("bar2"))
    }
    "decode form elements with multiple values" in {
      FormUrlEncodedParser.parse("foo=bar1&foo=bar2") must_== Map("foo" -> List("bar1", "bar2"))
    }
    "decode fields with empty names" in {
      FormUrlEncodedParser.parse("foo=bar&=") must_== Map("foo" -> List("bar"))
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
