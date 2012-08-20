package play.api.libs.json.util

class ApplicativeOps[M[_],A](ma:M[A])(implicit  a:Applicative[M]){

  def ~>[B](mb: M[B]):M[B] = a(a(a.pure((_:A) => (b:B) => b), ma),mb)
  def <~[B](mb: M[B]):M[A] = a(a(a.pure((a:A) => (_:B) => a), ma),mb)
  def <~>[B,C](mb: M[B])(implicit witness: <:<[A,B => C]):M[C] = apply(mb)
  def apply[B,C](mb: M[B])(implicit witness: <:<[A,B => C]):M[C] = a(a.map(ma,witness),mb)
}

class FunctionalBuilderOps[M[_],A](ma:M[A])(implicit fcb:FunctionalCanBuild[M]){

  def ~[B](mb:M[B]):FunctionalBuilder[M]#CanBuild2[A,B] = { 
    val b = new FunctionalBuilder(fcb)
    new b.CanBuild2(ma,mb)
  }

}

trait Applicative[M[_]]{

  def pure[A](a:A):M[A]
  def map[A,B](m:M[A], f: A => B):M[B]
  def apply[A,B](mf:M[A => B], ma:M[A]):M[B]

}

class AlternativeOps[M[_],A](alt1:M[A])(implicit a:Alternative[M]){

  def |[B >: A](alt2 :M[B]):M[B] = a.|(alt1,alt2)
  def or[B >: A](alt2 :M[B]):M[B] = |(alt2)

}

trait Alternative[M[_]]{

  def app:Applicative[M]
  def |[A,B >: A](alt1: M[A], alt2 :M[B]):M[B]
  def empty:M[Nothing]
  //def some[A](m:M[A]):M[List[A]]
  //def many[A](m:M[A]):M[List[A]]

}

trait FunctionalCanBuild[M[_]]{

  def apply[A,B](ma:M[A], mb:M[B]):M[A ~ B]

}

trait Functor[M[_]]{

  def fmap[A,B](m:M[A], f: A => B): M[B]

}

trait InvariantFunctor[M[_]]{

  def inmap[A,B](m:M[A], f1: A => B, f2: B => A):M[B]

}

trait ContravariantFunctor[M[_]]{

  def contramap[A,B](m:M[A], f1: B => A):M[B]

}

case class ~[A,B](_1:A,_2:B)

class FunctionalBuilder[M[_]](canBuild:FunctionalCanBuild[M]){

  class CanBuild2[A1,A2](m1:M[A1], m2:M[A2]){

    def ~[A3](m3:M[A3]) = new CanBuild3(canBuild(m1,m2),m3)

    def apply[B](f:(A1,A2) => B)(implicit fu:Functor[M]):M[B] =
      fu.fmap[A1 ~ A2, B](canBuild(m1,m2), {case a1 ~ a2 => f(a1,a2)} )

    def apply[B](f: B => (A1,A2))(implicit fu:ContravariantFunctor[M]):M[B] =
      fu.contramap(canBuild(m1,m2), (b:B) => { val (a1,a2) = f(b); new ~(a1,a2)})

    def apply[B](f1:(A1,A2) => B, f2: B => (A1,A2))(implicit fu:InvariantFunctor[M]):M[B] =
      fu.inmap[A1 ~ A2, B](canBuild(m1,m2),  {case a1 ~ a2 => f1(a1,a2)}, (b:B) => { val (a1,a2) = f2(b); new ~(a1,a2)})

  }

  class CanBuild3[A1,A2,A3](m1:M[A1 ~ A2], m2:M[A3]){

    def ~[A4](m3:M[A4]) = new CanBuild4(canBuild(m1,m2),m3)

    def apply[B](f:(A1,A2,A3) => B)(implicit fu:Functor[M]):M[B] =  null.asInstanceOf[M[B]]

    def apply[B](f: B => (A1,A2,A3))(implicit fu:ContravariantFunctor[M]):M[B] =  null.asInstanceOf[M[B]]

    def apply[B](f1:(A1,A2,A3) => B, f2: B => (A1,A2,A3))(implicit fu:InvariantFunctor[M]):M[B] =  null.asInstanceOf[M[B]]

  }

  class CanBuild4[A1,A2,A3,A4](m1:M[A1 ~ A2 ~ A3], m2:M[A4]){

    def ~[A5](m3:M[A5]) = new CanBuild4(canBuild(m1,m2),m3)

  }

  //class CanBuild4[A1,A2,A3,A4,A5](m1:M[A1 ~ A2 ~ A3 ~ A4], m2:M[A5])

}

object `package` {

  implicit def toAlternativeOps[M[_],A](a:M[A])(implicit app:Alternative[M]):AlternativeOps[M,A] = new AlternativeOps(a)

  implicit def toApplicativeOps[M[_],A](a:M[A])(implicit app:Applicative[M]):ApplicativeOps[M,A] = new ApplicativeOps(a)

  implicit def toFunctionalBuilderOps[M[_],A](a:M[A])(implicit fcb:FunctionalCanBuild[M]) = new FunctionalBuilderOps[M,A](a)(fcb)

  implicit def functionalCanBuildApplicative[M[_]](implicit app:Applicative[M]):FunctionalCanBuild[M] = new FunctionalCanBuild[M] {

    def apply[A,B](a: M[A], b:M[B]):M[A~B] = app.apply(app.map[A, B => A ~ B](a, a => ((b:B) => new ~(a,b))),b)

  }

  implicit def functorOption:Functor[Option] = new Functor[Option] {

    def fmap[A,B](a:Option[A], f: A => B):Option[B] = a.map(f)

  }

  implicit def applicativeOption:Applicative[Option] = new Applicative[Option]{

    def pure[A](a:A):Option[A] = Some(a)

    def map[A,B](m:Option[A], f: A => B):Option[B] = m.map(f)

    def apply[A,B](mf:Option[A => B], ma: Option[A]):Option[B] = mf.flatMap(f => ma.map(f))

  }
}


