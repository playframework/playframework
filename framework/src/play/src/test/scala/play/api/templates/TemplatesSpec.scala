/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.templates

import org.specs2.mutable._

object TemplatesSpec extends Specification {
  "HtmlFormat" should {
    "escape '<', '&' and '>'" in {
      HtmlFormat.escape("foo < bar & baz >").body must_== "foo &lt; bar &amp; baz &gt;"
    }

    "escape single quotes" in {
      HtmlFormat.escape("'single quotes'").body must_== "&#x27;single quotes&#x27;"
    }

    "escape double quotes" in {
      HtmlFormat.escape("\"double quotes\"").body must_== "&quot;double quotes&quot;"
    }

    "not escape non-ASCII characters" in {
      HtmlFormat.escape("こんにちは").body must_== "こんにちは"
    }
  }

  "XmlFormat" should {
    "escape '<', '&' and '>'" in {
      XmlFormat.escape("foo < bar & baz >").body must_== "foo &lt; bar &amp; baz &gt;"
    }

    "escape single quotes" in {
      XmlFormat.escape("'single quotes'").body must_== "&#39;single quotes&#39;"
    }

    "escape double quotes" in {
      XmlFormat.escape("\"double quotes\"").body must_== "&#34;double quotes&#34;"
    }

    "not escape non-ASCII characters" in {
      XmlFormat.escape("こんにちは").body must_== "こんにちは"
    }

    "replace ASCII control characters" in {
      XmlFormat.escape("\u0000\u0001\u0002\u0003\u0004\u0009\n\r\f\u001f \u007f\u0084\u0086\u009f").body must_== "     \u0009\n\r       "
    }
  }

  "toHtmlArgs" should {
    "escape attribute values" in {
      PlayMagic.toHtmlArgs(Map('foo -> """bar <>&"'""")).body must_== """foo="bar &lt;&gt;&amp;&quot;&#x27;""""
    }
  }

  "JavaScriptFormat" should {
    """escape ''', '"' and '\'""" in {
      JavaScriptFormat.escape("""foo ' bar " baz \""").body must equalTo ("""foo \' bar \" baz \\""")
    }
  }

}
