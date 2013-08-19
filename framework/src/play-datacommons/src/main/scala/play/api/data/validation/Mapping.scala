package play.api.data.validation

// We don't want Mapping to be covariant on Validation[E, O] (type if the return function)
// Typically, we don't want Mapping[E, I, JsNumber] >: Mapping[E, I, JsValue]
@annotation.implicitNotFound("No implicit Mapping found from ${I} to ${O}. Try to define an implicit Mapping[${E}, ${I}, ${O}].")
trait Mapping[E, I, O] extends (I => Validation[E, O])
object Mapping {
	def apply[E, I, O](f: I => Validation[E, O]) = new Mapping[E, I, O] {
		override def apply(i: I) = f(i)
	}/*
	// PROBABLY USELESS
	import play.api.libs.functional._

	implicit def applicativeMapping[E, I] = new Applicative[({type λ[O] = Mapping[E, I, O]})#λ] {
    override def pure[A](a: A): Mapping[E, I, A] =
      Mapping(_ => Success(a))

    override def map[A, B](m: Mapping[E, I, A], f: A => B): Mapping[E, I, B] =
			Mapping(m(_).map(f))

    override def apply[A, B](mf: Mapping[E, I, A => B], ma: Mapping[E, I, A]): Mapping[E, I, B] =
    	Mapping{ d =>
    		val a = ma(d)
        val f = mf(d)
        (f *> a).flatMap(x => f.map(_(x)))
    	}
  }

  implicit def functorMapping[E, I] = new Functor[({type λ[O] = Mapping[E, I, O]})#λ] {
    def fmap[A, B](m: Mapping[E, I, A], f: A => B): Mapping[E, I, B] =
    	applicativeMapping[E, I].map(m, f)
  }

  // Helps the compiler a bit
  import play.api.libs.functional.syntax._
  implicit def cba[E, I] = functionalCanBuildApplicative[({type λ[O] = Mapping[E, I, O]})#λ]
  implicit def fbo[E, I, O] = toFunctionalBuilderOps[({type λ[O] = Mapping[E, I, O]})#λ, O] _
*/
}