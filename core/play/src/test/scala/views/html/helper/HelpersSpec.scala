/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package views.html.helper

import java.util.Optional

import org.specs2.mutable.Specification
import play.api.data._
import play.api.data.Forms._
import play.api.http.HttpConfiguration
import play.api.i18n._
import play.api.Configuration
import play.api.Environment
import play.twirl.api.Html

class HelpersSpec extends Specification {
  import FieldConstructor.defaultField

  val conf                        = Configuration.reference
  val langs                       = new DefaultLangsProvider(conf).get
  val httpConfiguration           = HttpConfiguration.fromConfiguration(conf, Environment.simple())
  val messagesApi                 = new DefaultMessagesApiProvider(Environment.simple(), conf, langs, httpConfiguration).get
  implicit val messages: Messages = messagesApi.preferred(Seq.empty)

  "@inputText" should {
    "allow setting a custom id" in {
      val body = inputText.apply(Form(single("foo" -> Forms.text))("foo"), Symbol("id") -> "someid").body

      val idAttr = "id=\"someid\""
      body must contain(idAttr)

      // Make sure it doesn't have it twice, issue #478
      body.substring(body.indexOf(idAttr) + idAttr.length) must not contain idAttr
    }

    "default to a type of text" in {
      inputText.apply(Form(single("foo" -> Forms.text))("foo")).body must contain("type=\"text\"")
    }

    "allow setting a custom type" in {
      val body = inputText.apply(Form(single("foo" -> Forms.text))("foo"), Symbol("type") -> "email").body

      val typeAttr = "type=\"email\""
      body must contain(typeAttr)

      // Make sure it doesn't contain it twice
      body.substring(body.indexOf(typeAttr) + typeAttr.length) must not contain typeAttr
    }
  }

  "@checkbox" should {
    "translate the _text argument" in {
      val form = Form(single("foo" -> Forms.list(Forms.text)))
      val body = checkbox.apply(form("foo"), Symbol("_text") -> "myfieldlabel").body

      body must contain("""<span>I am the &lt;b&gt;label&lt;/b&gt; of the field</span>""")
    }

    "translate the _text argument but keep raw html" in {
      val form = Form(single("foo" -> Forms.list(Forms.text)))
      val body = checkbox.apply(form("foo"), Symbol("_text") -> Html("myfieldlabel")).body

      body must contain("""<span>I am the <b>label</b> of the field</span>""")
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
      val body =
        select.apply(Form(single("foo" -> Forms.text))("foo"), Seq(("0", "test")), Symbol("id") -> "someid").body

      val idAttr = "id=\"someid\""
      body must contain(idAttr)

      // Make sure it doesn't have it twice, issue #478
      body.substring(body.indexOf(idAttr) + idAttr.length) must not contain idAttr
    }

    "allow setting custom data attributes" in {
      import Implicits.toAttributePair

      val body = select.apply(Form(single("foo" -> Forms.text))("foo"), Seq(("0", "test")), "data-test" -> "test").body

      val dataTestAttr = "data-test=\"test\""
      body must contain(dataTestAttr)

      // Make sure it doesn't have it twice, issue #478
      body.substring(body.indexOf(dataTestAttr) + dataTestAttr.length) must not contain dataTestAttr
    }

    "Work as a simple select" in {
      val form = Form(single("foo" -> Forms.text)).fill("0")
      val body = select.apply(form("foo"), Seq(("0", "test"), ("1", "test"))).body

      body must contain("name=\"foo\"")

      body must contain("""<option value="0" selected="selected">""")
      body must contain("""<option value="1">""")
    }

    "Work as a multiple select" in {
      val form = Form(single("foo" -> Forms.list(Forms.text))).fill(List("0", "1"))
      val body = select.apply(form("foo"), Seq(("0", "test"), ("1", "test")), Symbol("multiple") -> None).body

      // Append [] to the name for the form binding
      body must contain("name=\"foo[]\"")
      body must contain("multiple")

      body must contain("""<option value="0" selected="selected">""")
      body must contain("""<option value="1" selected="selected">""")
    }

    "allow disabled options" in {
      val form = Form(single("foo" -> Forms.list(Forms.text))).fill(List("0", "1"))
      val body =
        select
          .apply(form("foo"), Seq("0" -> "test0", "1" -> "test1", "2" -> "test2"), Symbol("_disabled") -> Seq("0", "2"))
          .body

      body must contain("""<option value="0" disabled>test0</option>""")
      body must contain("""<option value="1">test1</option>""")
      body must contain("""<option value="2" disabled>test2</option>""")
    }

    "translate default option" in {
      val form = Form(single("foo" -> Forms.list(Forms.text))).fill(List("0", "1"))
      val body =
        select
          .apply(form("foo"), Seq("0" -> "test0", "1" -> "test1", "2" -> "test2"), Symbol("_default") -> "myfieldlabel")
          .body

      body must contain("""<option class="blank" value="">I am the &lt;b&gt;label&lt;/b&gt; of the field</option>""")
    }

    "translate default option but keep raw html" in {
      val form = Form(single("foo" -> Forms.list(Forms.text))).fill(List("0", "1"))
      val body =
        select
          .apply(
            form("foo"),
            Seq("0" -> "test0", "1" -> "test1", "2" -> "test2"),
            Symbol("_default") -> Html("myfieldlabel")
          )
          .body

      body must contain("""<option class="blank" value="">I am the <b>label</b> of the field</option>""")
    }
  }

