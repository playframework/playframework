package play.api.data.mapping

import scala.annotation.implicitNotFound
import scala.language.implicitConversions

@implicitNotFound("No Format found for types ${IR},${IW}, ${O}. Try to implement an implicit Format[${IR}, ${IW}, ${O}].")
trait Format[IR, IW, O] extends RuleLike[IR, O] with WriteLike[O, IW]

/**
 * Default formatters.
 */
object Format {

  def apply[IR, IW, O](r: RuleLike[IR, O], w: WriteLike[O, IW]): Format[IR, IW, O] = {
    new Format[IR, IW, O] {
      def validate(i: IR) = r.validate(i)
      def writes(o: O): IW = w.writes(o)
    }
  }

  import scala.language.experimental.macros
  def gen[IR, IW, O]: Format[IR, IW, O] = macro MappingMacros.format[IR, IW, O]

  import play.api.libs.functional._

  implicit def invariantFunctorFormat[IR, IW]: InvariantFunctor[({ type λ[O] = Format[IR, IW, O] })#λ] = new InvariantFunctor[({ type λ[O] = Format[IR, IW, O] })#λ] {
    def inmap[A, B](fa: Format[IR, IW, A], f1: A => B, f2: B => A): Format[IR, IW, B] =
      Format[IR, IW, B](Rule.toRule(fa).fmap(f1), Write.toWrite(fa).contramap(f2))
  }

  implicit def functionalCanBuildFormat[IR, IW: Monoid](implicit rcb: FunctionalCanBuild[({ type λ[O] = Rule[IR, O] })#λ], wcb: FunctionalCanBuild[({ type λ[O] = Write[O, IW] })#λ]): FunctionalCanBuild[({ type λ[O] = Format[IR, IW, O] })#λ] =
    new FunctionalCanBuild[({ type λ[O] = Format[IR, IW, O] })#λ] {
      def apply[A, B](fa: Format[IR, IW, A], fb: Format[IR, IW, B]): Format[IR, IW, A ~ B] =
        Format[IR, IW, A ~ B](rcb(Rule.toRule(fa), Rule.toRule(fb)), wcb(Write.toWrite(fa), Write.toWrite(fb)))
    }

  // XXX: Helps the compiler a bit
  import play.api.libs.functional.syntax._
  implicit def fboFormat[IR, IW: Monoid, O](f: Format[IR, IW, O])(implicit fcb: FunctionalCanBuild[({ type λ[O] = Format[IR, IW, O] })#λ]) =
    toFunctionalBuilderOps[({ type λ[O] = Format[IR, IW, O] })#λ, O](f)(fcb)

}