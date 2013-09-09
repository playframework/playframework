package play.api.data.mapping

import scala.language.implicitConversions

// TODO: this is really just a function...
trait Write[I, O] {
  def writes(i: I): O
}

trait DefaultMonoids {
  import play.api.libs.functional.Monoid

  implicit def mapMonoid = new Monoid[Map[String, Seq[String]]] {
    def append(a1: Map[String, Seq[String]], a2: Map[String, Seq[String]]) = a1 ++ a2
    def identity = Map.empty
  }
}

trait DefaultWrites {
  import play.api.libs.functional.Monoid

  import PM._

  def string = Write(identity[String] _)
  def int = Write((i: Int) => i.toString)

  // TODO
  /*
  implicit def jsonWrites[I](implicit w: JSWrites[I]) =
    Writes((i: I) => w.writes(i))
  */

  implicit def writeString(p: Path) = Write { x: String =>
    Map(asKey(p) -> Seq(x))
  }

  implicit def writeSeq[O](p: Path)(implicit w: Path => Write[O, Rules.M]) = Write{ os: Seq[O] =>
    os.zipWithIndex
      .toMap
      .flatMap{ case(o, i) =>
        w(p \ i).writes(o)
      }
  }

  implicit def writeMap(path: Path) = Write{ m: Map[String, Seq[String]] =>
    toM(toPM(m).map{ case (p, v) => (path ++ p) -> v })
  }

  def seq[I, O](w: Write[I, O]) = Write {
    (_: Seq[I]).map(w.writes _)
  }

  def option[I, O](w: Write[I, O]) = (p: Path) => Write {
    m: Option[I] => m.map(s => Map(asKey(p) -> Seq(w.writes(s)))).getOrElse(Map.empty)
  }

  def writeI[I]: Write[I, I] =
    Write(identity[I] _)
}

object Write extends DefaultWrites with DefaultMonoids {

  def apply[I, O](w: I => O): Write[I, O] = new Write[I, O] {
    def writes(i: I) = w(i)
  }

  def apply[I, O](i: I)(implicit w: Write[I, O]): O =
    w.writes(i)

  import play.api.libs.functional._
  implicit def functionalCanBuildWrite[O](implicit m: Monoid[O]) = new FunctionalCanBuild[({type f[I] = Write[I, O]})#f] {
    def apply[A, B](wa: Write[A, O], wb: Write[B, O]): Write[A ~ B, O] = Write[A ~ B, O] { (x: A ~ B) =>
      x match {
        case a ~ b => m.append(wa.writes(a), wb.writes(b))
      }
    }
  }

  implicit def contravariantfunctorOWrite[O] = new ContravariantFunctor[({type f[I] = Write[I, O]})#f] {
    def contramap[A, B](wa: Write[A, O], f: B => A): Write[B, O] = Write[B, O]( (b: B) => wa.writes(f(b)) )
  }

  import play.api.libs.functional.syntax._
  implicit def fbow[I, O](a: Write[I, O])(implicit m: Monoid[O]) = toFunctionalBuilderOps[({type f[I] = Write[I, O]})#f, I](a)

}
