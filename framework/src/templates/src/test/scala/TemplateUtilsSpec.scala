package play.templates.test

import org.specs2.mutable._

import play.templates._

object TemplateUtilsSpec extends Specification {

  "Templates" should {

    "provide a HASH util" in {
      Hash("itShouldWork".getBytes) must be_==("31c0c4e0e142fe9b605fff44528fedb3dd8ae254")
    }

    "provide a Format API" in {

      "HTML for example" in {

        case class Html(text: String) extends Appendable[Html] {
          val buffer = new StringBuilder(text)
          def +(other: Html) = {
            buffer.append(other.buffer)
            this
          }
          override def toString = buffer.toString
        }

        object HtmlFormat extends Format[Html] {
          def raw(text: String) = Html(text)
          def escape(text: String) = Html(text.replace("<", "&lt;"))
        }

        val html = HtmlFormat.raw("<h1>") + HtmlFormat.escape("Hello <world>") + HtmlFormat.raw("</h1>")

        html.toString must be_==("<h1>Hello &lt;world></h1>")

      }

      "Text for example" in {

        case class Text(text: String) extends Appendable[Text] {
          val buffer = new StringBuilder(text)
          def +(other: Text) = {
            buffer.append(other.buffer)
            this
          }
          override def toString = buffer.toString
        }

        object TextFormat extends Format[Text] {
          def raw(text: String) = Text(text)
          def escape(text: String) = Text(text)
        }

        val text = TextFormat.raw("<h1>") + TextFormat.escape("Hello <world>") + TextFormat.raw("</h1>")

        text.toString must be_==("<h1>Hello <world></h1>")

      }

    }

    "generate proper packages from the directory structure" in {
      import java.io.File

      val baseDir = new File("src/templates/src/test/templates/")
      def haveTemplateName(templateName: String*) =
        be_==(templateName) ^^
        ((fileName: String) =>
         ScalaTemplateCompiler.generatedFile(
           new File(baseDir, fileName),
           baseDir,
           new File("generated-templates")
         )._1.toSeq)

      "on the template dir root" in {
        "simple.scala.html" must haveTemplateName("html", "simple")
      }

      "one level deep" in {
        "example/simple.scala.html" must haveTemplateName("example", "html", "simple")
      }

      "several levels deep" in {
        "com/example/simple.scala.html" must haveTemplateName("com", "example", "html", "simple")
      }
    }
  }

}
