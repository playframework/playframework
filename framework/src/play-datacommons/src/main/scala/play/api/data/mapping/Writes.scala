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

  protected def optionW[I, J, O](r: => WriteLike[I, J], empty: O)(implicit w: Path => WriteLike[J, O]) =
    (p: Path) => Write[Option[I], O] { maybeI =>
      maybeI.map { i =>
        Write.toWrite(w(p)).contramap(r.writes).writes(i)
      }.getOrElse(empty)
    }

  implicit def seqW[I, O](implicit w: WriteLike[I, O]) = Write[Seq[I], Seq[O]] {
    _.map(w.writes)
  }

  implicit def headW[I, O](implicit w: WriteLike[I, O]): Write[I, Seq[O]] = Write.toWrite(w).map(Seq(_))

  def ignored[O](x: O) = Write[O, O](_ => x)
}

trait GenericWrites[O] {

  implicit def arrayW[I](implicit w: WriteLike[Seq[I], O]) =
    Write((_: Array[I]).toSeq) compose w

  implicit def listW[I](implicit w: WriteLike[Seq[I], O]) =
    Write((_: List[I]).toSeq) compose w

  implicit def traversableW[I](implicit w: WriteLike[Seq[I], O]) =
    Write((_: Traversable[I]).toSeq) compose w

  implicit def setW[I](implicit w: WriteLike[Seq[I], O]) =
    Write((_: Set[I]).toSeq) compose w
}

object Writes extends DefaultWrites with GenericWrites[PM.PM] with DefaultMonoids {

  import PM._

  // TODO: accept a format ?
  implicit def anyval[T <: AnyVal] = Write((i: T) => i.toString)
  implicit def scalanumber[T <: scala.math.ScalaNumber] = Write((i: T) => i.toString)
  implicit def javanumber[T <: java.lang.Number] = Write((i: T) => i.toString)

  implicit def opm[O](implicit w: WriteLike[O, UrlFormEncoded]) = Write[O, PM] {
    o => toPM(w.writes(o))
  }

  implicit def mapW[I](implicit w: WriteLike[I, Seq[String]]) = Write[Map[String, I], PM] {
    m => toPM(m.mapValues(w.writes))
  }

  implicit def spm[O](implicit w: WriteLike[O, PM]) =
    Write[Seq[O], PM] { os =>
      os.zipWithIndex
        .toMap
        .flatMap {
          case (o, i) =>
            repathPM(w.writes(o), (Path \ i) ++ _)
        }
    }

  implicit def writeM[I](path: Path)(implicit w: WriteLike[I, PM]) = Write[I, UrlFormEncoded] { i =>
    toM(repathPM(w.writes(i), path ++ _))
  }

  implicit def ospm[I](implicit w: WriteLike[I, String]) = Write[I, PM] { i =>
    Map(Path -> w.writes(i))
  }

  implicit def optW[I](implicit w: Path => WriteLike[I, UrlFormEncoded]): Path => Write[Option[I], UrlFormEncoded] =
    optionW[I, I](Write.zero[I])

  def optionW[I, J](r: => WriteLike[I, J])(implicit w: Path => WriteLike[J, UrlFormEncoded]) =
    super.optionW[I, J, UrlFormEncoded](r, Map.empty)

}
