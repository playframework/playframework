package play.api.data.validation

case class Rule[I, O](m: Mapping[(Path[I], Seq[ValidationError]), I, O]) {
  def validate(data: I): VA[I, O] = m(data)

  def compose[P](path: Path[I])(sub: Rule[O, P]): Rule[I, P] = {
    val rp = this
    Rule{ d =>
      val v = rp.validate(d)
      v.fold(
        es => Failure(es.map{ case (p, errs) => (path ++ p.as[I]) -> errs }),
        s  => sub.validate(s).fail.map{ _.map {
          case (p, errs) => (path ++ p.as[I]) -> errs
        }})
    }
  }

  //TODO: repath
}

object Rule {

  import play.api.libs.functional._

  def zero[O] = Rule[O, O](Success.apply)

  def fromMapping[I, O](f: Mapping[ValidationError, I, O]) =
    new Rule(f(_: I).fail.map(errs => Seq(Path[I]() -> errs)))

  implicit def IasI[I] = Rule[I, I](i => Success(i))

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
