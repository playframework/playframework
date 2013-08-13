package play.api.data.validation

// We don't want Mapping to be covariant on Validation[E, O] (type if the return function)
// Typically, we don't want Mapping[E, I, JsNumber] >: Mapping[E, I, JsValue]
@annotation.implicitNotFound("No implicit Mapping found from ${I} to ${O}. Try to define an implicit Mapping[${E}, ${I}, ${O}].")
trait Mapping[E, I, O] extends (I => Validation[E, O])
object Mapping {
	def apply[E, I, O](f: I => Validation[E, O]) = new Mapping[E, I, O] {
		override def apply(i: I) = f(i)
	}
}