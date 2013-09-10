package play.api.data.mapping

trait GenericRules {
  implicit def IasI[I] = Rule[I, I](i => Success(i))

  def validateWith[From](msg: String, args: Any*)(pred: From => Boolean): Constraint[From] =
    v => if(!pred(v)) Failure(Seq(ValidationError(msg, args: _*))) else Success(v)

  def array[I, O: scala.reflect.ClassTag](r: Rule[I, O]): Rule[Seq[I], Array[O]] =
    seq[I, O](r).fmap(_.toArray)

  def traversable[I, O](r: Rule[I, O]): Rule[Seq[I], Traversable[O]] =
    seq[I, O](r).fmap(_.toTraversable)

  def seq[I, O](r: Rule[I, O]): Rule[Seq[I], Seq[O]] =
    Rule { case is =>
      val withI = is.zipWithIndex.map { case (v, i) =>
        r.repath((Path() \ i) ++ _).validate(v)
      }
      Validation.sequence(withI)
    }

  def list[I, O](r: Rule[I, O]): Rule[Seq[I], List[O]] =
    seq[I, O](r).fmap(_.toList)

  def not[I, O](r: Rule[I, O]) = Rule[I, I] { d =>
    r.validate(d) match {
      case Success(_) => Failure(Nil)
      case Failure(_) => Success(d)
    }
  }

  def notEmpty = validateWith[String]("validation.nonemptytext"){ !_.isEmpty }
  def min[T](m: T)(implicit o: Ordering[T]) = validateWith[T]("validation.min", m){ x => o.gteq(x, m) }
  def max[T](m: T)(implicit o: Ordering[T]) = validateWith[T]("validation.max", m){ x => o.lteq(x, m) }
  def minLength(l: Int) = validateWith[String]("validation.minLength", l){ _.size >= l }
  def maxLength(l: Int) = validateWith[String]("validation.maxLength", l){ _.size <= l }
  def pattern(regex: scala.util.matching.Regex) = validateWith("validation.pattern", regex){regex.unapplySeq(_: String).isDefined}
  def email = pattern("""\b[a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*\b""".r)(_: String).fail.map(_ => Seq(ValidationError("validation.email")))
  def noConstraint[From]: Constraint[From] = Success(_)
}

trait DefaultRules[I] extends GenericRules {
  import scala.language.implicitConversions
  import play.api.libs.functional._

  implicit def monoidConstraint[T] = new Monoid[Constraint[T]] {
    def append(c1: Constraint[T], c2: Constraint[T]) = v => c1(v) *> (c2(v))
    def identity = noConstraint[T]
  }

  def ignored[O](x: O) = Rule[I, O](_ => Success(x))

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

  val date: Rule[String, java.util.Date] = date()

  /**
   * ISO 8601 Reads
   */
  val isoDate = date("yyyy-MM-dd'T'HH:mm:ssz", { input =>
    // NOTE: SimpleDateFormat uses GMT[-+]hh:mm for the TZ so need to refactor a bit
    // 1994-11-05T13:15:30Z -> 1994-11-05T13:15:30GMT-00:00
    // 1994-11-05T08:15:30-05:00 -> 1994-11-05T08:15:30GMT-05:00
    if (input.endsWith("Z")) {
      input.substring(0, input.length() - 1) + "GMT-00:00"
    } else {
      val inset = 6

      val s0 = input.substring(0, input.length - inset)
      val s1 = input.substring(input.length - inset, input.length)

      s0 + "GMT" + s1
    }
  })

  def option[J, O](r: Rule[J, O], noneValues: Rule[J, J]*)(implicit pick: Path => Rule[I, J]) = (path: Path) =>
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
    p.compose(Path())(
      Rule{ fs =>
        val validations = fs.map{ f =>
          r.repath((Path() \ f._1) ++ _)
            .validate(f._2)
            .map(f._1 -> _)
        }
        Validation.sequence(validations).map(_.toMap)
      })
  }

}