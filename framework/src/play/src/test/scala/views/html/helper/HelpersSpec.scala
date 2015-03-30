/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package views.html.helper

import org.specs2.mutable.Specification
import play.api.{ Configuration, Environment }
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{ DefaultLangs, DefaultMessagesApi, Lang }
import play.twirl.api.Html
import scala.beans.BeanProperty

object HelpersSpec extends Specification {
  import FieldConstructor.defaultField
  val messagesApi = new DefaultMessagesApi(Environment.simple(), Configuration.reference, new DefaultLangs(Configuration.reference))
  implicit val messages = messagesApi.preferred(Seq.empty)

  "@inputText" should {

    "allow setting a custom id" in {

      val body = inputText.apply(Form(single("foo" -> Forms.text))("foo"), 'id -> "someid").body

      val idAttr = "id=\"someid\""
      body must contain(idAttr)

      // Make sure it doesn't have it twice, issue #478
      body.substring(body.indexOf(idAttr) + idAttr.length) must not contain (idAttr)
    }

    "default to a type of text" in {
      inputText.apply(Form(single("foo" -> Forms.text))("foo")).body must contain("type=\"text\"")
    }

    "allow setting a custom type" in {
      val body = inputText.apply(Form(single("foo" -> Forms.text))("foo"), 'type -> "email").body

      val typeAttr = "type=\"email\""
      body must contain(typeAttr)

      // Make sure it doesn't contain it twice
      body.substring(body.indexOf(typeAttr) + typeAttr.length) must not contain (typeAttr)
    }
  }

  "@checkboxGroup" should {
    "allow to check more than one checkbox" in {
      val form = Form(single("hobbies" -> Forms.list(Forms.text))).fill(List("S", "B"))
      val body = inputCheckboxGroup.apply(form("hobbies"), Seq(("S", "Surfing"), ("B", "Biking"))).body

      // Append [] to the name for the form binding
      body must contain("name=\"hobbies[]\"")

      body must contain("""<input type="checkbox" id="hobbies_S" name="hobbies[]" value="S" checked="checked" />""")
      body must contain("""<input type="checkbox" id="hobbies_B" name="hobbies[]" value="B" checked="checked" />""")
    }
  }

  "@select" should {

    "allow setting a custom id" in {

      val body = select.apply(Form(single("foo" -> Forms.text))("foo"), Seq(("0", "test")), 'id -> "someid").body

      val idAttr = "id=\"someid\""
      body must contain(idAttr)

      // Make sure it doesn't have it twice, issue #478
      body.substring(body.indexOf(idAttr) + idAttr.length) must not contain (idAttr)
    }

    "allow setting custom data attributes" in {
      import Implicits.toAttributePair

      val body = select.apply(Form(single("foo" -> Forms.text))("foo"), Seq(("0", "test")), "data-test" -> "test").body

      val dataTestAttr = "data-test=\"test\""
      body must contain(dataTestAttr)

      // Make sure it doesn't have it twice, issue #478
      body.substring(body.indexOf(dataTestAttr) + dataTestAttr.length) must not contain (dataTestAttr)
    }

    "Work as a simple select" in {
      val form = Form(single("foo" -> Forms.text)).fill("0")
      val body = select.apply(form("foo"), Seq(("0", "test"), ("1", "test"))).body

      body must contain("name=\"foo\"")

      body must contain("""<option value="0" selected="selected">""")
      body must contain("""<option value="1" >""")
    }

    "Work as a multiple select" in {
      val form = Form(single("foo" -> Forms.list(Forms.text))).fill(List("0", "1"))
      val body = select.apply(form("foo"), Seq(("0", "test"), ("1", "test")), 'multiple -> None).body

      // Append [] to the name for the form binding
      body must contain("name=\"foo[]\"")
      body must contain("multiple")

      body must contain("""<option value="0" selected="selected">""")
      body must contain("""<option value="1" selected="selected">""")
    }
  }

  "@repeat" should {
    val form = Form(single("foo" -> Forms.seq(Forms.text)))
    def renderFoo(form: Form[_], min: Int = 1) = repeat.apply(form("foo"), min) { f =>
      Html(f.name + ":" + f.value.getOrElse(""))
    }.map(_.toString)

    val complexForm = Form(single("foo" ->
      Forms.seq(tuple(
        "a" -> Forms.text,
        "b" -> Forms.text
      ))
    ))
    def renderComplex(form: Form[_], min: Int = 1) = repeat.apply(form("foo"), min) { f =>
      val a = f("a")
      val b = f("b")
      Html(s"${a.name}=${a.value.getOrElse("")},${b.name}=${b.value.getOrElse("")}")
    }.map(_.toString)

    "render a sequence of fields" in {
      renderFoo(form.fill(Seq("a", "b", "c"))) must exactly("foo[0]:a", "foo[1]:b", "foo[2]:c").inOrder
    }

    "render a sequence of fields in an unfilled form" in {
      renderFoo(form, 4) must exactly("foo[0]:", "foo[1]:", "foo[2]:", "foo[3]:").inOrder
    }

    "fill the fields out if less than the min" in {
      renderFoo(form.fill(Seq("a", "b")), 4) must exactly("foo[0]:a", "foo[1]:b", "foo[2]:", "foo[3]:").inOrder
    }

    "fill the fields out if less than the min but the maximum is high" in {
      renderFoo(form.bind(Map("foo[0]" -> "a", "foo[123]" -> "b")), 4) must exactly("foo[0]:a", "foo[123]:b", "foo[124]:", "foo[125]:").inOrder
    }

    "render the right number of fields if there's multiple sub fields at a given index when filled" in {
      renderComplex(
        complexForm.fill(Seq("somea" -> "someb"))
      ) must exactly("foo[0].a=somea,foo[0].b=someb")
    }

    "render fill the right number of fields out if there's multiple sub fields at a given index when bound" in {
      renderComplex(
        // Don't bind, we don't want it to use the successfully bound value
        form.copy(data = Map("foo[0].a" -> "somea", "foo[0].b" -> "someb"))
      ) must exactly("foo[0].a=somea,foo[0].b=someb")
    }

    "work with i18n" in {
      import play.api.i18n.Lang
      implicit val lang = Lang("en-US")

      val roleForm = Form(single("role" -> Forms.text)).fill("foo")
      val body = repeat.apply(roleForm("bar"), min = 1) { roleField =>
        select.apply(roleField, Seq("baz" -> "qux"), '_default -> "Role")
      }.mkString("")

      body must contain("""label for="bar_0">bar.0""")
    }
  }
}
