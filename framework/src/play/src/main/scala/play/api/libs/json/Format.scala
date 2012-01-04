package play.api.libs.json

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

  /**
   * MapFormat[T] creates a Format[Map[String, T]] simply copying mapReads[T].reads and mapWrites[T].writes
   * because Scala compiles the following code but then can't resolve implicit Format[Map[String, T]]
   * {{{ 
   *  implicit def MapFormat[T](implicit fjs: Reads[Map[String,T], tjs: Writes[Map[String,T]): Format[collection.immutable.Map[String,T]] = {
   *    new Format[collection.immutable.Map[String,T]] {
   *  ...
   *  }
   * }}} 
   *
   * Certainly a problem of erasure or something like that???   
   */
  implicit def MapFormat[T](implicit fjs: Reads[T], tjs: Writes[T]): Format[collection.immutable.Map[String,T]] = {
    new Format[collection.immutable.Map[String,T]] {
      def reads(json: JsValue) = json match {
        case JsObject(m) => m.map { case (k, v) => (k -> fromJson[T](v)(fjs)) }.toMap
        case _ => throw new RuntimeException("Map expected")
      }
      def writes(o: collection.immutable.Map[String,T]) = JsObject(o.map { case (k, v) => (k, toJson(v)(tjs)) }.toList)
    }
  }
}