  "@selectGrouped" should {
    "not create unnecessary optgroup" in {
      val body = selectGrouped.apply(Form(single("foo" -> Forms.text))("foo"), Seq("" -> Seq("0" -> "test"))).body

      body must not contain ("""<optgroup""")
    }

    "allow disabled groups" in {
      val body = selectGrouped
        .apply(
          Form(single("foo" -> Forms.text))("foo"),
          Seq(
            ""        -> Seq("0" -> "test"),
            "Group 1" -> Seq("1" -> "foo", "2" -> "bar"),
            "Group 2" -> Seq("3" -> "boo", "4" -> "far")
          ),
          Symbol("_disabledGroups") -> Seq("Group 1")
        )
        .body

      body must contain("""<optgroup label="Group 1" disabled>""")
      body must contain("""<optgroup label="Group 2">""")
    }
  }

  "@repeat" should {
    val form                                   = Form(single("foo" -> Forms.seq(Forms.text)))
    def renderFoo(form: Form[?], min: Int = 1) =
      repeat
        .apply(form("foo"), min) { f => Html(f.name + ":" + f.value.getOrElse("")) }
        .map(_.toString)

    val complexForm = Form(
      single(
        "foo" ->
          Forms.seq(
            tuple(
              "a" -> Forms.text,
              "b" -> Forms.text
            )
          )
      )
    )
    def renderComplex(form: Form[?], min: Int = 1) =
      repeat
        .apply(form("foo"), min) { f =>
          val a = f("a")
          val b = f("b")
          Html(s"${a.name}=${a.value.getOrElse("")},${b.name}=${b.value.getOrElse("")}")
        }
        .map(_.toString)

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
      renderFoo(form.bind(Map("foo[0]" -> "a", "foo[123]" -> "b")), 4) must exactly(
        "foo[0]:a",
        "foo[123]:b",
        "foo[124]:",
        "foo[125]:"
      ).inOrder
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
      val body     = repeat
        .apply(roleForm("bar"), min = 1) { roleField =>
          select.apply(roleField, Seq("baz" -> "qux"), Symbol("_default") -> "Role")
        }
        .mkString("")

      body must contain("""label for="bar_0">bar.0""")
    }
  }

  "helpers" should {
    "correctly lookup and escape constraint, error and format messages" in {
      val field = Field(
        Form(single("foo" -> Forms.text)),
        "foo",
        Seq(("constraint.custom", Seq("constraint.customarg"))),
        Some("format.custom", Seq("format.customarg")),
        Seq(FormError("foo", "error.custom", Seq("error.customarg"))),
        None
      )

      val body = inputText.apply(field).body

      body must contain("""<dd class="error">This &lt;b&gt;is&lt;/b&gt; a custom &lt;b&gt;error&lt;/b&gt;</dd>""")
      body must contain("""<dd class="info">I &lt;b&gt;am&lt;/b&gt; a custom &lt;b&gt;constraint&lt;/b&gt;</dd>""")
      body must contain(
        """<dd class="info">Look &lt;b&gt;at&lt;/b&gt; me! I am a custom &lt;b&gt;format&lt;/b&gt; pattern</dd>"""
      )
    }

    "correctly lookup _label in messages" in {
      inputText.apply(Form(single("foo" -> Forms.text))("foo"), Symbol("_label") -> "myfieldlabel").body must contain(
        "I am the &lt;b&gt;label&lt;/b&gt; of the field"
      )
    }

    "correctly lookup _label in messages but keep raw html" in {
      inputText
        .apply(Form(single("foo" -> Forms.text))("foo"), Symbol("_label") -> Html("myfieldlabel"))
        .body must contain(
        "I am the <b>label</b> of the field"
      )
    }

    "correctly lookup _name in messages" in {
      inputText.apply(Form(single("foo" -> Forms.text))("foo"), Symbol("_name") -> "myfieldname").body must contain(
        "I am the &lt;b&gt;name&lt;/b&gt; of the field"
      )
    }

    "correctly lookup _name in messages but keep raw html" in {
      inputText
        .apply(Form(single("foo" -> Forms.text))("foo"), Symbol("_name") -> Html("myfieldname"))
        .body must contain(
        "I am the <b>name</b> of the field"
      )
    }

    "correctly lookup _help in messages" in {
      inputText.apply(Form(single("foo" -> Forms.text))("foo"), Symbol("_help") -> "myfieldname").body must contain(
        """<dd class="info">I am the &lt;b&gt;name&lt;/b&gt; of the field</dd>"""
      )
    }

    "correctly lookup _help in messages but keep raw html" in {
      inputText
        .apply(Form(single("foo" -> Forms.text))("foo"), Symbol("_help") -> Html("myfieldname"))
        .body must contain(
        """<dd class="info">I am the <b>name</b> of the field</dd>"""
      )
    }

    "correctly display an error when _error is supplied as String" in {
      inputText.apply(Form(single("foo" -> Forms.text))("foo"), Symbol("_error") -> "Force an error").body must contain(
        """<dd class="error">Force an error</dd>"""
      )
    }

    "correctly lookup error in messages when _error is supplied as String" in {
      inputText
        .apply(Form(single("foo" -> Forms.text))("foo"), Symbol("_error") -> "error.generalcustomerror")
        .body must contain(
        """<dd class="error">Some &lt;b&gt;general custom&lt;/b&gt; error message</dd>"""
      )
    }

    "correctly lookup error in messages when _error is supplied as String but keep raw html" in {
      inputText
        .apply(Form(single("foo" -> Forms.text))("foo"), Symbol("_error") -> Html("error.generalcustomerror"))
        .body must contain(
        """<dd class="error">Some <b>general custom</b> error message</dd>"""
      )
    }

    "correctly display an error when _error is supplied as Option[String]" in {
      inputText
        .apply(Form(single("foo" -> Forms.text))("foo"), Symbol("_error") -> Option("Force an error"))
        .body must contain(
        """<dd class="error">Force an error</dd>"""
      )
    }

    "correctly lookup error in messages when _error is supplied as Option[String]" in {
      inputText
        .apply(Form(single("foo" -> Forms.text))("foo"), Symbol("_error") -> Option("error.generalcustomerror"))
        .body must contain(
        """<dd class="error">Some &lt;b&gt;general custom&lt;/b&gt; error message</dd>"""
      )
    }

    "correctly lookup error in messages when _error is supplied as Option[String] but keep raw html" in {
      inputText
        .apply(Form(single("foo" -> Forms.text))("foo"), Symbol("_error") -> Option(Html("error.generalcustomerror")))
        .body must contain(
        """<dd class="error">Some <b>general custom</b> error message</dd>"""
      )
    }

    "correctly lookup error in messages when _error is supplied as Optional[String]" in {
      inputText
        .apply(Form(single("foo" -> Forms.text))("foo"), Symbol("_error") -> Optional.of("error.generalcustomerror"))
        .body must contain(
        """<dd class="error">Some &lt;b&gt;general custom&lt;/b&gt; error message</dd>"""
      )
    }

    "correctly lookup error in messages when _error is supplied as Optional[String] but keep raw html" in {
      inputText
        .apply(
          Form(single("foo" -> Forms.text))("foo"),
          Symbol("_error") -> Optional.of(Html("error.generalcustomerror"))
        )
        .body must contain(
        """<dd class="error">Some <b>general custom</b> error message</dd>"""
      )
    }

    "correctly display an error when _error is supplied as Option[FormError]" in {
      inputText
        .apply(Form(single("foo" -> Forms.text))("foo"), Symbol("_error") -> Option(FormError("foo", "Force an error")))
        .body must contain(
        """<dd class="error">Force an error</dd>"""
      )
    }

    "correctly lookup error in messages when _error is supplied as FormError" in {
      inputText
        .apply(
          Form(single("foo" -> Forms.text))("foo"),
          Symbol("_error") -> FormError("foo", "error.generalcustomerror")
        )
        .body must contain(
        """<dd class="error">Some &lt;b&gt;general custom&lt;/b&gt; error message</dd>"""
      )
    }

    "correctly lookup error in messages when _error is supplied as Option[FormError]" in {
      inputText
        .apply(
          Form(single("foo" -> Forms.text))("foo"),
          Symbol("_error") -> Option(FormError("foo", "error.generalcustomerror"))
        )
        .body must contain(
        """<dd class="error">Some &lt;b&gt;general custom&lt;/b&gt; error message</dd>"""
      )
    }

    "don't display an error when _error is supplied but is None" in {
      inputText.apply(Form(single("foo" -> Forms.text))("foo"), Symbol("_error") -> None).body must not contain
        """class="error""""
    }
  }
}
