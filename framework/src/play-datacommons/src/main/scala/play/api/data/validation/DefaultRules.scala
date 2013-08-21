package play.api.data.validation

trait DefaultRules {
  import scala.language.implicitConversions
  import play.api.libs.functional._

  implicit def monoidConstraint[T] = new Monoid[Constraint[T]] {
    def append(c1: Constraint[T], c2: Constraint[T]) = v => c1(v) *> (c2(v))
    def identity = noConstraint[T]
  }

  def opt[I, J, O](path: Path[I])(implicit pick: Path[I] => Rule[I, J], c:  Rule[J, O]) =
    Rule[I, Option[O]] {
      (d: I) =>
        (pick(path).validate(d).map(Some.apply) | Success(None))
          .flatMap {
            case None => Success(None)
            case Some(i) => c.validate(i).map(Some.apply)
          }.fail.map(_.map{ case (p, es) => p.as[I] -> es })
  }

  implicit def optR[I, O](path: Path[I])(implicit pick: Path[I] => Rule[I, I], c:  Rule[I, O]) =
    opt[I, I, O](path)(pick, c)

  def validateWith[From](msg: String, args: Any*)(pred: From => Boolean): Constraint[From] =
    v => if(!pred(v)) Failure(Seq(ValidationError(msg, args: _*))) else Success(v)

  def optional[O](c: Constraint[O]): Constraint[Option[O]] =
    _.map(v => c(v).map(Some.apply)).getOrElse(Success(None))

  def seq[O](c: Constraint[O]): Rule[Seq[O], Seq[O]] =
    seq(Rule.fromMapping[O, O](c))

  def seq[I, O](r: Rule[I, O]): Rule[Seq[I], Seq[O]] =
    Rule { case is =>
      val vs = is.map(r.validate _)
      val withI = vs.zipWithIndex.map { case (v, i) =>
          v.fail.map { errs =>
            errs.map { case (p, es) =>
              ((Path[Seq[I]]() \ i) ++ p.as[Seq[I]]) -> es // XXX: not a big fan of this "as". Feels like casting
            }
          }
        }
      Validation.sequence(withI)
    }

  def nonEmptyText = validateWith("validation.nonemptytext"){ !(_: String).isEmpty }
  def min(m: Int) = validateWith("validation.min", m){(_: Int) > m}
  def max(m: Int) = validateWith("validation.max", m){(_: Int) < m}
  def minLength(l: Int) = validateWith("validation.minLength", l){(_: String).size >= l}
  def maxLength(l: Int) = validateWith("validation.maxLength", l){(_: String).size < l}
  def pattern(regex: scala.util.matching.Regex) = validateWith("validation.pattern", regex){regex.unapplySeq(_: String).isDefined}
  def email = pattern("""\b[a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*\b""".r)(_: String).fail.map(_ => Seq(ValidationError("validation.email")))
  def noConstraint[From]: Constraint[From] = Success(_)

}