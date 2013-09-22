package play.api.data.mapping.json

import play.api.data.mapping._

import play.api.libs.json.{ Writes => JSWrites, DefaultWrites => _, PathNode => _, IdxPathNode => _, KeyPathNode => _, _ }

object Writes extends DefaultWrites {
  private def writeObj(j: JsValue, n: PathNode) = n match {
    case IdxPathNode(_) => Json.arr(j)
    case KeyPathNode(key) => Json.obj(key -> j)
  }

  implicit val string: Write[String, JsValue] =
    Write(s => JsString(s))

  // XXX
  private def tToJs[T] = Write[T, JsValue]((i: T) => JsNumber(BigDecimal(i.toString)))
  implicit def anyval[T <: AnyVal] = tToJs[T]
  implicit def scalanumber[T <: scala.math.ScalaNumber] = tToJs[T]
  implicit def javanumber[T <: java.lang.Number] = tToJs[T]

  implicit def seqToJsArray[I](implicit w: Write[I, JsValue]): Write[Seq[I], JsValue] =
    Write(ss => JsArray(ss.map(w.writes _)))

  implicit def option[I](path: Path)(implicit w: Path => Write[I, JsValue]) =
    Write[Option[I], JsValue]{
      _.map(o => w(path).writes(o))
       .getOrElse(Json.obj())
    }

  implicit def writeJson[I](path: Path)(implicit w: Write[I, JsValue]): Write[I, JsValue] = Write { i =>
    val ps = path.path.reverse
    val h = ps.head
    val o = writeObj(w.writes(i), h)
    ps.tail.foldLeft(o)(writeObj)
  }
}