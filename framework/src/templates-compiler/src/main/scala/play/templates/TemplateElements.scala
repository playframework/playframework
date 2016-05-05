//Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
package play.templates

import scala.util.parsing.input.Positional

trait TemplateElements {

  abstract class TemplateTree
  abstract class ScalaExpPart

  case class Params(code: String) extends Positional
  case class Template(name: PosString, comment: Option[Comment], params: PosString, imports: Seq[Simple], defs: Seq[Def], sub: Seq[Template], content: Seq[TemplateTree]) extends Positional
  case class PosString(str: String) extends Positional {
    override def toString = str
  }
  case class Def(name: PosString, params: PosString, code: Simple) extends Positional
  case class Plain(text: String) extends TemplateTree with Positional
  case class Display(exp: ScalaExp) extends TemplateTree with Positional
  case class Comment(msg: String) extends TemplateTree with Positional
  case class ScalaExp(parts: Seq[ScalaExpPart]) extends TemplateTree with Positional
  case class Simple(code: String) extends ScalaExpPart with Positional
  case class Block(whitespace: String, args: Option[PosString], content: Seq[TemplateTree]) extends ScalaExpPart with Positional
  case class Value(ident: PosString, block: Block) extends Positional
}