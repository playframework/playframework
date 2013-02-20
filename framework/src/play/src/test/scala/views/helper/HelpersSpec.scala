package views.helper

import org.specs2.mutable.Specification
import views.html.helper._
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.Lang

object HelpersSpec extends Specification {
  import FieldConstructor.defaultField
  import Lang.defaultLang

  "@inputText" should {

    "allow setting a custom id" in {

      val body = inputText.apply(Form(single("foo" -> Forms.text))("foo"), 'id -> "someid").body

      val idAttr = "id=\"someid\""
      body must contain(idAttr)

      // Make sure it doesn't have it twice, issue #478
      body.substring(body.indexOf(idAttr) + idAttr.length) must not contain(idAttr)
    }

    "default to a type of text" in {
      inputText.apply(Form(single("foo" -> Forms.text))("foo")).body must contain("type=\"text\"")
    }

    "allow setting a custom type" in {
      val body = inputText.apply(Form(single("foo" -> Forms.text))("foo"), 'type -> "email").body

      val typeAttr = "type=\"email\""
      body must contain(typeAttr)

      // Make sure it doesn't contain it twice
      body.substring(body.indexOf(typeAttr) + typeAttr.length) must not contain(typeAttr)
    }
  }

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
