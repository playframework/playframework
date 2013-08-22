package play.api.data.validation

trait DefaultRules {
  import scala.language.implicitConversions
  import play.api.libs.functional._

  def IasI[I] = Rule[I, I](i => Success(i))

  implicit def monoidConstraint[T] = new Monoid[Constraint[T]] {
    def append(c1: Constraint[T], c2: Constraint[T]) = v => c1(v) *> (c2(v))
    def identity = noConstraint[T]
  }

  def option[I, J, O](r: Rule[J, O])(implicit pick: Path => Rule[I, J]) = (path: Path) =>
    Rule[I, Option[O]] {
      (d: I) =>
        (pick(path).validate(d).map(Some.apply) | Success(None))
          .flatMap {
            case None => Success(None)
            case Some(i) => r.validate(i).map(Some.apply)
          }
    }

  def validateWith[From](msg: String, args: Any*)(pred: From => Boolean): Constraint[From] =
    v => if(!pred(v)) Failure(Seq(ValidationError(msg, args: _*))) else Success(v)

  def array[I, O: scala.reflect.ClassTag](r: Rule[I, O]): Rule[Seq[I], Array[O]] =
    seq[I, O](r).fmap(_.toArray)

  def traversable[I, O](r: Rule[I, O]): Rule[Seq[I], Traversable[O]] =
    seq[I, O](r).fmap(_.toTraversable)

  def seq[I, O](r: Rule[I, O]): Rule[Seq[I], Seq[O]] =
    Rule { case is =>
      val vs = is.map(r.validate _)
      val withI = vs.zipWithIndex.map { case (v, i) =>
          v.fail.map { errs =>
            errs.map { case (p, es) =>
              ((Path() \ i) ++ p) -> es // XXX: not a big fan of this "as". Feels like casting
            }
          }
        }
      Validation.sequence(withI)
    }

  def notEmpty = validateWith[String]("validation.nonemptytext"){ !_.isEmpty }
  def min[A](m: Ordered[A]) = validateWith[Ordered[A]]("validation.min", m){ _ >= m.asInstanceOf[A] }
  def max[A](m: Ordered[A]) = validateWith[Ordered[A]]("validation.max", m){ _ <= m.asInstanceOf[A] }
  def minLength(l: Int) = validateWith[String]("validation.minLength", l){ _.size >= l }
  def maxLength(l: Int) = validateWith[String]("validation.maxLength", l){ _.size <= l }
  def pattern(regex: scala.util.matching.Regex) = validateWith("validation.pattern", regex){regex.unapplySeq(_: String).isDefined}
  def email = pattern("""\b[a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*\b""".r)(_: String).fail.map(_ => Seq(ValidationError("validation.email")))
  def noConstraint[From]: Constraint[From] = Success(_)

}