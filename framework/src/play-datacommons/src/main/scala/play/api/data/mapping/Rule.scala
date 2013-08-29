package play.api.data.mapping

case class Rule[I, O](m: Mapping[(Path, Seq[ValidationError]), I, O]) {
  def validate(data: I): VA[I, O] = m(data)

  def compose[P](path: Path)(sub: Rule[O, P]): Rule[I, P] =
    this.flatMap{ o => Rule(_ => sub.validate(o)) }.repath(path ++ _)

  def flatMap[B](f: O => Rule[I, B]): Rule[I, B] =
    Rule { d =>
      this.validate(d)
        .map(f)
        .fold(
          es => Failure(es),
          r => r.validate(d))
    }

  def orElse[OO >: O](t: => Rule[I, OO]): Rule[I, OO] =
    Rule(d => this.validate(d) orElse t.validate(d))

  // would be nice to have Kleisli in play
  def compose[P](sub: Rule[O, P]): Rule[I, P] = compose(Path())(sub)
  def compose[P](m: Mapping[ValidationError, O, P]): Rule[I, P] = compose(Rule.fromMapping(m))

  def repath(f: Path => Path): Rule[I, O] =
    Rule { d =>
      this.validate(d).fail.map{ _.map {
        case (p, errs) => f(p) -> errs
      }}
    }

}

object Rule {

  import play.api.libs.functional._

  def zero[O] = Rule[O, O](Success.apply)

  def fromMapping[I, O](f: Mapping[ValidationError, I, O]) =
    new Rule[I, O](f(_).fail.map(errs => Seq(Path() -> errs)))

  implicit def applicativeRule[I] = new Applicative[({type λ[O] = Rule[I, O]})#λ] {
    override def pure[A](a: A): Rule[I, A] =
      Rule(_ => Success(a))

    override def map[A, B](m: Rule[I, A], f: A => B): Rule[I, B] =
      Rule(d => m.m(d).map(f))

    override def apply[A, B](mf: Rule[I, A => B], ma: Rule[I, A]): Rule[I, B] =
      Rule{ d =>
        val a = ma.validate(d)
        val f = mf.validate(d)
        (f *> a).flatMap(x => f.map(_(x)))
      }
  }

  implicit def functorRule[I] = new Functor[({type λ[O] = Rule[I, O]})#λ] {
    def fmap[A, B](m: Rule[I, A], f: A => B): Rule[I, B] = applicativeRule[I].map(m, f)
  }


  // XXX: Helps the compiler a bit
  import play.api.libs.functional.syntax._
  implicit def cba[I] = functionalCanBuildApplicative[({type λ[O] = Rule[I, O]})#λ]
  implicit def fbo[I, O] = toFunctionalBuilderOps[({type λ[O] = Rule[I, O]})#λ, O] _
  implicit def ao[I, O] = toApplicativeOps[({type λ[O] = Rule[I, O]})#λ, O] _
  implicit def f[I, O] = toFunctorOps[({type λ[O] = Rule[I, O]})#λ, O] _
}
