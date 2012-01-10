package play.utils

object Conversions {
  
  def newMap[A,B](data: (A, B)*) = Map(data:_*)
  
}