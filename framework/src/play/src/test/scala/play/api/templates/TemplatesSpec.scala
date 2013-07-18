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
