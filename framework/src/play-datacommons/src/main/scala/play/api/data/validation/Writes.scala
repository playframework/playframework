package play.api.data.mapping

import scala.language.implicitConversions

trait Writes[I, O] {
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

  def asKey(p: Path) = p.path.head.toString + p.path.tail.foldLeft("") {
    case (path, IdxPathNode(i)) => path + s"[$i]"
    case (path, KeyPathNode(k)) => path + "." + k
  }

  // TODO
  /*
  implicit def jsonWrites[I](implicit w: JSWrites[I]) =
    Writes((i: I) => w.writes(i))
  */

  implicit def writeInt: Writes[Int, String] =
    Writes((i: Int) => i.toString)

  implicit def writeString: Writes[String, String] =
    Writes((i: String) => i)

  implicit def writeAsSeq[I, O](implicit w: Writes[I, O]): Writes[I, Seq[O]] =
    Writes((i: I) => Seq(w.writes(i)))

  implicit def writeOpt[I, O](implicit w: Writes[I, O]): Writes[Option[I], Seq[O]] =
    Writes((i: Option[I]) => i.map(w.writes).toSeq)

  implicit def writeToMap[I](p: Path)(implicit w: Writes[I, Seq[String]]) =
    Writes((i: I) => Map(asKey(p) -> w.writes(i)))

  implicit def writeFromMapToMap[I](p: Path)(implicit w: Writes[I, Map[String, Seq[String]]]): Writes[I, Map[String, Seq[String]]] =
    Writes((i: I) => w.writes(i).map { case (k, v) =>
      (asKey(p) + "." + k) -> v
    })

  implicit def writeSeq[I](p: Path)(implicit w: Writes[I, Seq[String]]): Writes[Seq[I], Map[String, Seq[String]]] =
    Writes{(is: Seq[I]) =>
      is.zipWithIndex.map { case (i, index) =>
        val prefix = asKey(p \ index)
        prefix -> w.writes(i)
      }.toMap
    }

  // implicit def writeJson[I](p: Path[JsValue])(implicit w: Writes[I, JsValue]): Writes[I, JsValue] = ???

  implicit def writeSeqToMap[I](p: Path)(implicit w: Writes[I, Map[String, Seq[String]]]): Writes[Seq[I], Map[String, Seq[String]]] =
    Writes{(is: Seq[I]) =>
      val ms = is.zipWithIndex.map { case (i, index) =>
        val prefix = asKey(p \ index)
        w.writes(i).map{ case (k, v) => (prefix + "." + k) -> v }
      }
      ms.foldLeft(Map.empty[String, Seq[String]])((acc, m) => acc ++ m)
    }
}

object Writes extends DefaultWrites with DefaultMonoids {

  def apply[I, O](f: I => O) = new Writes[I, O] {
    def writes(i: I) = f(i)
  }

  def apply[I, O](i: I)(implicit w: Writes[I, O]) =
    w.writes(i)

  import play.api.libs.functional._
  implicit def functionalCanBuildWrites[O](implicit m: Monoid[O]) = new FunctionalCanBuild[({type f[I] = Writes[I, O]})#f] {
    def apply[A, B](wa: Writes[A, O], wb: Writes[B, O]): Writes[A ~ B, O] = Writes[A ~ B, O] { (x: A ~ B) =>
      x match {
        case a ~ b => m.append(wa.writes(a), wb.writes(b))
      }
    }
  }

  implicit def contravariantfunctorOWrites[O] = new ContravariantFunctor[({type f[I] = Writes[I, O]})#f] {
    def contramap[A, B](wa: Writes[A, O], f: B => A): Writes[B, O] = Writes[B, O]( (b: B) => wa.writes(f(b)) )
  }

  import play.api.libs.functional.syntax._
  implicit def fbow[I, O](a: Writes[I, O])(implicit m: Monoid[O]) = toFunctionalBuilderOps[({type f[I] = Writes[I, O]})#f, I](a)

}
