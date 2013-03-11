package play.router

import scala.util.parsing.input._
import scala.util.parsing.combinator._
import scala.util.matching._

/**
 * captures URL parts
 */
trait PathPart

case class DynamicPart(name: String, constraint: String, encode: Boolean) extends PathPart with Positional {
  override def toString = """DynamicPart("""" + name + "\", \"\"\"" + constraint + "\"\"\"," + encode + ")" //"
}

case class StaticPart(value: String) extends PathPart {
  override def toString = """StaticPart("""" + value + """")"""
}

case class PathPattern(parts: Seq[PathPart]) {
  def has(key: String): Boolean = parts.exists {
    case DynamicPart(name, _, _) if name == key => true
    case _ => false
  }

  override def toString = parts.map {
    case DynamicPart(name, constraint, encode) => "$" + name + "<" + constraint + ">"
    case StaticPart(path) => path
  }.mkString

}
