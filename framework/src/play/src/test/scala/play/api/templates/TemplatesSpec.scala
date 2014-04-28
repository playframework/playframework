/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.templates

import org.specs2.mutable._

object TemplatesSpec extends Specification {
  "toHtmlArgs" should {
    "escape attribute values" in {
      PlayMagic.toHtmlArgs(Map('foo -> """bar <>&"'""")).body must_== """foo="bar &lt;&gt;&amp;&quot;&#x27;""""
    }
  }
}
