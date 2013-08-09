package play.api.libs.json
import scala.annotation.implicitNotFound

/**
 * Json formatter: write an implicit to define both a serializer and a deserializer for any type.
 */
@implicitNotFound(
  "No Json formatter found for type ${A}. Try to implement an implicit Format for this type."
)
trait Format[A] extends Writes[A] with Reads[A]
trait OFormat[A] extends OWrites[A] with Reads[A] with Format[A]

object OFormat {

  import play.api.libs.functional._

  implicit def functionalCanBuildFormats(implicit rcb: FunctionalCanBuild[Reads], wcb: FunctionalCanBuild[OWrites]): FunctionalCanBuild[OFormat] = new FunctionalCanBuild[OFormat] {

    def apply[A, B](fa: OFormat[A], fb: OFormat[B]): OFormat[A ~ B] =
      OFormat[A ~ B](
        rcb(fa, fb),
        wcb(fa, fb)
      )

  }

  implicit val invariantFunctorOFormat: InvariantFunctor[OFormat] = new InvariantFunctor[OFormat] {

    def inmap[A, B](fa: OFormat[A], f1: A => B, f2: B => A): OFormat[B] = OFormat[B]((js: JsValue) => fa.reads(js).map(f1), (b: B) => fa.writes(f2(b)))

  }

  implicit def GenericOFormat[T](implicit fjs: Reads[T], tjs: OWrites[T]): Format[T] = apply(fjs, tjs)

  def apply[A](read: JsValue => JsResult[A], write: A => JsObject): OFormat[A] = new OFormat[A] {

    def reads(js: JsValue): JsResult[A] = read(js)

    def writes(a: A): JsObject = write(a)

  }

  def apply[A](r: Reads[A], w: OWrites[A]): OFormat[A] = new OFormat[A] {
    def reads(js: JsValue): JsResult[A] = r.reads(js)

    def writes(a: A): JsObject = w.writes(a)
  }
}

/**
 * Default Json formatters.
 */
object Format extends PathFormat with ConstraintFormat with DefaultFormat {

  val constraints: ConstraintFormat = this
  val path: PathFormat = this

  def apply[A](fjs: Reads[A], tjs: Writes[A]): Format[A] = {
    new Format[A] {
      def reads(json: JsValue) = fjs.reads(json)
      def writes(o: A) = tjs.writes(o)
    }
  }

}

/**
 * Default Json formatters.
 */
trait DefaultFormat {

  implicit def GenericFormat[T](implicit fjs: Reads[T], tjs: Writes[T]): Format[T] = {
    new Format[T] {
      def reads(json: JsValue) = fjs.reads(json)
      def writes(o: T) = tjs.writes(o)
    }
  }

}

