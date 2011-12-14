package play.api.json

/**
 * Json formatter: write an implicit to define both a serializer and a deserializer for any type
 */
trait Format[T] extends Writes[T] with Reads[T]

object Format extends DefaultFormat

trait DefaultFormat {

  implicit def GenericFormat[T](implicit fjs: Reads[T], tjs: Writes[T]) = {
    new Format[T] {
      def reads(json: JsValue) = fjs.reads(json)
      def writes(o: T) = tjs.writes(o)
    }
  }

}
