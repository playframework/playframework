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

  implicit def writeString(p: Path) = Write {
    x: String => Map(asKey(p) -> Seq(x))
  }

  def seq[I, O](w: Write[I, O])(implicit pw: Path => Write[O, Map[String, Seq[String]]]) = (p: Path) => Write {
    ms: Seq[I] => ms.zipWithIndex.toMap.flatMap {
      case (m, i) => pw(p \ i).writes(w.writes(m))
    }
  }

  def option[I, O](w: Write[I, O]) = (p: Path) => Write {
    m: Option[I] => m.map(s => Map(asKey(p) -> Seq(w.writes(s)))).getOrElse(Map.empty)
  }

  // implicit def writeI[I]: Write[I, I] =
  //   Write(identity[I] _)

  // implicit def writeInt: Write[Int, String] =
  //   Write((i: Int) => i.toString)

  // implicit def writeString: Write[String, String] =
  //   Write((i: String) => i)

  // implicit def writeAsSeq[I, O](implicit w: Write[I, O]): Write[I, Seq[O]] =
  //   Write((i: I) => Seq(w.writes(i)))

  // implicit def writeOpt[I, O](implicit w: Write[I, O]): Write[Option[I], Seq[O]] =
  //   Write((i: Option[I]) => i.map(w.writes).toSeq)

  // implicit def writeToMap[I](p: Path)(implicit w: Write[I, Seq[String]]) =
  //   Write((i: I) => Map(asKey(p) -> w.writes(i)))

  // implicit def writeFromMapToMap[I](p: Path)(implicit w: Write[I, Map[String, Seq[String]]]): Write[I, Map[String, Seq[String]]] =
  //   Write((i: I) => w.writes(i).map { case (k, v) =>
  //     (asKey(p) + "." + k) -> v
  //   })

  // implicit def writeSeq[I](p: Path)(implicit w: Write[I, Seq[String]]): Write[Seq[I], Map[String, Seq[String]]] =
  //   Write{(is: Seq[I]) =>
  //     is.zipWithIndex.map { case (i, index) =>
  //       val prefix = asKey(p \ index)
  //       prefix -> w.writes(i)
  //     }.toMap
  //   }

  // // implicit def writeJson[I](p: Path[JsValue])(implicit w: Write[I, JsValue]): Write[I, JsValue] = ???

  // implicit def writeSeqToMap[I](p: Path)(implicit w: Write[I, Map[String, Seq[String]]]): Write[Seq[I], Map[String, Seq[String]]] =
  //   Write{(is: Seq[I]) =>
  //     val ms = is.zipWithIndex.map { case (i, index) =>
  //       val prefix = asKey(p \ index)
  //       w.writes(i).map{ case (k, v) => (prefix + "." + k) -> v }
  //     }
  //     ms.foldLeft(Map.empty[String, Seq[String]])((acc, m) => acc ++ m)
  //   }
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
