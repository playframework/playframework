package play.router

import scala.util.parsing.input._
import scala.util.parsing.combinator._
import scala.util.matching._

/**
 * captures URL parts
 */
trait PathPart

case class DynamicPart(name: String, constraint: String) extends PathPart with Positional {
  override def toString = """DynamicPart("""" + name + "\", \"\"\"" + constraint + "\"\"\")" //"
}

case class StaticPart(value: String) extends PathPart {
  override def toString = """StaticPart("""" + value + """")"""
}

case class PathPattern(parts: Seq[PathPart]) {

  import java.util.regex._

  lazy val (regex, groups) = {
    Some(parts.foldLeft("", Map.empty[String, Int], 0) { (s, e) =>
      e match {
        case StaticPart(p) => ((s._1 + Pattern.quote(p)), s._2, s._3)
        case DynamicPart(k, r) => {
          ((s._1 + "(" + r + ")"), (s._2 + (k -> (s._3 + 1))), s._3 + 1 + Pattern.compile(r).matcher("").groupCount)
        }
      }
    }).map {
      case (r, g, _) => Pattern.compile("^" + r + "$") -> g
    }.get
  }

  def apply(path: String): Option[Map[String, String]] = {
    val matcher = regex.matcher(path)
    if (matcher.matches) {
      Some(groups.map {
        case (name, g) => name -> matcher.group(g)
      }.toMap)
    } else {
      None
    }
  }

  def has(key: String): Boolean = parts.exists {
    case DynamicPart(name, _) if name == key => true
    case _ => false
  }

  override def toString = parts.map {
    case DynamicPart(name, constraint) => "$" + name + "<" + constraint + ">"
    case StaticPart(path) => path
  }.mkString

}
