package play.api.data.validation

trait DefaultRules[I, J] {
  import scala.language.implicitConversions
  import play.api.libs.functional._

  def IasI[In] = Rule[In, In](i => Success(i))

  implicit def monoidConstraint[T] = new Monoid[Constraint[T]] {
    def append(c1: Constraint[T], c2: Constraint[T]) = v => c1(v) *> (c2(v))
    def identity = noConstraint[T]
  }

  def option[O](r: Rule[J, O], noneValues: Rule[J, J]*)(implicit pick: Path => Rule[I, J]) = (path: Path) =>
    Rule[I, Option[O]] {
      (d: I) =>
        val isNone = not(noneValues.foldLeft(Rule.zero[J])(_ compose not(_))).fmap(_ => None)
        (pick(path).validate(d).map(Some.apply) orElse Success(None))
          .flatMap {
            case None => Success(None)
            case Some(i) => (isNone orElse r.fmap[Option[O]](Some.apply)).validate(i)
          }
    }

  def validateWith[From](msg: String, args: Any*)(pred: From => Boolean): Constraint[From] =
    v => if(!pred(v)) Failure(Seq(ValidationError(msg, args: _*))) else Success(v)

  def array[O: scala.reflect.ClassTag](r: Rule[I, O]): Rule[Seq[I], Array[O]] =
    seq[O](r).fmap(_.toArray)

  def traversable[O](r: Rule[I, O]): Rule[Seq[I], Traversable[O]] =
    seq[O](r).fmap(_.toTraversable)

  def seq[O](r: Rule[I, O]): Rule[Seq[I], Seq[O]] =
    Rule { case is =>
      val withI = is.zipWithIndex.map { case (v, i) =>
        r.repath((Path() \ i) ++ _).validate(v)
      }
      Validation.sequence(withI)
    }

  def not[In, Out](r: Rule[In, Out]) = Rule[In, In] { d =>
    r.validate(d) match {
      case Success(_) => Failure(Nil)
      case Failure(_) => Success(d)
    }
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