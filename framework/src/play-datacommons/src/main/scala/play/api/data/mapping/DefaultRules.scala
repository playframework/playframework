package play.api.data.mapping

trait DateRules {

  def date(format: String = "yyyy-MM-dd", corrector: String => String = identity) = Rule.fromMapping[String, java.util.Date]{ s =>
    def parseDate(input: String): Option[java.util.Date] = {
      // REMEMBER THAT SIMPLEDATEFORMAT IS NOT THREADSAFE
      val df = new java.text.SimpleDateFormat(format)
      df.setLenient(false)
      try { Some(df.parse(input)) } catch {
        case _: java.text.ParseException => None
      }
    }

    parseDate(corrector(s)) match {
      case Some(d) => Success(d)
      case None => Failure(Seq(ValidationError("validation.date", format)))
    }
  }

  implicit val date: Rule[String, java.util.Date] = date()

  /**
   * Rule for the `org.joda.time.DateTime` type.
   *
   * @param pattern a date pattern, as specified in `java.text.SimpleDateFormat`.
   * @param corrector a simple string transformation function that can be used to transform input String before parsing. Useful when standards are not exactly respected and require a few tweaks
   */
  def jodaDateRule(pattern: String, corrector: String => String = identity) = Rule.fromMapping[String, org.joda.time.DateTime] { s =>
    import scala.util.Try
    import org.joda.time.DateTime

    val df = org.joda.time.format.DateTimeFormat.forPattern(pattern)
    Try(df.parseDateTime(corrector(s)))
      .map(Success.apply)
      .getOrElse(Failure(Seq(ValidationError("validation.expected.jodadate.format", pattern))))
  }

  implicit def jodaTime = Rule.fromMapping[Long, org.joda.time.DateTime] { d =>
    import org.joda.time.DateTime
    Success(new DateTime(d.toLong))
  }

  /**
   * the default implicit JodaDate reads
   */
  implicit val jodaDate = jodaDateRule("yyyy-MM-dd")

  implicit def jodaLocalDateRule(pattern: String, corrector: String => String = identity) = Rule.fromMapping[String, org.joda.time.LocalDate] { s =>
    import scala.util.Try
    import org.joda.time.LocalDate
    import org.joda.time.format.{ DateTimeFormat, ISODateTimeFormat }

    val df = if (pattern == "") ISODateTimeFormat.localDateParser else DateTimeFormat.forPattern(pattern)
    Try(LocalDate.parse(corrector(s), df))
      .map(Success.apply)
      .getOrElse(Failure(Seq(ValidationError("validation.expected.jodadate.format", pattern))))
  }
  /**
   * the default implicit joda.time.LocalDate reads
   */
  implicit val jodaLocalDate = jodaLocalDateRule("")

  /**
  * ISO 8601 Reads
  */
  val isoDate = Rule.fromMapping[String, java.util.Date]{ s =>
    import scala.util.Try
    import java.util.Date
    import org.joda.time.format.ISODateTimeFormat
    val parser = ISODateTimeFormat.dateOptionalTimeParser()
    Try(parser.parseDateTime(s).toDate())
      .map(Success.apply)
      .getOrElse(Failure(Seq(ValidationError("validation.iso8601"))))
  }

  def sqlDateRule(pattern: String, corrector: String => String = identity): Rule[String, java.sql.Date] =
    date(pattern, corrector).fmap(d => new java.sql.Date(d.getTime))

  implicit val sqlDate = sqlDateRule("yyyy-MM-dd")
}

trait GenericRules {

  def validateWith[From](msg: String, args: Any*)(pred: From => Boolean) = Rule.fromMapping[From, From] {
    v => if(!pred(v)) Failure(Seq(ValidationError(msg, args: _*))) else Success(v)
  }

  implicit def array[I, O: scala.reflect.ClassTag](implicit r: Rule[I, O]): Rule[Seq[I], Array[O]] =
    seq[I, O](r).fmap(_.toArray)

  implicit def traversable[I, O](implicit r: Rule[I, O]): Rule[Seq[I], Traversable[O]] =
    seq[I, O](r).fmap(_.toTraversable)

  implicit def seq[I, O](implicit r: Rule[I, O]): Rule[Seq[I], Seq[O]] =
    Rule { case is =>
      val withI = is.zipWithIndex.map { case (v, i) =>
        r.repath((Path \ i) ++ _).validate(v)
      }
      Validation.sequence(withI)
    }

  implicit def list[I, O](implicit r: Rule[I, O]): Rule[Seq[I], List[O]] =
    seq[I, O](r).fmap(_.toList)

  implicit def headAs[I, O](implicit c: Rule[I, O]) = Rule.fromMapping[Seq[I], I] {
    _.headOption.map(Success[ValidationError, I](_))
      .getOrElse(Failure[ValidationError, I](Seq(ValidationError("validation.required"))))
  }.compose(c)

  def not[I, O](r: Rule[I, O]) = Rule[I, I] { d =>
    r.validate(d) match {
      case Success(_) => Failure(Nil)
      case Failure(_) => Success(d)
    }
  }

  def equalTo[T](t: T) = validateWith[T]("validation.equals", t){ _.equals(t) }
  def notEmpty = validateWith[String]("validation.nonemptytext"){ !_.isEmpty }
  def min[T](m: T)(implicit o: Ordering[T]) = validateWith[T]("validation.min", m){ x => o.gteq(x, m) }
  def max[T](m: T)(implicit o: Ordering[T]) = validateWith[T]("validation.max", m){ x => o.lteq(x, m) }
  def minLength(l: Int) = validateWith[String]("validation.minLength", l){ _.size >= l }
  def maxLength(l: Int) = validateWith[String]("validation.maxLength", l){ _.size <= l }
  def pattern(regex: scala.util.matching.Regex) = validateWith("validation.pattern", regex){regex.unapplySeq(_: String).isDefined}
  def email = Rule.fromMapping[String, String](
    pattern("""\b[a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*\b""".r)
      .validate(_: String)
      .fail.map(_ => Seq(ValidationError("validation.email"))))
  def noConstraint[From]: Constraint[From] = Success(_)

  def checked[I](implicit b: Rule[I, Boolean]) = b compose Rules.equalTo(true)
}

trait DefaultRules[I] extends GenericRules with DateRules {
  import scala.language.implicitConversions
  import play.api.libs.functional._

  implicit def monoidConstraint[T] = new Monoid[Constraint[T]] {
    def append(c1: Constraint[T], c2: Constraint[T]) = v => c1(v) *> (c2(v))
    def identity = noConstraint[T]
  }

  def ignored[O](x: O) = (_: Path) => Rule[I, O](_ => Success(x))

  protected def option[J, O](r: Rule[J, O], noneValues: Rule[J, J]*)(implicit pick: Path => Rule[I, J]) = (path: Path) =>
    Rule[I, Option[O]] {
      (d: I) =>
        val isNone = not(noneValues.foldLeft(Rule.zero[J])(_ compose not(_))).fmap(_ => None)
        (pick(path).validate(d).map(Some.apply) orElse Success(None))
          .flatMap {
            case None => Success(None)
            case Some(i) => (isNone orElse r.fmap[Option[O]](Some.apply)).validate(i)
          }
    }

  def map[K, O](r: Rule[K, O], p: Rule[I, Seq[(String, K)]]): Rule[I, Map[String, O]] = {
    p.compose(Path)(
      Rule{ fs =>
        val validations = fs.map{ f =>
          r.repath((Path \ f._1) ++ _)
            .validate(f._2)
            .map(f._1 -> _)
        }
        Validation.sequence(validations).map(_.toMap)
      })
  }

}