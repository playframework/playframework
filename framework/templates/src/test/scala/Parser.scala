package play.templates.test

import org.specs._
import play.templates._

object TemplateParser extends Specification("Template parser") {
    
    "The template parser" should {
        
        import scala.util.parsing.input.CharSequenceReader
        
        val parser = ScalaTemplateCompiler.templateParser
        
        def get(templateName:String) = {
            new CharSequenceReader(scalax.file.Path("templates/src/test/templates/" + templateName).slurpString)
        }
        
        def parse(templateName:String) = {
            parser.parser(get(templateName))
        }
        
        val fullyParsed:PartialFunction[parser.ParseResult[ScalaTemplateCompiler.Template],Boolean] = {
            case parser.Success(_, rest) => rest.atEnd
        }
        
        def failAt(message:String,line:Int,column:Int):PartialFunction[parser.ParseResult[ScalaTemplateCompiler.Template],Boolean] = {
            case parser.NoSuccess(msg, rest) => {
                message == msg && rest.pos.line == line && rest.pos.column == column
            }
        }
        
        "success for" in {
            
            "static.scala.html" in {
                parse("static.scala.html") must beLike(fullyParsed)
            }
            
            "simple.scala.html" in {
                parse("simple.scala.html") must beLike(fullyParsed)
            }
            
            "complicated.scala.html" in {
                parse("complicated.scala.html") must beLike(fullyParsed)
            }
            
        }
        
        "fail for" in {
            
            "unclosedBracket.scala.html" in {
                parse("unclosedBracket.scala.html") must beLike(failAt("Unmatched bracket", 8, 12))
            }
            
            "unclosedBracket2.scala.html" in {
                parse("unclosedBracket2.scala.html") must beLike(failAt("Unmatched bracket", 13, 20))
            }
            
            "invalidAt.scala.html" in {
                parse("invalidAt.scala.html") must beLike(failAt("`identifier' expected but `<' found", 5, 6))
            }
            
        }
        
    }
    
}