//Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
package play.templates

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.language.postfixOps
import scala.language.reflectiveCalls
import scala.util.parsing.combinator.JavaTokenParsers
import scala.util.parsing.input.OffsetPosition

trait TemplateParser { self: TemplateElements =>

  class TemplateParser extends JavaTokenParsers {

    def as[T](parser: Parser[T], error: String) = {
      Parser(in => parser(in) match {
        case s @ Success(_, _) => s
        case Failure(_, next) => Failure("`" + error + "' expected but `" + next.first + "' found", next)
        case Error(_, next) => Error(error, next)
      })
    }

    def several[T](p: => Parser[T]): Parser[List[T]] = Parser { in =>
      import scala.collection.mutable.ListBuffer
      val elems = new ListBuffer[T]
      def continue(in: Input): ParseResult[List[T]] = {
        val p0 = p // avoid repeatedly re-evaluating by-name parser
        @tailrec
        def applyp(in0: Input): ParseResult[List[T]] = p0(in0) match {
          case Success(x, rest) =>
            elems += x; applyp(rest)
          case Failure(_, _) => Success(elems.toList, in0)
          case err: Error => err
        }
        applyp(in)
      }
      continue(in)
    }

    def at = "@"

    def eof = """\Z""".r

    def newLine = (("\r"?) ~> "\n")

    def identifier = as(ident, "identifier")

    def whiteSpaceNoBreak = """[ \t]+""".r

    def escapedAt = at ~> at

    def any = {
      Parser(in => if (in.atEnd) {
        Failure("end of file", in)
      } else {
        Success(in.first, in.rest)
      })
    }

    def plain: Parser[Plain] = {
      positioned(
        ((escapedAt | (not(at) ~> (not("{" | "}") ~> any))) +) ^^ {
          case charList => Plain(charList.mkString)
        })
    }

    def squareBrackets: Parser[String] = {
      "[" ~ (several((squareBrackets | not("]") ~> any))) ~ commit("]") ^^ {
        case p1 ~ charList ~ p2 => p1 + charList.mkString + p2
      }
    }

    def parentheses: Parser[String] = {
      "(" ~ several(stringLiteral | parentheses | not(")") ~> any) ~ commit(")") ^^ {
        case p1 ~ charList ~ p2 => p1 + charList.mkString + p2
      }
    }

    def comment: Parser[Comment] = {
      positioned((at ~ "*") ~> ((not("*@") ~> any *) ^^ { case chars => Comment(chars.mkString) }) <~ ("*" ~ at))
    }

    def brackets: Parser[String] = {
      ensureMatchedBrackets((several((brackets | not("}") ~> any)))) ^^ {
        case charList => "{" + charList.mkString + "}"
      }
    }

    def ensureMatchedBrackets[T](p: Parser[T]): Parser[T] = Parser { in =>
      val pWithBrackets = "{" ~> p <~ ("}" | eof ~ err("EOF"))
      pWithBrackets(in) match {
        case s @ Success(_, _) => s
        case f @ Failure(_, _) => f
        case Error("EOF", _) => Error("Unmatched bracket", in)
        case e: Error => e
      }
    }

    def block: Parser[Block] = {
      positioned(
        (whiteSpaceNoBreak?) ~ ensureMatchedBrackets((blockArgs?) ~ several(mixed)) ^^ {
          case w ~ (args ~ content) => Block(w.getOrElse(""), args, content.flatten)
        })
    }

    def blockArgs: Parser[PosString] = positioned((not("=>" | newLine) ~> any *) ~ "=>" ^^ { case args ~ arrow => PosString(args.mkString + arrow) })

    def methodCall: Parser[String] = identifier ~ (squareBrackets?) ~ (parentheses?) ^^ {
      case methodName ~ types ~ args => methodName + types.getOrElse("") + args.getOrElse("")
    }

    def expression: Parser[Display] = {
      at ~> commit(positioned(methodCall ^^ { case code => Simple(code) })) ~ several(expressionPart) ^^ {
        case first ~ parts => Display(ScalaExp(first :: parts))
      }
    }

    def expressionPart: Parser[ScalaExpPart] = {
      chainedMethods | block | (whiteSpaceNoBreak ~> scalaBlockChained) | elseCall | positioned[Simple]((parentheses ^^ { case code => Simple(code) }))
    }

    def chainedMethods: Parser[Simple] = {
      positioned(
        "." ~> rep1sep(methodCall, ".") ^^ {
          case calls => Simple("." + calls.mkString("."))
        })
    }

    def elseCall: Parser[Simple] = {
      (whiteSpaceNoBreak?) ~> positioned("else" ^^ { case e => Simple(e) }) <~ (whiteSpaceNoBreak?)
    }

    def safeExpression: Parser[Display] = {
      at ~> positioned(parentheses ^^ { case code => Simple(code) }) ^^ {
        case code => Display(ScalaExp(code :: Nil))
      }
    }

