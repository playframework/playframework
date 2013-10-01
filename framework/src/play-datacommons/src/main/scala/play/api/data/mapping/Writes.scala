package play.api.data.mapping

import scala.language.implicitConversions

trait Write[I, +O] {
  def writes(i: I): O

  def map[B](f: O => B) = Write[I, B] {
    f compose (this.writes _)
  }

  def compose[OO >: O, P](w: Write[OO, P]) =
    Write((w.writes _) compose (this.writes _))
}

trait DefaultMonoids {
  import play.api.libs.functional.Monoid

  implicit def mapMonoid = new Monoid[Map[String, Seq[String]]] {
    def append(a1: Map[String, Seq[String]], a2: Map[String, Seq[String]]) = a1 ++ a2
    def identity = Map.empty
  }
}

object Write {

  def apply[I, O](w: I => O): Write[I, O] = new Write[I, O] {
    def writes(i: I) = w(i)
  }

  implicit def zero[I]: Write[I, I] = Write(identity[I] _)

  import play.api.libs.functional._
  implicit def functionalCanBuildWrite[O](implicit m: Monoid[O]) = new FunctionalCanBuild[({type λ[I] = Write[I, O]})#λ] {
    def apply[A, B](wa: Write[A, O], wb: Write[B, O]): Write[A ~ B, O] = Write[A ~ B, O] { (x: A ~ B) =>
      x match {
        case a ~ b => m.append(wa.writes(a), wb.writes(b))
      }
    }
  }

  implicit def contravariantfunctorWrite[O] = new ContravariantFunctor[({type λ[I] = Write[I, O]})#λ] {
    def contramap[A, B](wa: Write[A, O], f: B => A): Write[B, O] = Write[B, O]( (b: B) => wa.writes(f(b)) )
  }

   // XXX: Helps the compiler a bit
  import play.api.libs.functional.syntax._
  implicit def fbo[I, O: Monoid](a: Write[I, O]) = toFunctionalBuilderOps[({type λ[I] = Write[I, O]})#λ, I](a)
  implicit def cfo[I, O](a: Write[I, O]) = toContraFunctorOps[({type λ[I] = Write[I, O]})#λ, I](a)

}

trait DateWrites {
  /**
   * Serializer for java.util.Date
   * @param pattern the pattern used by SimpleDateFormat
   */
  def dateWrite(pattern: String = "yyyy-MM-dd") = Write[java.util.Date, String] {
    (d: java.util.Date) => new java.text.SimpleDateFormat(pattern).format(d)
  }

  implicit val date = dateWrite()

  val isoDate = Write[java.util.Date, String]{ d =>
    import java.util.Date
    import org.joda.time.format.ISODateTimeFormat
    val fmt = ISODateTimeFormat.dateTimeNoMillis()
    fmt.print(d.getTime)
  }

  def jodaDateWrite(pattern: String) = Write[org.joda.time.DateTime, String] { d =>
    val fmt = org.joda.time.format.DateTimeFormat.forPattern(pattern)
    fmt.print(d)
  }

  implicit def jodaTime = Write[org.joda.time.DateTime, Long] { d =>
    d.getMillis
  }

  def jodaLocalDateWrite(pattern: String) = Write[org.joda.time.LocalDate, String] { d =>
    import org.joda.time.format.{ DateTimeFormat, ISODateTimeFormat }
    val fmt =  if (pattern == "") ISODateTimeFormat.date else DateTimeFormat.forPattern(pattern)
    fmt.print(d)
  }
  /**
   * the default implicit joda.time.LocalDate reads
   */
  implicit val jodaLocalDate = jodaLocalDateWrite("")

  /**
   * the default implicit JodaDate write
   */
  implicit val jodaDate = jodaDateWrite("yyyy-MM-dd")

  def sqlDateWrite(pattern: String): Write[java.sql.Date, String] =
    dateWrite(pattern).contramap((d: java.sql.Date) => new java.util.Date(d.getTime))

  implicit val sqlDate = sqlDateWrite("yyyy-MM-dd")
}

trait DefaultWrites extends DateWrites {
  import play.api.libs.functional.Monoid

  implicit def seq[I, O](implicit w: Write[I, O]) = Write[Seq[I], Seq[O]] {
    _.map(w.writes)
  }

  implicit def head[I, O](implicit w: Write[I, O]): Write[I, Seq[O]] = w.map(Seq(_))
}

trait GenericWrites[O] {
  implicit def array[I](implicit w: Write[Seq[I], O]) =
    Write((_: Array[I]).toSeq) compose w

  implicit def traversable[I](implicit w: Write[Seq[I], O]) =
    Write((_: Traversable[I]).toSeq) compose w
}

object Writes extends DefaultWrites with GenericWrites[PM.PM] with DefaultMonoids {

  import PM._

  implicit def anyval[T <: AnyVal] = Write((i: T) => i.toString)
  implicit def scalanumber[T <: scala.math.ScalaNumber] = Write((i: T) => i.toString)
  implicit def javanumber[T <: java.lang.Number] = Write((i: T) => i.toString)

  implicit def opm[O](implicit w: Write[O, M]) = Write[O, PM] {
    o => toPM(w.writes(o))
  }

  implicit def map[I](implicit w: Write[I, Seq[String]]) = Write[Map[String, I], PM] {
    m => toPM(m.mapValues(w.writes))
  }

  implicit def spm[O](implicit w: Write[O, PM]) =
    Write[Seq[O], PM]{ os =>
      os.zipWithIndex
        .toMap
        .flatMap{ case(o, i) =>
          repathPM(w.writes(o), (Path() \ i) ++ _)
        }
    }

  implicit def writeM[I](path: Path)(implicit w: Write[I, PM]) = Write[I, M] { i =>
    toM(repathPM(w.writes(i), path ++ _))
  }

  implicit def ospm[I](implicit w: Write[I, Seq[String]]) = Write[I, PM]{ i =>
    Map(Path() -> w.writes(i))
  }

  implicit def option[I](implicit w: Write[I, Seq[String]]) = Write[Option[I], PM] { m =>
    m.map(s => Map(Path() -> w.writes(s)))
     .getOrElse(Map.empty)
  }
}
