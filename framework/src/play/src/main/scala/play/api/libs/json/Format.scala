package play.api.libs.json
import scala.annotation.implicitNotFound

/**
 * Json formatter: write an implicit to define both a serializer and a deserializer for any type.
 */
@implicitNotFound(
  "No Json formatter found for type ${T}. Try to implement an implicit Format for this type."
)
trait Format[T] extends Writes[T] with Reads[T]

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

}
