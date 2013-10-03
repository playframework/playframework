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
      case Path(n :: ns) => Some((Path \ n) -> Path(ns))
      case Path(Nil) => None
    }
  }
}

case object Path extends Path(Nil) {
  def apply(path: String) = new Path(KeyPathNode(path) :: Nil)
	def apply(path: List[PathNode] = Nil) = new Path(path)
	def unapply(p: Path): Option[List[PathNode]] = Some(p.path)
}

class Path(val path: List[PathNode]) {

  def \(key: String): Path = this \ KeyPathNode(key)
  def \(idx: Int): Path = this \ IdxPathNode(idx)
  def \(child: PathNode): Path = Path(path :+ child)

  /**
  * Aggregate 2 paths
  * {{{
  *   (Path \ "foo" \ "bar").compose(Path \ "baz") == (Path \ "foo" \ "bar" \ "baz")
  * }}}
  */
  def compose(p: Path): Path = Path(this.path ++ p.path)
  def ++(other: Path) = this compose other

  def read[I, J, O](sub: Rule[J, O])(implicit r: Path => Rule[I, J]): Rule[I, O] =
    Formatter[I](this).read(sub)

  def read[I, O](implicit r: Path => Rule[I, O]): Rule[I, O] =
    Formatter[I](this).read[O]

  /**
  * Creates a Writes the serialize data to the desired type
  * {{{
  *   val contact = Contact("Julien", "Tournay")
  *   implicit def contactWrite = (Path \ "firstname").write[String, UrlFormEncoded]
  *   contactWrite.writes(contact) mustEqual Map("firstname" -> "Julien")
  * }}}
  * @param m a lookup function. This function finds data in a structure of type I, and coerce it to tyoe O
  * @return A Rule validating the presence of data at this Path
  */
  def write[O, I](implicit w: Path => Write[O, I]): Write[O, I] =
    Formatter[I](this).write(w)

  def write[O, J, I](format: Write[O, J])(implicit w: Path => Write[J, I]): Write[O, I] =
    Formatter[I](this).write(format)

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