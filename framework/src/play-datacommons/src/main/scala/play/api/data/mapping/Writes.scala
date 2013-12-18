package play.api.data.mapping

import scala.language.implicitConversions

trait DateWrites {
  /**
   * Serializer for java.util.Date
   * @param pattern the pattern used by SimpleDateFormat
   */
  def date(pattern: String = "yyyy-MM-dd") = Write[java.util.Date, String] {
    (d: java.util.Date) => new java.text.SimpleDateFormat(pattern).format(d)
  }
  implicit val date: Write[java.util.Date, String] = date()

  val isoDate = Write[java.util.Date, String] { d =>
    import java.util.Date
    import org.joda.time.format.ISODateTimeFormat
    val fmt = ISODateTimeFormat.dateTimeNoMillis()
    fmt.print(d.getTime)
  }

  def jodaDate(pattern: String) = Write[org.joda.time.DateTime, String] { d =>
    val fmt = org.joda.time.format.DateTimeFormat.forPattern(pattern)
    fmt.print(d)
  }

  implicit def jodaTime = Write[org.joda.time.DateTime, Long] { d =>
    d.getMillis
  }

  def jodaLocalDate(pattern: String) = Write[org.joda.time.LocalDate, String] { d =>
    import org.joda.time.format.{ DateTimeFormat, ISODateTimeFormat }
    val fmt = if (pattern == "") ISODateTimeFormat.date else DateTimeFormat.forPattern(pattern)
    fmt.print(d)
  }
  /**
   * the default implicit joda.time.LocalDate reads
   */
  implicit val jodaLocalDate: Write[org.joda.time.LocalDate, String] = jodaLocalDate("")

  /**
   * the default implicit JodaDate write
   */
  implicit val jodaDate: Write[org.joda.time.DateTime, String] = jodaDate("yyyy-MM-dd")

  def sqlDate(pattern: String): Write[java.sql.Date, String] =
    date(pattern).contramap((d: java.sql.Date) => new java.util.Date(d.getTime))

  val sqlDate: Write[java.sql.Date, String] = sqlDate("yyyy-MM-dd")
}

trait DefaultWrites extends DateWrites {
  import play.api.libs.functional.Monoid

  protected def option[I, J, O](r: => Write[I, J], empty: O)(implicit w: Path => Write[J, O]) =
    (p: Path) => Write[Option[I], O] { maybeI =>
      maybeI.map { i =>
        w(p).contramap(r.writes).writes(i)
      }.getOrElse(empty)
    }

  implicit def seq[I, O](implicit w: Write[I, O]) = Write[Seq[I], Seq[O]] {
    _.map(w.writes)
  }

  implicit def head[I, O](implicit w: Write[I, O]): Write[I, Seq[O]] = w.map(Seq(_))

  def ignored[O](x: O) = Write[O, O](_ => x)
}

trait GenericWrites[O] {

  implicit def array[I](implicit w: Write[Seq[I], O]) =
    Write((_: Array[I]).toSeq) compose w

  implicit def list[I](implicit w: Write[Seq[I], O]) =
    Write((_: List[I]).toSeq) compose w

  implicit def traversable[I](implicit w: Write[Seq[I], O]) =
    Write((_: Traversable[I]).toSeq) compose w

  implicit def set[I](implicit w: Write[Seq[I], O]) =
    Write((_: Set[I]).toSeq) compose w
}

object Writes extends DefaultWrites with GenericWrites[PM.PM] with DefaultMonoids {

  import PM._

  // TODO: accept a format ?
  implicit def anyval[T <: AnyVal] = Write((i: T) => i.toString)
  implicit def scalanumber[T <: scala.math.ScalaNumber] = Write((i: T) => i.toString)
  implicit def javanumber[T <: java.lang.Number] = Write((i: T) => i.toString)

  implicit def opm[O](implicit w: Write[O, UrlFormEncoded]) = Write[O, PM] {
    o => toPM(w.writes(o))
  }

  implicit def map[I](implicit w: Write[I, Seq[String]]) = Write[Map[String, I], PM] {
    m => toPM(m.mapValues(w.writes))
  }

  implicit def spm[O](implicit w: Write[O, PM]) =
    Write[Seq[O], PM] { os =>
      os.zipWithIndex
        .toMap
        .flatMap {
          case (o, i) =>
            repathPM(w.writes(o), (Path \ i) ++ _)
        }
    }

  implicit def writeM[I](path: Path)(implicit w: Write[I, PM]) = Write[I, UrlFormEncoded] { i =>
    toM(repathPM(w.writes(i), path ++ _))
  }

  implicit def ospm[I](implicit w: Write[I, String]) = Write[I, PM] { i =>
    Map(Path -> w.writes(i))
  }

  implicit def opt[I](implicit w: Path => Write[I, UrlFormEncoded]): Path => Write[Option[I], UrlFormEncoded] =
    option[I, I](Write.zero[I])

  def option[I, J](r: => Write[I, J])(implicit w: Path => Write[J, UrlFormEncoded]) =
    super.option[I, J, UrlFormEncoded](r, Map.empty)

}
