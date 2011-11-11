package anorm

import scala.util.parsing.combinator._

object SqlStatementParser extends JavaTokenParsers {

  def parse(in: String): (String, List[String]) = {
    val r = parse(instr, in.trim().replace("\r", "").replace("\n", " ")).get
    (r.flatMap(_._1).mkString, (r.flatMap(_._2)))
  }

  def instr = rep(literal | variable | other)

  def literal = (stringLiteral | simpleQuotes) ^^ { case s => (s, None) }

  def variable = "{" ~> (ident ~ (("." ~> ident)?)) <~ "}" ^^ {
    case i1 ~ i2 => ("?": String, Some(i1 + i2.map("." + _).getOrElse("")))
  }

  def other = """.""".r ^^ {
    case element => (element, None)
  }

  def simpleQuotes = ("'" + """([^'\p{Cntrl}\\]|\\[\\/bfnrt]|\\u[a-fA-F0-9]{4})*""" + "'").r

  override def skipWhitespace = false

}

