package play.templates.test

import org.specs2.mutable._

import play.templates._

object TemplateParserSpec extends Specification {

  "The template parser" should {

    import scala.util.parsing.input.CharSequenceReader

    val parser = ScalaTemplateCompiler.templateParser

    def get(templateName: String) = {
      new CharSequenceReader(scalax.file.Path.fromString("src/templates-compiler/src/test/templates/" + templateName).string)
    }

    def parse(templateName: String) = {
      parser.parser(get(templateName))
    }

    def parseString(template: String) = parser.parser(new CharSequenceReader(template))

    def parseStringSuccess(template: String) = parseString(template) must beLike {
      case parser.Success(_, rest) if rest.atEnd => ok
    }

    "succeed for" in {

      "static.scala.html" in {
        parse("static.scala.html") must beLike({ case parser.Success(_, rest) => if (rest.atEnd) ok else ko })
      }

      "simple.scala.html" in {
        parse("simple.scala.html") must beLike({ case parser.Success(_, rest) => if (rest.atEnd) ok else ko })
      }

      "complicated.scala.html" in {
        parse("complicated.scala.html") must beLike({ case parser.Success(_, rest) => if (rest.atEnd) ok else ko })
      }

      "brackets in strings" in {
        "open" in parseStringSuccess("""@foo("(")""")
        "close" in parseStringSuccess("""@foo(")@")""")
      }
    }

    "fail for" in {

      "unclosedBracket.scala.html" in {
        parse("unclosedBracket.scala.html") must beLike({
          case parser.NoSuccess(msg, rest) => {
            if (msg == "Unmatched bracket" && rest.pos.line == 8 && rest.pos.column == 12) ok else ko
          }
        })
      }

      "unclosedBracket2.scala.html" in {
        parse("unclosedBracket2.scala.html") must beLike({
          case parser.NoSuccess(msg, rest) => {
            if (msg == "Unmatched bracket" && rest.pos.line == 13 && rest.pos.column == 20) ok else ko
          }
        })
      }

      "invalidAt.scala.html" in {
        parse("invalidAt.scala.html") must beLike({
          case parser.NoSuccess(msg, rest) => {
            if (msg.contains("identifier' expected but `<' found") && rest.pos.line == 5 && rest.pos.column == 6) ok else ko
          }
        })
      }

    }

  }

}
