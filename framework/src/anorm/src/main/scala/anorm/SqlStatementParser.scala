/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package anorm

import scala.language.postfixOps

import scala.util.parsing.combinator._

/** Parser for SQL statement. */
object SqlStatementParser extends JavaTokenParsers {

  /**
   * Returns updated statement and associated parameter names.
   * Extracts parameter names from {placeholder}s, replaces with ?.
   *
   * {{{
   * import anorm.SqlStatementParser.parse
   *
   * val parsed: (String, List[String]) =
   *   parse("SELECT * FROM schema.table WHERE name = {name} AND cat = ?")
   * // parsed ==
   * //   ("SELECT * FROM schema.table WHERE name = ? AND cat = ?" ->
   * //    List("name"))
   * }}}
   */
  def parse(sql: String): (String, List[String]) = {
    val r = parse(instr, sql.trim().replace("\r", "").replace("\n", " ")).get
    (r.flatMap(_._1).mkString, (r.flatMap(_._2)))
  }

  private val instr: Parser[List[(String, Option[String])]] = rep(literal | variable | other)

  private val literal: Parser[(String, Option[String])] = (stringLiteral | simpleQuotes) ^^ { case s => (s, None) }

  private val variable = "{" ~> (ident ~ (("." ~> ident)?)) <~ "}" ^^ {
    case i1 ~ i2 => ("%s": String, Some(i1 + i2.map("." + _).getOrElse("")))
  }

  private val other: Parser[(String, Option[String])] = """.""".r ^^ {
    case element => (element, None)
  }

  private val simpleQuotes = ("'" + """([^'\p{Cntrl}\\]|\\[\\/bfnrt]|\\u[a-fA-F0-9]{4})*""" + "'").r

  override def skipWhitespace = false
}
