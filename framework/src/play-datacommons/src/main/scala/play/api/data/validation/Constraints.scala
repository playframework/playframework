package play.api.data.validation

object Constraints {
  import scala.util.matching.Regex

  def validateWith[From](msg: String, args: Any*)(pred: From => Boolean): Constraint[From] =
    Mapping(v => if(!pred(v)) Failure(Seq(ValidationError(msg, args: _*))) else Success(v))

  def optional[O](c: Constraint[O]): Constraint[Option[O]] =
    Mapping(_.map(v => c(v).map(Some.apply)).getOrElse(Success(None)))

  def seq[O](c: Constraint[O]): Constraint[Seq[O]] =
    Mapping(vs => Validation.sequence(vs.map(c)))

  def seq[I, O](r: Rule[I, O]): Rule[Seq[I], Seq[O]] =
    Rule(Path[Seq[I]](), (p: Path[Seq[I]]) => Mapping{ is: Seq[I] =>
      val vs = is.map(r.validate _)
      val withI = vs.zipWithIndex.map { case (v, i) =>
          v.fail.map { errs =>
            errs.map { case (path, es) => (p.as[Seq[I]] \ i).compose(path.as[Seq[I]]) -> es }
          }
        }
      Validation.sequence(withI)
    })

  def list[O](c: Constraint[O]): Constraint[List[O]] =
    Mapping(seq(c)(_).map(_.toList))

  def nonEmptyText = validateWith("validation.nonemptytext"){ !(_: String).isEmpty }
  def min(m: Int) = validateWith("validation.min", m){(_: Int) > m}
  def max(m: Int) = validateWith("validation.max", m){(_: Int) < m}
  def minLength(l: Int) = validateWith("validation.minLength", l){(_: String).size >= l}
  def maxLength(l: Int) = validateWith("validation.maxLength", l){(_: String).size < l}
  def pattern(regex: Regex) = validateWith("validation.pattern", regex){regex.unapplySeq(_: String).isDefined}
  def email = Mapping(pattern("""\b[a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*\b""".r)(_: String).fail.map(_ => Seq(ValidationError("validation.email"))))
  def noConstraint[From]: Constraint[From] = Mapping(Success(_))
}