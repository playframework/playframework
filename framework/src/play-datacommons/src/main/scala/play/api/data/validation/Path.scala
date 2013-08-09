package play.api.data.validation

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
  def unapply[I](path: Path[I]): Option[(PathNode, Path[I])] = {
    path match {
      case Path(Nil) => None
      case Path(n :: ns) => Some(n -> Path[I](ns))
    }
  }
}

object Path {
  def apply[I](path: String) = new Path[I](KeyPathNode(path) :: Nil)
	def apply[I](path: List[PathNode] = Nil) = new Path[I](path)
	def unapply[I](p: Path[I]): Option[List[PathNode]] = Some(p.path)
}

class Path[I](val path: List[PathNode]) {

  def \(key: String): Path[I] = this \ KeyPathNode(key)
  def \(idx: Int): Path[I] = this \ IdxPathNode(idx)
  def \(child: PathNode): Path[I] = Path(path :+ child)

  def as[J] = Path[J](path)

  def compose(p: Path[I]): Path[I] = Path(this.path ++ p.path)
  def ++(other: Path[I]) = this compose other

  def read[O](v: Constraint[O])(implicit m: Path[I] => Mapping[ValidationError, I, O]): Rule[I, O] =
    Rule(this, (p: Path[I]) => (d: I) => m(p)(d).fail.map{ errs => Seq(p -> errs) }, v) // XXX: DRY "fail.map" thingy

  def read[J, O](sub: Rule[J, O])(implicit l: Path[I] => Mapping[ValidationError, I, J]): Rule[I, O] = {
    val parent = this
    Rule(parent compose Path[I](sub.p.path), { p => d =>
      val v = l(parent)(d)
      v.fold(
        es => Failure(Seq(parent -> es)),
        s  => sub.m(sub.p)(s)).fail.map{ _.map {
          case (path, errs) => (parent compose Path[I](path.path)) -> errs
        }}
    }, sub.v)
  }

  def read[O](implicit m: Path[I] => Mapping[ValidationError, I, O]): Rule[I, O] =
    read(Constraints.noConstraint[O])

  def write[In](implicit w: Path[I] => Writes[In, I]) = w(this)

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
      val j = o.asInstanceOf[Path[I]]
      this.path == j.path
    }
    else
      false
  }
  def canEqual(o: Any) = o.isInstanceOf[Path[I]]
}