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

  /**
  * Aggregate 2 paths
  * {{{
  *   val __ = Path[JsValue]()
  *   (__ \ "foo" \ "bar").compose(__ \ "baz") == (__ \ "foo" \ "bar" \ "baz")
  * }}}
  */
  def compose(p: Path[I]): Path[I] = Path(this.path ++ p.path)
  def ++(other: Path[I]) = this compose other

  /**
  * Creates a Rule searching and validating data in a stucture of type I
  * When applied, the rule will lookup for data at the given path, and apply the given Constraint on it
  * {{{
  *   val __ = Path[JsValue]()
  *   val json = Json.parse("""{"firstname": "Julien"}""")
  *   val r = (__ \ "firstame").read(nonEmptyText)
  *   r.validate(json) == Success("Julien")
  * }}}
  * @param v the constraint to apply on data
  * @param m a lookup function. This function finds data in a structure of type I, and coerce it to tyoe O
  * @return A Rule validating the presence and validity of data at this Path
  */
  import Mappings._
  def read[J, O](c: Mapping[ValidationError, J, O])(implicit m: Path[I] => Rule[I, J]): Rule[I, O] =
    read(Rule(c))(m)

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
  def read[J, O](sub: Rule[J, O])(implicit r: Path[I] => Rule[I, J]): Rule[I, O] =
    r(this) compose sub

  /**
  * Creates a Rule searching data in a stucture of type I
  * When applied, the rule will lookup for data at the given path, and coerce it to type O
  * {{{
  *   val __ = Path[JsValue]()
  *   val json = Json.parse("""{"company": "typesafe"}""")
  *   val r = (__ \ "company").read[Option[String]]
  *   r.validate(validJson) == Success(Some("typesafe"))
  * }}}
  * @param m a lookup function. This function finds data in a structure of type I, and coerce it to tyoe O
  * @return A Rule validating the presence of data at this Path
  */
  def read[O](implicit r: Path[I] => Rule[I, O]): Rule[I, O] =
    read(Rule[O, O]((o: O) => Success(o)))(r)


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