    def matchExpression: Parser[Display] = {
      val simpleExpr: Parser[List[ScalaExpPart]] = positioned(methodCall ^^ { Simple(_) }) ~ several(expressionPart) ^^ {
        case first ~ parts => first :: parts
      }
      val complexExpr = positioned(parentheses ^^ { expr => (Simple(expr)) }) ^^ { List(_) }

      at ~> ((simpleExpr | complexExpr) ~ positioned((whiteSpaceNoBreak ~ "match" ^^ { case w ~ m => Simple(w + m) })) ^^ {
        case e ~ m => e ++ Seq(m)
      }) ~ block ^^ {
        case expr ~ block => Display(ScalaExp(expr ++ Seq(block)))
      }
    }

    def forExpression: Parser[Display] = {
      at ~> positioned("for" ~ parentheses ^^ { case f ~ p => Simple(f + p + " yield ") }) ~ block ^^ {
        case expr ~ block => {
          Display(ScalaExp(List(expr, block)))
        }
      }
    }

    def caseExpression: Parser[ScalaExp] = {
      (whiteSpace?) ~> positioned("""case (.+)=>""".r ^^ { case c => Simple(c) }) ~ block <~ (whiteSpace?) ^^ {
        case pattern ~ block => ScalaExp(List(pattern, block))
      }
    }

    def importExpression: Parser[Simple] = {
      at ~> positioned("""import .*(\r)?\n""".r ^^ {
        case stmt => Simple(stmt)
      })
    }

    def scalaBlock: Parser[Simple] = {
      at ~> positioned(
        brackets ^^ { case code => Simple(code) })
    }

    def scalaBlockChained: Parser[Block] = {
      scalaBlock ^^ {
        case code => Block("", None, ScalaExp(code :: Nil) :: Nil)
      }
    }

    def scalaBlockDisplayed: Parser[Display] = {
      scalaBlock ^^ {
        case code => Display(ScalaExp(code :: Nil))
      }
    }

    def positionalLiteral(s: String): Parser[Plain] = new Parser[Plain] {
      def apply(in: Input) = {
        val offset = in.offset
        val result = literal(s)(in)
        result match {
          case Success(s, r) => {
            val plainString = Plain(s)
            plainString.pos = new OffsetPosition(in.source, offset)
            Success(plainString, r)
          }
          case Failure(s, t) => Failure(s, t)
          case Error(s, t) => Failure(s, t)
        }
      }
    }

    def mixed: Parser[Seq[TemplateTree]] = {
      ((comment | scalaBlockDisplayed | caseExpression | matchExpression | forExpression | safeExpression | plain | expression) ^^ { case t => List(t) }) |
        (positionalLiteral("{") ~ several(mixed) ~ positionalLiteral("}")) ^^ { case p1 ~ content ~ p2 => { p1 +: content.flatten :+ p2 } }
    }

    def template: Parser[Template] = {
      templateDeclaration ~ """[ \t]*=[ \t]*[{]""".r ~ templateContent <~ "}" ^^ {
        case declaration ~ assign ~ content => {
          Template(declaration._1, None, declaration._2, content._1, content._2, content._3, content._4)
        }
      }
    }

    def localDef: Parser[Def] = {
      templateDeclaration ~ """[ \t]*=[ \t]*""".r ~ scalaBlock ^^ {
        case declaration ~ w ~ code => {
          Def(declaration._1, declaration._2, code)
        }
      }
    }

    def templateDeclaration: Parser[(PosString, PosString)] = {
      at ~> positioned(identifier ^^ { case s => PosString(s) }) ~ positioned(opt(squareBrackets) ~ several(parentheses) ^^ { case t ~ p => PosString(t.getOrElse("") + p.mkString) }) ^^ {
        case name ~ params => name -> params
      }
    }

    def templateContent: Parser[(List[Simple], List[Def], List[Template], List[TemplateTree])] = {
      (several(importExpression | localDef | template | mixed)) ^^ {
        case elems => {
          elems.foldLeft((List[Simple](), List[Def](), List[Template](), List[TemplateTree]())) { (s, e) =>
            e match {
              case i: Simple => (s._1 :+ i, s._2, s._3, s._4)
              case d: Def => (s._1, s._2 :+ d, s._3, s._4)
              case v: Template => (s._1, s._2, s._3 :+ v, s._4)
              case c: Seq[_] => (s._1, s._2, s._3, s._4 ++ c.asInstanceOf[Seq[TemplateTree]])
            }
          }
        }
      }
    }

    def parser: Parser[Template] = {
      opt(comment) ~ opt(whiteSpace) ~ opt(at ~> positioned((parentheses+) ^^ { case s => PosString(s.mkString) })) ~ templateContent ^^ {
        case comment ~ _ ~ args ~ content => {
          Template(PosString(""), comment, args.getOrElse(PosString("()")), content._1, content._2, content._3, content._4)
        }
      }
    }

    override def skipWhitespace = false

  }
}