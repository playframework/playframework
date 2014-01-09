package play.api.data.mapping

import scala.language.implicitConversions

trait WriteLike[I, +O] {
  /**
   * "Serialize" `i` to the output type
   */
  def writes(i: I): O
}

object WriteLike {
  implicit def zero[I]: WriteLike[I, I] = Write(identity[I] _)
}

trait Write[I, +O] extends WriteLike[I, O] {

  /**
   * returns a new Write that applies function `f` to the result of this write.
   * {{{
   *  val w = Writes.int.map("Number: " + _)
   *  w.writes(42) == "Number: 42"
   * }}}
   */
  def map[B](f: O => B) = Write[I, B] {
    f.compose(x => this.writes(x))
  }

  /**
   * Returns a new Write that applies `this` Write, and then applies `w` to its result
   */
  def compose[OO >: O, P](w: WriteLike[OO, P]) =
    this.map(o => w.writes(o))
}

trait DefaultMonoids {
  import play.api.libs.functional.Monoid

  implicit def mapMonoid = new Monoid[UrlFormEncoded] {
    def append(a1: UrlFormEncoded, a2: UrlFormEncoded) = a1 ++ a2
    def identity = Map.empty
  }
}

object Write {
  import scala.language.experimental.macros

  def apply[I, O](w: I => O): Write[I, O] = new Write[I, O] {
    def writes(i: I) = w(i)
  }

  def toWrite[I, O](r: WriteLike[I, O]) = new Write[I, O] {
    def writes(data: I): O = r.writes(data)
  }

  def gen[I, O]: Write[I, O] = macro MappingMacros.write[I, O]

  implicit def zero[I]: Write[I, I] = toWrite(WriteLike.zero[I])

  import play.api.libs.functional._
  implicit def functionalCanBuildWrite[O](implicit m: Monoid[O]) = new FunctionalCanBuild[({ type λ[I] = Write[I, O] })#λ] {
    def apply[A, B](wa: Write[A, O], wb: Write[B, O]): Write[A ~ B, O] = Write[A ~ B, O] { (x: A ~ B) =>
      x match {
        case a ~ b => m.append(wa.writes(a), wb.writes(b))
      }
    }
  }

  implicit def contravariantfunctorWrite[O] = new ContravariantFunctor[({ type λ[I] = Write[I, O] })#λ] {
    def contramap[A, B](wa: Write[A, O], f: B => A): Write[B, O] = Write[B, O]((b: B) => wa.writes(f(b)))
  }

  // XXX: Helps the compiler a bit
  import play.api.libs.functional.syntax._
  implicit def fboWrite[I, O: Monoid](a: Write[I, O]) = toFunctionalBuilderOps[({ type λ[I] = Write[I, O] })#λ, I](a)
  implicit def cfoWrite[I, O](a: Write[I, O]) = toContraFunctorOps[({ type λ[I] = Write[I, O] })#λ, I](a)

}