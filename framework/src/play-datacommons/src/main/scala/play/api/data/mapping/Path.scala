package play.api.data.mapping

import scala.language.higherKinds
import scala.language.implicitConversions

sealed trait PathNode
case class KeyPathNode(key: String) extends PathNode {
  override def toString = key
}

case class IdxPathNode(idx: Int) extends PathNode {
  override def toString = s"[$idx]"
}

case class RecursiveSearch(key: String) extends PathNode {
  override def toString = "//" + key
}

object \: {
  def unapply(path: Path): Option[(Path, Path)] = {
    path match {
      case Path(n :: ns) => Some((Path() \ n) -> Path(ns))
    }
  }
}

case object Path {
  def apply(path: String) = new Path(KeyPathNode(path) :: Nil)
	def apply(path: List[PathNode] = Nil) = new Path(path)

  def \(path: String) = new Path(KeyPathNode(path) :: Nil)
  def \(path: List[PathNode] = Nil) = new Path(path)

	def unapply(p: Path): Option[List[PathNode]] = Some(p.path)
}

class Path(val path: List[PathNode]) {

  def \(key: String): Path = this \ KeyPathNode(key)
  def \(idx: Int): Path = this \ IdxPathNode(idx)
  def \(child: PathNode): Path = Path(path :+ child)

  /**
  * Aggregate 2 paths
  * {{{
  *   val __ = Path[JsValue]()
  *   (__ \ "foo" \ "bar").compose(__ \ "baz") == (__ \ "foo" \ "bar" \ "baz")
  * }}}
  */
  def compose(p: Path): Path = Path(this.path ++ p.path)
  def ++(other: Path) = this compose other

  /**
  * When applied, the rule will lookup for data at the given path, and apply the given Constraint on it
  * {{{
  *   val __ = Path[JsValue]()
  *   val json = Json.parse("""{
  *      "informations": {
  *        "label": "test"
  *      }
  *   }""")
  *   val infoValidation = (__ \ "label").read(nonEmptyText)
  *   val v = (__ \ "informations").read(infoValidation))
  *   v.validate(json) == Success("test")
  * }}}
  * @param sub the constraint to apply on the subdata
  * @param l a lookup function. This function finds data in a structure of type I, and coerce it to tyoe O
  * @return A Rule validating the presence and validity of data at this Path
  */
  def read[I, J, O](sub: Rule[J, O])(implicit r: Path => Rule[I, J]): Rule[I, O] =
    r(this).compose(this)(sub)

  def read[I, O](r: Path => Rule[I, O]): Rule[I, O] =
    read(Rule.zero[O])(r)

  /**
  * Creates a Writes the serialize data to the desired type
  * {{{
  *   val contact = Contact("Julien", "Tournay")
  *   val __ = Path[Map[String, Seq[String]]]()
  *   implicit def contactWrite = (__ \ "firstname").write[String]
  *   Writes[Contact, Map[String, Seq[String]]](contact) mustEqual Map("firstname" -> "Julien")
  * }}}
  * @param m a lookup function. This function finds data in a structure of type I, and coerce it to tyoe O
  * @return A Rule validating the presence of data at this Path
  */
  def write[I, O](w: Path => Write[I, O]): Write[I, O] = w(this)
  def write[I, J, O](format: Write[I, J])(implicit w: Path => Write[J, O]): Write[I, O] =
    Write((w(this).writes _) compose (format.writes _))

  override def toString = this.path match {
    case Nil => "/"
    case hs => hs.foldLeft("") {
      case (path, IdxPathNode(i)) => path + s"[$i]"
      case (path, KeyPathNode(k)) => path + "/" + k
      case (path, RecursiveSearch(k)) => path + "//" + k
    }
  }

  override def hashCode = path.hashCode
  override def equals(o: Any) = {
    if(canEqual(o)) {
      val j = o.asInstanceOf[Path]
      this.path == j.path
    }
    else
      false
  }
  def canEqual(o: Any) = o.isInstanceOf[Path]
}