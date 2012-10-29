package play.utils

/**
 * provides conversion helpers
 */
object Conversions {

  def newMap[A, B](data: (A, B)*) = Map(data: _*)

}