package play.api.libs.json
import scala.annotation.implicitNotFound

/**
 * Json formatter: write an implicit to define both a serializer and a deserializer for any type.
 */
@implicitNotFound(
  "No Json formatter found for type ${T}. Try to implement an implicit Format for this type."
)
trait Format[T] extends Writes[T] with Reads[T]

trait OFormat[T] extends OWrites[T] with Reads[T] with Format[T]

object OFormat {

  import play.api.libs.json.util._

  implicit def functionalCanBuildFormats(implicit rcb: FunctionalCanBuild[Reads], wcb: FunctionalCanBuild[OWrites]): FunctionalCanBuild[OFormat] = new FunctionalCanBuild[OFormat] {

    def apply[A,B](fa: OFormat[A], fb: OFormat[B]): OFormat[A~B] = 
      OFormat[A~B](
        rcb(fa, fb),
        wcb(fa, fb)
      )

  }

  implicit val invariantFunctorOFormat:InvariantFunctor[OFormat] = new InvariantFunctor[OFormat] {

    def inmap[A,B](fa:OFormat[A], f1:A => B, f2: B => A):OFormat[B] = OFormat[B]( (js: JsValue) => fa.reads(js).map(f1), (b: B) => fa.writes(f2(b)))

  }

  def apply[A](read: JsValue => JsResult[A], write: A => JsObject):OFormat[A] = new OFormat[A] {

    def reads(js:JsValue):JsResult[A] = read(js)

    def writes(a:A):JsObject = write(a)

  }

  def apply[A](r: Reads[A], w: OWrites[A]):OFormat[A] = new OFormat[A] {
    def reads(js:JsValue):JsResult[A] = r.reads(js)

    def writes(a:A):JsObject = w.writes(a)    
  }
}

/**
 * Default Json formatters.
 */
object Format extends DefaultFormat

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

  implicit def GenericOFormat[T](implicit fjs: Reads[T], tjs: OWrites[T]): Format[T] = {
    new OFormat[T] {
      def reads(json: JsValue) = fjs.reads(json)
      def writes(o: T) = tjs.writes(o)
    }
  }

}

