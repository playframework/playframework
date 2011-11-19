package play.templates.test

import org.specs2.mutable._

import play.templates._

object TemplateMagicTest extends Specification {

  "TemplateMagic" should {

    val magic = TemplateMagic(new java.util.Locale("en"))
    import magic._

    case class Html(text: String) extends Appendable[Html] {
      val buffer = new StringBuilder(text)
      def +(other: Html) = {
        buffer.append(other.buffer)
        this
      }
      override def toString = buffer.toString
    }

    implicit object HtmlFormat extends Format[Html] {
      def raw(text: String) = Html(text)
      def escape(text: String) = Html(text.replace("<", "&lt;"))
    }

    "enrich Dates with" in {
      val date = new java.util.Date(1275910970000L)
      "a format method using the default locale" in {
        date.format("dd MMMM yyyy hh:mm:ss") must be_==("07 June 2010 01:42:50")
      }
      "a localized format method" in {
        date.format("dd MMMM yyyy hh:mm:ss", "fr") must be_==("07 juin 2010 01:42:50")
      }
    }

    "enrich Strings with" in {
      val str = "foo"
      "a when method" in {
        str.when(false) must be_==("")
        str.when(true) must be_==(str)
      }
    }

    "enrich Iterables with" in {
      val list = List(1, 2, 3)
      val oneElt = List(42)
      val empty = List.empty[Int]

      "a pluralize method" in {
        list.pluralize must be_==("s")
        oneElt.pluralize must be_==("")
        empty.pluralize must be_==("s")
      }

      "a pluralize method with a custom plural" in {
        list.pluralize("es") must be_==("es")
        oneElt.pluralize("es") must be_==("")
        empty.pluralize("es") must be_==("es")
      }

      "a pluralize method with custom plural and singular" in {
        list.pluralize("al", "aux") must be_==("aux")
        oneElt.pluralize("al", "aux") must be_==("al")
        empty.pluralize("al", "aux") must be_==("aux")
      }
    }

    "enrich Numerics with" in {

      "an asDate method using the default locale" in {
        (1275910970000L asDate "dd MMMM yyyy hh:mm:ss") must be_==("07 June 2010 01:42:50")
      }

      "an asDate method with a custom locale" in {
        (1275910970000L asDate ("dd MMMM yyyy hh:mm:ss", "fr")) must be_==("07 juin 2010 01:42:50")
      }

      "an formatSize method" in {
        726016.formatSize must be_==("709KB")
        (42 * 1024L * 1024L).formatSize must be_==("42MB")
      }

      "a divisibleBy method" in {
        (42 divisibleBy 7) must be_==(true)
        (42 divisibleBy 9) must be_==(false)
      }

      "a format method" in {
        (42 format "000.00") must be_==("042.00")
      }

      "a page method" in {
        (42 page 10) must be_==(5)
        (1 page 10) must be_==(1)
        (40 page 10) must be_==(4)
      }

      "a pluralize method" in {
        (3.pluralize) must be_==("s")
        (1.pluralize) must be_==("")
        (0.pluralize) must be_==("s")
      }

      "a pluralize method with a custom plural" in {
        (3.pluralize("es")) must be_==("es")
        (1.pluralize("es")) must be_==("")
        (0.pluralize("es")) must be_==("es")
      }

      "a pluralize method with a custom plural and singular" in {
        (3.pluralize("al", "aux")) must be_==("aux")
        (1.pluralize("al", "aux")) must be_==("al")
        (0.pluralize("al", "aux")) must be_==("aux")
      }
    }

    "enrich Maps with" in {
      val map = Map("id" -> 42, "color" -> "red")

      "an asAttr method" in {
        val attrs = map.asAttr.toString
        attrs must contain("""id="42" """)
        attrs must contain("""color="red" """)
      }

      "a conditional asAttr method" in {
        map.asAttr(false).toString must be_==("")
        val attrs = map.asAttr(true).toString
        attrs must contain("""id="42" """)
        attrs must contain("""color="red" """)
      }
    }

    "enrich any type with" in {
      "an addSlashes method" in {
        """single quote (') double quote (")""".addSlashes must be_==("""single quote (\') double quote (\")""")
      }

      "a capAll method" in {
        "lorum ipsum dolor".capAll must be_==("Lorum Ipsum Dolor")
      }

      "a capFirst method" in {
        "lorum ipsum dolor".capFirst must be_==("Lorum ipsum dolor")
      }

      "a nl2br method" in {
        """one
          |two""".stripMargin.nl2br.toString must be_==("one<br/>two")
      }
    }

    "enrich Strings with" in {
      "a capitalizeWords method" in {
        "lorum ipsum dolor".capitalizeWords must be_==("Lorum Ipsum Dolor")
      }

      "an escapeJavaScript method" in {
        """single quote (') double quote (")""".escapeJavaScript must be_==("""single quote (\') double quote (\")""")
      }
    }
  }
}