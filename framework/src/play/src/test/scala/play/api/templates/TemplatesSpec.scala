package play.api.templates

import org.specs2.mutable._

object TemplatesSpec extends Specification {
  "HtmlFormat" should {
    "escape '<', '&' and '>'" in {
      HtmlFormat.escape("foo < bar & baz >").body must equalTo("foo &lt; bar &amp; baz &gt;")
    }
  }

  "HtmlFormat" should {
    "not escape non-ASCII characters" in {
      HtmlFormat.escape("こんにちは").body must equalTo("こんにちは")
    }
  }
}
