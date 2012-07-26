package play.api.libs.json.util

class ApplicativeOps[M[_],A](ma:M[A]){

  def ~>[B](mb: M[B])(implicit a:Applicative[M]):M[B] = a(a(a.pure((_:A) => (b:B) => b), ma),mb)
  def <~[B](mb: M[B])(implicit a:Applicative[M]):M[A] = a(a(a.pure((a:A) => (_:B) => a), ma),mb)
  def <~>[B,C](mb: M[B])(implicit witness: <:<[A,B => C],  a:Applicative[M]):M[C] = apply(mb)
  def apply[B,C](mb: M[B])(implicit witness: <:<[A,B => C],  a:Applicative[M]):M[C] = a(a.map(ma,witness),mb)
  def ~[B](mb:M[B])(implicit a:Applicative[M]):ApplicativeBuilder[M]#Builder2[A,B] = { val ab = new ApplicativeBuilder(a); new ab.Builder2(ma,mb) }

}

class ApplicativeBuilder[M[_]]( app:Applicative[M]){

  class Builder2[A1,A2](m1:M[A1], m2:M[A2]){

    def ~[A3](m3:M[A3]): Builder3[A1,A2,A3] = new Builder3(m1,m2,m3)

    def tupled:M[(A1,A2)] = app.apply(app.apply(app.pure((a1:A1) => (a2:A2) => (a1,a2)),m1),m2)

    def apply[B](f: (A1,A2) => B ):M[B] = app.apply(app.pure(f.tupled), tupled)

  }

  class Builder3[A1,A2,A3](m1:M[A1], m2:M[A2], m3:M[A3]){

    def ~[A4](m4:M[A4]): Builder4[A1,A2,A3,A4] = new Builder4(m1,m2,m3,m4)

    def tupled:M[(A1,A2,A3)] = app.apply(app.apply(app.apply(app.pure((a1:A1) => (a2:A2) => (a3:A3) => (a1,a2,a3)),m1),m2),m3)

    def apply[B](f: (A1,A2,A3) => B ):M[B] = app.apply(app.pure(f.tupled), tupled)

  }

  class Builder4[A1,A2,A3,A4](m1:M[A1], m2:M[A2], m3:M[A3], m4:M[A4]){

    def tupled:M[(A1,A2,A3,A4)] =  app.apply(app.apply(app.apply(app.apply(app.pure((a1:A1) => (a2:A2) => (a3:A3) => (a4:A4) => (a1,a2,a3,a4)),m1),m2),m3),m4)

    def apply[B](f: (A1,A2,A3,A4) => B ):M[B] = app.apply(app.pure(f.tupled), tupled)

  }

}

trait Applicative[M[_]]{

  def pure[A](a:A):M[A]
  def map[A,B](m:M[A], f: A => B):M[B]
  def apply[A,B](mf:M[A => B], ma:M[A]):M[B]

}

trait AlternativeOps[M[_],A]{

  def |[AA >: A ,B >: AA](alt2 :M[B])(implicit a:Alternative[M]):M[AA]

}

trait Alternative[M[_]]{

  def app:Applicative[M]
  def |[A,B >: A](alt1: M[A], alt2 :M[B]):M[B]
  def empty:M[Nothing]
  //def some[A](m:M[A]):M[List[A]]
  //def many[A](m:M[A]):M[List[A]]

}
object `package` {

  implicit def toApplicativeOps[M[_],A](a:M[A]):ApplicativeOps[M,A] = new ApplicativeOps(a)

  implicit def applicativeOption:Applicative[Option] = new Applicative[Option]{

    def pure[A](a:A):Option[A] = Some(a)

    def map[A,B](m:Option[A], f: A => B):Option[B] = m.map(f)

    def apply[A,B](mf:Option[A => B], ma: Option[A]):Option[B] = mf.flatMap(f => ma.map(f))

  }
  import play.api.libs.json._
  implicit def applicativeReads:Applicative[Reads] = new Applicative[Reads]{

    def pure[A](a:A):Reads[A] = new Reads[A] { def reads(js: JsValue) = JsSuccess(a) }

    def map[A,B](m:Reads[A], f: A => B):Reads[B] = m.map(f)

    def apply[A,B](mf:Reads[A => B], ma: Reads[A]):Reads[B] = new Reads[B]{ def reads(js: JsValue) = applicativeJsResult(mf.reads(js),ma.reads(js)) }

  }

  implicit def applicativeJsResult:Applicative[JsResult] = new Applicative[JsResult] {

    def pure[A](a:A):JsResult[A] = JsSuccess(a)

    def map[A,B](m:JsResult[A], f: A => B):JsResult[B] = m.map(f)

    def apply[A,B](mf:JsResult[A => B], ma: JsResult[A]):JsResult[B] = (mf, ma) match {
      case (JsSuccess(f), JsSuccess(a)) => JsSuccess(f(a))
      case (JsError(e1), JsError(e2)) => JsError(JsError.merge(e1, e2))
      case (JsError(e), _) => JsError(e)
      case (_, JsError(e)) => JsError(e)
    }
  }

  implicit def alternativeJsResult(implicit a:Applicative[JsResult]):Alternative[JsResult] = new Alternative[JsResult]{
    val app = a
    def |[A,B >: A](alt1: JsResult[A], alt2 :JsResult[B]):JsResult[B] = (alt1, alt2) match {
      case (JsError(e), JsSuccess(t)) => JsSuccess(t)
      case (JsSuccess(t), _) => JsSuccess(t)
      case (JsError(e1), JsError(e2)) => JsError(JsError.merge(e1, e2))
    }
    def empty:JsResult[Nothing] = JsError(Seq())   
  }

  implicit def alternativeReads(implicit a:Applicative[Reads]):Alternative[Reads] = new Alternative[Reads]{
    val app = a
    def |[A,B >: A](alt1: Reads[A], alt2 :Reads[B]):Reads[B] = new Reads[B] {
      def reads(js: JsValue) = alt1.reads(js) match {
        case r@JsSuccess(_) => r
        case r@JsError(es1) => alt2.reads(js) match {
          case r2@JsSuccess(_) => r2
          case r2@JsError(es2) => JsError(JsError.merge(es1,es2))
        }
      }
    }
    def empty:Reads[Nothing] = new Reads[Nothing] { def reads(js: JsValue) = JsError(Seq()) }
  }
}


