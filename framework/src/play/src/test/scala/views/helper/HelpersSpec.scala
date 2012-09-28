package views.helper

import org.specs2.mutable.Specification
import views.html.helper._
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.Lang

object HelpersSpec extends Specification {
  import FieldConstructor.defaultField
  import Lang.defaultLang

  "inputText" should {

    "allow setting a custom id" in {

      val body = inputText.apply(Form(single("foo" -> Forms.text))("foo"), 'id -> "someid").body

      val idAttr = "id=\"someid\""
      body must contain(idAttr)

      // Make sure it doesn't have it twice, issue #478
      body.substring(body.indexOf(idAttr) + idAttr.length) must not contain(idAttr)
    }

  }
}
