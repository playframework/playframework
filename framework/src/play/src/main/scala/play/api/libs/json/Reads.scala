package play.api.libs.json

import scala.collection._

/**
 * Json deserializer: write an implicit to define a deserializer for any type
 */
trait Reads[T] {
  /**
   * Convert the JsValue into a T
   */
  def reads(json: JsValue): T
}

object Reads extends DefaultReads

trait DefaultReads {

  implicit object IntReads extends Reads[Int] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n.toInt
      case _ => throw new RuntimeException("Int expected")
    }
  }

  implicit object ShortReads extends Reads[Short] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n.toShort
      case _ => throw new RuntimeException("Short expected")
    }
  }

  implicit object LongReads extends Reads[Long] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n.toLong
      case _ => throw new RuntimeException("Long expected")
    }
  }

  implicit object FloatReads extends Reads[Float] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n.toFloat
      case _ => throw new RuntimeException("Float expected")
    }
  }

  implicit object DoubleReads extends Reads[Double] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n.toDouble
      case _ => throw new RuntimeException("Double expected")
    }
  }

  implicit object BooleanReads extends Reads[Boolean] {
    def reads(json: JsValue) = json match {
      case JsBoolean(b) => b
      case _ => throw new RuntimeException("Boolean expected")
    }
  }

  implicit object StringReads extends Reads[String] {
    def reads(json: JsValue) = json match {
      case JsString(s) => s
      case _ => throw new RuntimeException("String expected")
    }
  }

  implicit def listReads[T](implicit fmt: Reads[T]): Reads[List[T]] = new Reads[List[T]] {
    def reads(json: JsValue) = json match {
      case JsArray(ts) => ts.map(t => fromJson(t)(fmt))
      case _ => throw new RuntimeException("List expected")
    }
  }

  implicit def seqReads[T](implicit fmt: Reads[T]): Reads[Seq[T]] = new Reads[Seq[T]] {
    def reads(json: JsValue) = json match {
      case JsArray(ts) => ts.map(t => fromJson(t)(fmt))
      case _ => throw new RuntimeException("Seq expected")
    }
  }

  implicit def arrayReads[T](implicit fmt: Reads[T], mf: Manifest[T]): Reads[Array[T]] = new Reads[Array[T]] {
    def reads(json: JsValue) = json match {
      case JsArray(ts) => listToArray(ts.map(t => fromJson(t)(fmt)))
      case _ => throw new RuntimeException("Array expected")
    }
  }
  private[this] def listToArray[T: Manifest](ls: List[T]): Array[T] = ls.toArray

  implicit def mapReads[V](implicit fmtv: Reads[V]): Reads[collection.immutable.Map[String, V]] = new Reads[collection.immutable.Map[String, V]] {
    def reads(json: JsValue) = json match {
      case JsObject(m) => m.map { case (k, v) => (k -> fromJson[V](v)(fmtv)) }.toMap
      case _ => throw new RuntimeException("Map expected")
    }
  }

  implicit def mutableSetReads[T](implicit fmt: Reads[T]): Reads[mutable.Set[T]] =
    viaSeq((x: Seq[T]) => mutable.Set(x: _*))

  implicit def immutableSetReads[T](implicit fmt: Reads[T]): Reads[immutable.Set[T]] =
    viaSeq((x: Seq[T]) => immutable.Set(x: _*))

  implicit def immutableSortedSetReads[S](implicit ord: S => Ordered[S], binS: Reads[S]): Reads[immutable.SortedSet[S]] = {
    viaSeq((x: Seq[S]) => immutable.TreeSet[S](x: _*))
  }

  def viaSeq[S <: Iterable[T], T](f: Seq[T] => S)(implicit fmt: Reads[T]): Reads[S] = new Reads[S] {
    def reads(json: JsValue) = json match {
      case JsArray(ts) => f(ts.map(t => fromJson[T](t)))
      case _ => throw new RuntimeException("Collection expected")
    }
  }

  implicit object JsValueReads extends Reads[JsValue] {
    def writes(o: JsValue) = o
    def reads(json: JsValue) = json
  }

  implicit def tuple2Reads[T1, T2](implicit fmt1: Reads[T1], fmt2: Reads[T2], sz: Int = 2): Reads[Tuple2[T1, T2]] = new Reads[Tuple2[T1, T2]]{
    def reads(json: JsValue) = json match {
      case JsObject(m) if(m.size >= sz) => {
        val idxm = m.view(0, sz).toIndexedSeq
        (
          fromJson(idxm(0).value)(fmt1),
          fromJson(idxm(1).value)(fmt2)
        )
      }  
      case _ => throw new RuntimeException("Map with size>="+sz+" expected")
    }
  }

  implicit def tuple3Reads[T1, T2, T3](implicit fmt1: Reads[T1], fmt2: Reads[T2], fmt3: Reads[T3], sz: Int = 3): Reads[Tuple3[T1, T2, T3]] = new Reads[Tuple3[T1, T2, T3]]{
    def reads(json: JsValue) = json match {
      case JsObject(m) if(m.size >= sz) => {
        val idxm = m.view(0, sz).toIndexedSeq
        (
          fromJson(idxm(0).value)(fmt1),
          fromJson(idxm(1).value)(fmt2),
          fromJson(idxm(2).value)(fmt3)
        )
      }  
      case _ => throw new RuntimeException("Map with size>="+sz+" expected")
    }
  }

  implicit def tuple4Reads[T1, T2, T3, T4](implicit fmt1: Reads[T1], fmt2: Reads[T2], fmt3: Reads[T3], fmt4: Reads[T4], sz: Int = 4): Reads[Tuple4[T1, T2, T3, T4]] = new Reads[Tuple4[T1, T2, T3, T4]]{
    def reads(json: JsValue) = json match {
      case JsObject(m) if(m.size >= sz) => {
        val idxm = m.view(0, sz).toIndexedSeq
        (
          fromJson(idxm(0).value)(fmt1),
          fromJson(idxm(1).value)(fmt2),
          fromJson(idxm(2).value)(fmt3),
          fromJson(idxm(3).value)(fmt4)
        )
      }  
      case _ => throw new RuntimeException("Map with size>="+sz+" expected")
    }
  }

  implicit def tuple5Reads[T1, T2, T3, T4, T5](implicit fmt1: Reads[T1], fmt2: Reads[T2], fmt3: Reads[T3], fmt4: Reads[T4], fmt5: Reads[T5], sz: Int = 5): Reads[Tuple5[T1, T2, T3, T4, T5]] = new Reads[Tuple5[T1, T2, T3, T4, T5]]{
    def reads(json: JsValue) = json match {
      case JsObject(m) if(m.size >= sz) => {
        val idxm = m.view(0, sz).toIndexedSeq
        (
          fromJson(idxm(0).value)(fmt1),
          fromJson(idxm(1).value)(fmt2),
          fromJson(idxm(2).value)(fmt3),
          fromJson(idxm(3).value)(fmt4),
          fromJson(idxm(4).value)(fmt5)
        )
      }  
      case _ => throw new RuntimeException("Map with size>="+sz+" expected")
    }
  }

  implicit def tuple6Reads[T1, T2, T3, T4, T5, T6](implicit fmt1: Reads[T1], fmt2: Reads[T2], fmt3: Reads[T3], fmt4: Reads[T4], fmt5: Reads[T5], fmt6: Reads[T6], sz: Int = 6): Reads[Tuple6[T1, T2, T3, T4, T5, T6]] = new Reads[Tuple6[T1, T2, T3, T4, T5, T6]]{
    def reads(json: JsValue) = json match {
      case JsObject(m) if(m.size >= sz) => {
        val idxm = m.view(0, sz).toIndexedSeq
        (
          fromJson(idxm(0).value)(fmt1),
          fromJson(idxm(1).value)(fmt2),
          fromJson(idxm(2).value)(fmt3),
          fromJson(idxm(3).value)(fmt4),
          fromJson(idxm(4).value)(fmt5),
          fromJson(idxm(5).value)(fmt6)
        )
      }  
      case _ => throw new RuntimeException("Map with size>="+sz+" expected")
    }
  }

  implicit def tuple7Reads[T1, T2, T3, T4, T5, T6, T7](implicit fmt1: Reads[T1], fmt2: Reads[T2], fmt3: Reads[T3], fmt4: Reads[T4], fmt5: Reads[T5], fmt6: Reads[T6], fmt7: Reads[T7], sz: Int = 7): Reads[Tuple7[T1, T2, T3, T4, T5, T6, T7]] = new Reads[Tuple7[T1, T2, T3, T4, T5, T6, T7]]{
    def reads(json: JsValue) = json match {
      case JsObject(m) if(m.size >= sz) => {
        val idxm = m.view(0, sz).toIndexedSeq
        (
          fromJson(idxm(0).value)(fmt1),
          fromJson(idxm(1).value)(fmt2),
          fromJson(idxm(2).value)(fmt3),
          fromJson(idxm(3).value)(fmt4),
          fromJson(idxm(4).value)(fmt5),
          fromJson(idxm(5).value)(fmt6),
          fromJson(idxm(6).value)(fmt7)
        )
      }  
      case _ => throw new RuntimeException("Map with size>="+sz+" expected")
    }
  }

  implicit def tuple8Reads[T1, T2, T3, T4, T5, T6, T7, T8](implicit fmt1: Reads[T1], fmt2: Reads[T2], fmt3: Reads[T3], fmt4: Reads[T4], fmt5: Reads[T5], fmt6: Reads[T6], fmt7: Reads[T7], fmt8: Reads[T8], sz: Int = 8): Reads[Tuple8[T1, T2, T3, T4, T5, T6, T7, T8]] = new Reads[Tuple8[T1, T2, T3, T4, T5, T6, T7, T8]]{
    def reads(json: JsValue) = json match {
      case JsObject(m) if(m.size >= sz) => {
        val idxm = m.view(0, sz).toIndexedSeq
        (
          fromJson(idxm(0).value)(fmt1),
          fromJson(idxm(1).value)(fmt2),
          fromJson(idxm(2).value)(fmt3),
          fromJson(idxm(3).value)(fmt4),
          fromJson(idxm(4).value)(fmt5),
          fromJson(idxm(5).value)(fmt6),
          fromJson(idxm(6).value)(fmt7),
          fromJson(idxm(7).value)(fmt8)
        )
      }  
      case _ => throw new RuntimeException("Map with size>="+sz+" expected")
    }
  }

  implicit def tuple9Reads[T1, T2, T3, T4, T5, T6, T7, T8, T9](implicit fmt1: Reads[T1], fmt2: Reads[T2], fmt3: Reads[T3], fmt4: Reads[T4], fmt5: Reads[T5], fmt6: Reads[T6], fmt7: Reads[T7], fmt8: Reads[T8], fmt9: Reads[T9], sz: Int = 9): Reads[Tuple9[T1, T2, T3, T4, T5, T6, T7, T8, T9]] = new Reads[Tuple9[T1, T2, T3, T4, T5, T6, T7, T8, T9]]{
    def reads(json: JsValue) = json match {
      case JsObject(m) if(m.size >= sz) => {
        val idxm = m.view(0, sz).toIndexedSeq
        (
          fromJson(idxm(0).value)(fmt1),
          fromJson(idxm(1).value)(fmt2),
          fromJson(idxm(2).value)(fmt3),
          fromJson(idxm(3).value)(fmt4),
          fromJson(idxm(4).value)(fmt5),
          fromJson(idxm(5).value)(fmt6),
          fromJson(idxm(6).value)(fmt7),
          fromJson(idxm(7).value)(fmt8),
          fromJson(idxm(8).value)(fmt9)
        )
      }  
      case _ => throw new RuntimeException("Map with size>="+sz+" expected")
    }
  }

  implicit def tuple10Reads[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10](implicit fmt1: Reads[T1], fmt2: Reads[T2], fmt3: Reads[T3], fmt4: Reads[T4], fmt5: Reads[T5], fmt6: Reads[T6], fmt7: Reads[T7], fmt8: Reads[T8], fmt9: Reads[T9], fmt10: Reads[T10], sz: Int = 10): Reads[Tuple10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10]] = new Reads[Tuple10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10]]{
    def reads(json: JsValue) = json match {
      case JsObject(m) if(m.size >= sz) => {
        val idxm = m.view(0, sz).toIndexedSeq
        (
          fromJson(idxm(0).value)(fmt1),
          fromJson(idxm(1).value)(fmt2),
          fromJson(idxm(2).value)(fmt3),
          fromJson(idxm(3).value)(fmt4),
          fromJson(idxm(4).value)(fmt5),
          fromJson(idxm(5).value)(fmt6),
          fromJson(idxm(6).value)(fmt7),
          fromJson(idxm(7).value)(fmt8),
          fromJson(idxm(8).value)(fmt9),
          fromJson(idxm(9).value)(fmt10)
        )
      }  
      case _ => throw new RuntimeException("Map with size>="+sz+" expected")
    }
  }

  implicit def tuple11Reads[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11](implicit fmt1: Reads[T1], fmt2: Reads[T2], fmt3: Reads[T3], fmt4: Reads[T4], fmt5: Reads[T5], fmt6: Reads[T6], fmt7: Reads[T7], fmt8: Reads[T8], fmt9: Reads[T9], fmt10: Reads[T10], fmt11: Reads[T11], sz: Int = 11): Reads[Tuple11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11]] = new Reads[Tuple11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11]]{
    def reads(json: JsValue) = json match {
      case JsObject(m) if(m.size >= sz) => {
        val idxm = m.view(0, sz).toIndexedSeq
        (
          fromJson(idxm(0).value)(fmt1),
          fromJson(idxm(1).value)(fmt2),
          fromJson(idxm(2).value)(fmt3),
          fromJson(idxm(3).value)(fmt4),
          fromJson(idxm(4).value)(fmt5),
          fromJson(idxm(5).value)(fmt6),
          fromJson(idxm(6).value)(fmt7),
          fromJson(idxm(7).value)(fmt8),
          fromJson(idxm(8).value)(fmt9),
          fromJson(idxm(9).value)(fmt10),
          fromJson(idxm(10).value)(fmt11)
        )
      }  
      case _ => throw new RuntimeException("Map with size>="+sz+" expected")
    }
  }

  implicit def tuple12Reads[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12](implicit fmt1: Reads[T1], fmt2: Reads[T2], fmt3: Reads[T3], fmt4: Reads[T4], fmt5: Reads[T5], fmt6: Reads[T6], fmt7: Reads[T7], fmt8: Reads[T8], fmt9: Reads[T9], fmt10: Reads[T10], fmt11: Reads[T11], fmt12: Reads[T12], sz: Int = 12): Reads[Tuple12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12]] = new Reads[Tuple12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12]]{
    def reads(json: JsValue) = json match {
      case JsObject(m) if(m.size >= sz) => {
        val idxm = m.view(0, sz).toIndexedSeq
        (
          fromJson(idxm(0).value)(fmt1),
          fromJson(idxm(1).value)(fmt2),
          fromJson(idxm(2).value)(fmt3),
          fromJson(idxm(3).value)(fmt4),
          fromJson(idxm(4).value)(fmt5),
          fromJson(idxm(5).value)(fmt6),
          fromJson(idxm(6).value)(fmt7),
          fromJson(idxm(7).value)(fmt8),
          fromJson(idxm(8).value)(fmt9),
          fromJson(idxm(9).value)(fmt10),
          fromJson(idxm(10).value)(fmt11),
          fromJson(idxm(11).value)(fmt12)          
        )
      }  
      case _ => throw new RuntimeException("Map with size>="+sz+" expected")
    }
  }

  implicit def tuple13Reads[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13](implicit fmt1: Reads[T1], fmt2: Reads[T2], fmt3: Reads[T3], fmt4: Reads[T4], fmt5: Reads[T5], fmt6: Reads[T6], fmt7: Reads[T7], fmt8: Reads[T8], fmt9: Reads[T9], fmt10: Reads[T10], fmt11: Reads[T11], fmt12: Reads[T12], fmt13: Reads[T13], sz: Int = 13): Reads[Tuple13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13]] = new Reads[Tuple13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13]]{
    def reads(json: JsValue) = json match {
      case JsObject(m) if(m.size >= sz) => {
        val idxm = m.view(0, sz).toIndexedSeq
        (
          fromJson(idxm(0).value)(fmt1),
          fromJson(idxm(1).value)(fmt2),
          fromJson(idxm(2).value)(fmt3),
          fromJson(idxm(3).value)(fmt4),
          fromJson(idxm(4).value)(fmt5),
          fromJson(idxm(5).value)(fmt6),
          fromJson(idxm(6).value)(fmt7),
          fromJson(idxm(7).value)(fmt8),
          fromJson(idxm(8).value)(fmt9),
          fromJson(idxm(9).value)(fmt10),
          fromJson(idxm(10).value)(fmt11),
          fromJson(idxm(11).value)(fmt12),
          fromJson(idxm(12).value)(fmt13)
        )
      }  
      case _ => throw new RuntimeException("Map with size>="+sz+" expected")
    }
  }

  implicit def tuple14Reads[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14](implicit fmt1: Reads[T1], fmt2: Reads[T2], fmt3: Reads[T3], fmt4: Reads[T4], fmt5: Reads[T5], fmt6: Reads[T6], fmt7: Reads[T7], fmt8: Reads[T8], fmt9: Reads[T9], fmt10: Reads[T10], fmt11: Reads[T11], fmt12: Reads[T12], fmt13: Reads[T13], fmt14: Reads[T14], sz: Int = 14): Reads[Tuple14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14]] = new Reads[Tuple14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14]]{
    def reads(json: JsValue) = json match {
      case JsObject(m) if(m.size >= sz) => {
        val idxm = m.view(0, sz).toIndexedSeq
        (
          fromJson(idxm(0).value)(fmt1),
          fromJson(idxm(1).value)(fmt2),
          fromJson(idxm(2).value)(fmt3),
          fromJson(idxm(3).value)(fmt4),
          fromJson(idxm(4).value)(fmt5),
          fromJson(idxm(5).value)(fmt6),
          fromJson(idxm(6).value)(fmt7),
          fromJson(idxm(7).value)(fmt8),
          fromJson(idxm(8).value)(fmt9),
          fromJson(idxm(9).value)(fmt10),
          fromJson(idxm(10).value)(fmt11),
          fromJson(idxm(11).value)(fmt12),
          fromJson(idxm(12).value)(fmt13),
          fromJson(idxm(13).value)(fmt14)
        )
      }  
      case _ => throw new RuntimeException("Map with size>="+sz+" expected")
    }
  }

  implicit def tuple15Reads[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15](implicit fmt1: Reads[T1], fmt2: Reads[T2], fmt3: Reads[T3], fmt4: Reads[T4], fmt5: Reads[T5], fmt6: Reads[T6], fmt7: Reads[T7], fmt8: Reads[T8], fmt9: Reads[T9], fmt10: Reads[T10], fmt11: Reads[T11], fmt12: Reads[T12], fmt13: Reads[T13], fmt14: Reads[T14], fmt15: Reads[T15], sz: Int = 15): Reads[Tuple15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15]] = new Reads[Tuple15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15]]{
    def reads(json: JsValue) = json match {
      case JsObject(m) if(m.size >= sz) => {
        val idxm = m.view(0, sz).toIndexedSeq
        (
          fromJson(idxm(0).value)(fmt1),
          fromJson(idxm(1).value)(fmt2),
          fromJson(idxm(2).value)(fmt3),
          fromJson(idxm(3).value)(fmt4),
          fromJson(idxm(4).value)(fmt5),
          fromJson(idxm(5).value)(fmt6),
          fromJson(idxm(6).value)(fmt7),
          fromJson(idxm(7).value)(fmt8),
          fromJson(idxm(8).value)(fmt9),
          fromJson(idxm(9).value)(fmt10),
          fromJson(idxm(10).value)(fmt11),
          fromJson(idxm(11).value)(fmt12),
          fromJson(idxm(12).value)(fmt13),
          fromJson(idxm(13).value)(fmt14),
          fromJson(idxm(14).value)(fmt15)
        )
      }  
      case _ => throw new RuntimeException("Map with size>="+sz+" expected")
    }
  }

  implicit def tuple16Reads[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16](implicit fmt1: Reads[T1], fmt2: Reads[T2], fmt3: Reads[T3], fmt4: Reads[T4], fmt5: Reads[T5], fmt6: Reads[T6], fmt7: Reads[T7], fmt8: Reads[T8], fmt9: Reads[T9], fmt10: Reads[T10], fmt11: Reads[T11], fmt12: Reads[T12], fmt13: Reads[T13], fmt14: Reads[T14], fmt15: Reads[T15], fmt16: Reads[T16], sz: Int = 16): Reads[Tuple16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16]] = new Reads[Tuple16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16]]{
    def reads(json: JsValue) = json match {
      case JsObject(m) if(m.size >= sz) => {
        val idxm = m.view(0, sz).toIndexedSeq
        (
          fromJson(idxm(0).value)(fmt1),
          fromJson(idxm(1).value)(fmt2),
          fromJson(idxm(2).value)(fmt3),
          fromJson(idxm(3).value)(fmt4),
          fromJson(idxm(4).value)(fmt5),
          fromJson(idxm(5).value)(fmt6),
          fromJson(idxm(6).value)(fmt7),
          fromJson(idxm(7).value)(fmt8),
          fromJson(idxm(8).value)(fmt9),
          fromJson(idxm(9).value)(fmt10),
          fromJson(idxm(10).value)(fmt11),
          fromJson(idxm(11).value)(fmt12),
          fromJson(idxm(12).value)(fmt13),
          fromJson(idxm(13).value)(fmt14),
          fromJson(idxm(14).value)(fmt15),
          fromJson(idxm(15).value)(fmt16)
        )
      }  
      case _ => throw new RuntimeException("Map with size>="+sz+" expected")
    }
  }

  implicit def tuple17Reads[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17](implicit fmt1: Reads[T1], fmt2: Reads[T2], fmt3: Reads[T3], fmt4: Reads[T4], fmt5: Reads[T5], fmt6: Reads[T6], fmt7: Reads[T7], fmt8: Reads[T8], fmt9: Reads[T9], fmt10: Reads[T10], fmt11: Reads[T11], fmt12: Reads[T12], fmt13: Reads[T13], fmt14: Reads[T14], fmt15: Reads[T15], fmt16: Reads[T16], fmt17: Reads[T17], sz: Int = 17): Reads[Tuple17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17]] = new Reads[Tuple17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17]]{
    def reads(json: JsValue) = json match {
      case JsObject(m) if(m.size >= sz) => {
        val idxm = m.view(0, sz).toIndexedSeq
        (
          fromJson(idxm(0).value)(fmt1),
          fromJson(idxm(1).value)(fmt2),
          fromJson(idxm(2).value)(fmt3),
          fromJson(idxm(3).value)(fmt4),
          fromJson(idxm(4).value)(fmt5),
          fromJson(idxm(5).value)(fmt6),
          fromJson(idxm(6).value)(fmt7),
          fromJson(idxm(7).value)(fmt8),
          fromJson(idxm(8).value)(fmt9),
          fromJson(idxm(9).value)(fmt10),
          fromJson(idxm(10).value)(fmt11),
          fromJson(idxm(11).value)(fmt12),
          fromJson(idxm(12).value)(fmt13),
          fromJson(idxm(13).value)(fmt14),
          fromJson(idxm(14).value)(fmt15),
          fromJson(idxm(15).value)(fmt16),
          fromJson(idxm(16).value)(fmt17)
        )
      }  
      case _ => throw new RuntimeException("Map with size>="+sz+" expected")
    }
  }

  implicit def tuple18Reads[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18](implicit fmt1: Reads[T1], fmt2: Reads[T2], fmt3: Reads[T3], fmt4: Reads[T4], fmt5: Reads[T5], fmt6: Reads[T6], fmt7: Reads[T7], fmt8: Reads[T8], fmt9: Reads[T9], fmt10: Reads[T10], fmt11: Reads[T11], fmt12: Reads[T12], fmt13: Reads[T13], fmt14: Reads[T14], fmt15: Reads[T15], fmt16: Reads[T16], fmt17: Reads[T17], fmt18: Reads[T18], sz: Int = 18): Reads[Tuple18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18]] = new Reads[Tuple18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18]]{
    def reads(json: JsValue) = json match {
      case JsObject(m) if(m.size >= sz) => {
        val idxm = m.view(0, sz).toIndexedSeq
        (
          fromJson(idxm(0).value)(fmt1),
          fromJson(idxm(1).value)(fmt2),
          fromJson(idxm(2).value)(fmt3),
          fromJson(idxm(3).value)(fmt4),
          fromJson(idxm(4).value)(fmt5),
          fromJson(idxm(5).value)(fmt6),
          fromJson(idxm(6).value)(fmt7),
          fromJson(idxm(7).value)(fmt8),
          fromJson(idxm(8).value)(fmt9),
          fromJson(idxm(9).value)(fmt10),
          fromJson(idxm(10).value)(fmt11),
          fromJson(idxm(11).value)(fmt12),
          fromJson(idxm(12).value)(fmt13),
          fromJson(idxm(13).value)(fmt14),
          fromJson(idxm(14).value)(fmt15),
          fromJson(idxm(15).value)(fmt16),
          fromJson(idxm(16).value)(fmt17),
          fromJson(idxm(17).value)(fmt18)
        )
      }  
      case _ => throw new RuntimeException("Map with size>="+sz+" expected")
    }
  }

  implicit def tuple19Reads[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19](implicit fmt1: Reads[T1], fmt2: Reads[T2], fmt3: Reads[T3], fmt4: Reads[T4], fmt5: Reads[T5], fmt6: Reads[T6], fmt7: Reads[T7], fmt8: Reads[T8], fmt9: Reads[T9], fmt10: Reads[T10], fmt11: Reads[T11], fmt12: Reads[T12], fmt13: Reads[T13], fmt14: Reads[T14], fmt15: Reads[T15], fmt16: Reads[T16], fmt17: Reads[T17], fmt18: Reads[T18], fmt19: Reads[T19], sz: Int = 19): Reads[Tuple19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19]] = new Reads[Tuple19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19]]{
    def reads(json: JsValue) = json match {
      case JsObject(m) if(m.size >= sz) => {
        val idxm = m.view(0, sz).toIndexedSeq
        (
          fromJson(idxm(0).value)(fmt1),
          fromJson(idxm(1).value)(fmt2),
          fromJson(idxm(2).value)(fmt3),
          fromJson(idxm(3).value)(fmt4),
          fromJson(idxm(4).value)(fmt5),
          fromJson(idxm(5).value)(fmt6),
          fromJson(idxm(6).value)(fmt7),
          fromJson(idxm(7).value)(fmt8),
          fromJson(idxm(8).value)(fmt9),
          fromJson(idxm(9).value)(fmt10),
          fromJson(idxm(10).value)(fmt11),
          fromJson(idxm(11).value)(fmt12),
          fromJson(idxm(12).value)(fmt13),
          fromJson(idxm(13).value)(fmt14),
          fromJson(idxm(14).value)(fmt15),
          fromJson(idxm(15).value)(fmt16),
          fromJson(idxm(16).value)(fmt17),
          fromJson(idxm(17).value)(fmt18),
          fromJson(idxm(18).value)(fmt19)
        )
      }  
      case _ => throw new RuntimeException("Map with size>="+sz+" expected")
    }
  }

  implicit def tuple20Reads[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20](implicit fmt1: Reads[T1], fmt2: Reads[T2], fmt3: Reads[T3], fmt4: Reads[T4], fmt5: Reads[T5], fmt6: Reads[T6], fmt7: Reads[T7], fmt8: Reads[T8], fmt9: Reads[T9], fmt10: Reads[T10], fmt11: Reads[T11], fmt12: Reads[T12], fmt13: Reads[T13], fmt14: Reads[T14], fmt15: Reads[T15], fmt16: Reads[T16], fmt17: Reads[T17], fmt18: Reads[T18], fmt19: Reads[T19], fmt20: Reads[T20], sz: Int = 20): Reads[Tuple20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20]] = new Reads[Tuple20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20]]{
    def reads(json: JsValue) = json match {
      case JsObject(m) if(m.size >= sz) => {
        val idxm = m.view(0, sz).toIndexedSeq
        (
          fromJson(idxm(0).value)(fmt1),
          fromJson(idxm(1).value)(fmt2),
          fromJson(idxm(2).value)(fmt3),
          fromJson(idxm(3).value)(fmt4),
          fromJson(idxm(4).value)(fmt5),
          fromJson(idxm(5).value)(fmt6),
          fromJson(idxm(6).value)(fmt7),
          fromJson(idxm(7).value)(fmt8),
          fromJson(idxm(8).value)(fmt9),
          fromJson(idxm(9).value)(fmt10),
          fromJson(idxm(10).value)(fmt11),
          fromJson(idxm(11).value)(fmt12),
          fromJson(idxm(12).value)(fmt13),
          fromJson(idxm(13).value)(fmt14),
          fromJson(idxm(14).value)(fmt15),
          fromJson(idxm(15).value)(fmt16),
          fromJson(idxm(16).value)(fmt17),
          fromJson(idxm(17).value)(fmt18),
          fromJson(idxm(18).value)(fmt19),
          fromJson(idxm(19).value)(fmt20)
        )
      }  
      case _ => throw new RuntimeException("Map with size>="+sz+" expected")
    }
  }  

  implicit def tuple21Reads[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21](implicit fmt1: Reads[T1], fmt2: Reads[T2], fmt3: Reads[T3], fmt4: Reads[T4], fmt5: Reads[T5], fmt6: Reads[T6], fmt7: Reads[T7], fmt8: Reads[T8], fmt9: Reads[T9], fmt10: Reads[T10], fmt11: Reads[T11], fmt12: Reads[T12], fmt13: Reads[T13], fmt14: Reads[T14], fmt15: Reads[T15], fmt16: Reads[T16], fmt17: Reads[T17], fmt18: Reads[T18], fmt19: Reads[T19], fmt20: Reads[T20], fmt21: Reads[T21], sz: Int = 21): Reads[Tuple21[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21]] = new Reads[Tuple21[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21]]{
    def reads(json: JsValue) = json match {
      case JsObject(m) if(m.size >= sz) => {
        val idxm = m.view(0, sz).toIndexedSeq
        (
          fromJson(idxm(0).value)(fmt1),
          fromJson(idxm(1).value)(fmt2),
          fromJson(idxm(2).value)(fmt3),
          fromJson(idxm(3).value)(fmt4),
          fromJson(idxm(4).value)(fmt5),
          fromJson(idxm(5).value)(fmt6),
          fromJson(idxm(6).value)(fmt7),
          fromJson(idxm(7).value)(fmt8),
          fromJson(idxm(8).value)(fmt9),
          fromJson(idxm(9).value)(fmt10),
          fromJson(idxm(10).value)(fmt11),
          fromJson(idxm(11).value)(fmt12),
          fromJson(idxm(12).value)(fmt13),
          fromJson(idxm(13).value)(fmt14),
          fromJson(idxm(14).value)(fmt15),
          fromJson(idxm(15).value)(fmt16),
          fromJson(idxm(16).value)(fmt17),
          fromJson(idxm(17).value)(fmt18),
          fromJson(idxm(18).value)(fmt19),
          fromJson(idxm(19).value)(fmt20),
          fromJson(idxm(20).value)(fmt21)
        )
      }  
      case _ => throw new RuntimeException("Map with size>="+sz+" expected")
    }
  }    

 implicit def tuple22Reads[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22](implicit fmt1: Reads[T1], fmt2: Reads[T2], fmt3: Reads[T3], fmt4: Reads[T4], fmt5: Reads[T5], fmt6: Reads[T6], fmt7: Reads[T7], fmt8: Reads[T8], fmt9: Reads[T9], fmt10: Reads[T10], fmt11: Reads[T11], fmt12: Reads[T12], fmt13: Reads[T13], fmt14: Reads[T14], fmt15: Reads[T15], fmt16: Reads[T16], fmt17: Reads[T17], fmt18: Reads[T18], fmt19: Reads[T19], fmt20: Reads[T20], fmt21: Reads[T21], fmt22: Reads[T22], sz: Int = 22): Reads[Tuple22[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22]] = new Reads[Tuple22[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22]]{
    def reads(json: JsValue) = json match {
      case JsObject(m) if(m.size >= sz) => {
        val idxm = m.view(0, sz).toIndexedSeq
        (
          fromJson(idxm(0).value)(fmt1),
          fromJson(idxm(1).value)(fmt2),
          fromJson(idxm(2).value)(fmt3),
          fromJson(idxm(3).value)(fmt4),
          fromJson(idxm(4).value)(fmt5),
          fromJson(idxm(5).value)(fmt6),
          fromJson(idxm(6).value)(fmt7),
          fromJson(idxm(7).value)(fmt8),
          fromJson(idxm(8).value)(fmt9),
          fromJson(idxm(9).value)(fmt10),
          fromJson(idxm(10).value)(fmt11),
          fromJson(idxm(11).value)(fmt12),
          fromJson(idxm(12).value)(fmt13),
          fromJson(idxm(13).value)(fmt14),
          fromJson(idxm(14).value)(fmt15),
          fromJson(idxm(15).value)(fmt16),
          fromJson(idxm(16).value)(fmt17),
          fromJson(idxm(17).value)(fmt18),
          fromJson(idxm(18).value)(fmt19),
          fromJson(idxm(19).value)(fmt20),
          fromJson(idxm(20).value)(fmt21),
          fromJson(idxm(21).value)(fmt22)
        )
      }  
      case _ => throw new RuntimeException("Map with size>="+sz+" expected")
    }
  }      
}

