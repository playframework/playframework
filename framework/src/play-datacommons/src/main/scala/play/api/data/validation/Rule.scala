package play.api.data.validation

/**
* When need to separate p from m so that we can compose Rules together, combining paths in results
*/
case class Rule[I, O](p: Path[I], m: Path[I] => Mapping[(Path[I], Seq[ValidationError]), I, O]) {
  def validate(data: I): VA[I, O] =
    m(p)(data)

  // XXX
  def compose[P](sub: Rule[O, P]): Rule[I, P] = {
    val rp = this
    Rule(rp.p, (path: Path[I]) => Mapping { (d: I) =>
      val v = rp.validate(d)
      v.fold(
        es => Failure(es.map{ case (_, errs) => rp.p -> errs }),
        s  => sub.validate(s).fail.map{ _.map {
          case (path, errs) => (rp.p compose path.as[I]) -> errs
        }})
    })
  }
}

object Rule {
  import play.api.libs.functional._

  def apply[I, O](f: I => Validation[ValidationError, O]) =
    new Rule(Path[I](), (p: Path[I]) => Mapping { (i: I) =>
      f(i).fail.map(errs => Seq(p -> errs)): VA[I, O]
    })

  implicit def applicativeRule[I] = new Applicative[({type f[O] = Rule[I, O]})#f] {
    override def pure[A](a: A): Rule[I, A] =
      Rule(Path[I](), (_: Path[I]) => Mapping{ (_: I) => Success(a) })

    override def map[A, B](m: Rule[I, A], f: A => B): Rule[I, B] =
      Rule(m.p, { p => Mapping{ d =>
        m.m(p)(d)
         .map(f)
      }})

    override def apply[A, B](mf: Rule[I, A => B], ma: Rule[I, A]): Rule[I, B] =
      Rule(Path[I](), { p => Mapping{d =>
        val a = ma.validate(d)
        val f = mf.validate(d)
        val res = (f *> a).flatMap(x => f.map(_(x)))
        res
      }})
  }

  implicit def functorRule[I] = new Functor[({type f[O] = Rule[I, O]})#f] {
    def fmap[A, B](m: Rule[I, A], f: A => B): Rule[I, B] = applicativeRule[I].map(m, f)
  }

  // Helps the compiler a bit
  import play.api.libs.functional.syntax._
  implicit def cba[I] = functionalCanBuildApplicative[({type f[O] = Rule[I, O]})#f]
  implicit def fbo[I, O] = toFunctionalBuilderOps[({type f[O] = Rule[I, O]})#f, O] _
  implicit def ao[I, O] = toApplicativeOps[({type f[O] = Rule[I, O]})#f, O] _
}


/*
case class Rule[I, O](m: Mapping[(Path[I], Seq[ValidationError]), I, O]) {
  def validate(data: I): VA[I, O] = m(data)
}


object Rule {
  import play.api.libs.functional._

  def apply[I, O](f: I => Validation[ValidationError, O]) =
    new Rule(Mapping { (i: I) =>
      f(i).fail.map(errs => Seq(Path[I]() -> errs)): VA[I, O]
    })

  implicit def applicativeRule[I] = new Applicative[({type f[O] = Rule[I, O]})#f] {
    override def pure[A](a: A): Rule[I, A] =
      Rule(Mapping{ (_: I) => Success(a) })

    override def map[A, B](m: Rule[I, A], f: A => B): Rule[I, B] =
      Rule(Mapping{ (d: I) => m.m(d).map(f) })

    override def apply[A, B](mf: Rule[I, A => B], ma: Rule[I, A]): Rule[I, B] =
      Rule(Mapping{ (d: I) =>
        val a = ma.validate(d)
        val f = mf.validate(d)
        val res = (f *> a).flatMap(x => f.map(_(x)))
        res
      })
  }

  implicit def functorRule[I] = new Functor[({type f[O] = Rule[I, O]})#f] {
    def fmap[A, B](m: Rule[I, A], f: A => B): Rule[I, B] = applicativeRule[I].map(m, f)
  }

  // Helps the compiler a bit
  import play.api.libs.functional.syntax._
  implicit def cba[I] = functionalCanBuildApplicative[({type f[O] = Rule[I, O]})#f]
  implicit def fbo[I, O] = toFunctionalBuilderOps[({type f[O] = Rule[I, O]})#f, O] _
  implicit def ao[I, O] = toApplicativeOps[({type f[O] = Rule[I, O]})#f, O] _
}
*/
