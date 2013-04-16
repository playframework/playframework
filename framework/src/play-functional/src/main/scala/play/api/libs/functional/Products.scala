package play.api.libs.functional

import scala.language.higherKinds

case class ~[A, B](_1: A, _2: B)

trait FunctionalCanBuild[M[_]] {

  def apply[A, B](ma: M[A], mb: M[B]): M[A ~ B]

}

class FunctionalBuilderOps[M[_], A](ma: M[A])(implicit fcb: FunctionalCanBuild[M]) {

  def ~[B](mb: M[B]): FunctionalBuilder[M]#CanBuild2[A, B] = {
    val b = new FunctionalBuilder(fcb)
    new b.CanBuild2[A, B](ma, mb)
  }

  def and[B](mb: M[B]): FunctionalBuilder[M]#CanBuild2[A, B] = this.~(mb)
}

class FunctionalBuilder[M[_]](canBuild: FunctionalCanBuild[M]) {

  class CanBuild2[A1, A2](m1: M[A1], m2: M[A2]) {

    def ~[A3](m3: M[A3]) = new CanBuild3[A1, A2, A3](canBuild(m1, m2), m3)

    def and[A3](m3: M[A3]) = this.~(m3)

    def apply[B](f: (A1, A2) => B)(implicit fu: Functor[M]): M[B] =
      fu.fmap[A1 ~ A2, B](canBuild(m1, m2), { case a1 ~ a2 => f(a1, a2) })

    def apply[B](f: B => (A1, A2))(implicit fu: ContravariantFunctor[M]): M[B] =
      fu.contramap(canBuild(m1, m2), (b: B) => { val (a1, a2) = f(b); new ~(a1, a2) })

    def apply[B](f1: (A1, A2) => B, f2: B => (A1, A2))(implicit fu: InvariantFunctor[M]): M[B] =
      fu.inmap[A1 ~ A2, B](
        canBuild(m1, m2), { case a1 ~ a2 => f1(a1, a2) },
        (b: B) => { val (a1, a2) = f2(b); new ~(a1, a2) }
      )

    def join[A >: A1](implicit witness1: <:<[A, A1], witness2: <:<[A, A2], fu: ContravariantFunctor[M]): M[A] =
      apply[A]((a: A) => (a: A1, a: A2))(fu)

    def reduce[A >: A1, B](implicit witness1: <:<[A1, A], witness2: <:<[A2, A], fu: Functor[M], reducer: Reducer[A, B]): M[B] =
      apply[B]((a1: A1, a2: A2) => reducer.append(reducer.unit(a1: A), a2: A))(fu)

    def tupled(implicit v: Variant[M]): M[(A1, A2)] =
      // SO UGLY UGLY UGLY workaround for unchecked type erasure warning... no cleaner way found...
      (v: Any) match {
        case fu: Functor[_] => apply { (a1: A1, a2: A2) => (a1, a2) }(fu.asInstanceOf[Functor[M]])
        case fu: ContravariantFunctor[_] => apply[(A1, A2)] { (a: (A1, A2)) => (a._1, a._2) }(fu.asInstanceOf[ContravariantFunctor[M]])
        case fu: InvariantFunctor[_] => apply[(A1, A2)]({ (a1: A1, a2: A2) => (a1, a2) }, { (a: (A1, A2)) => (a._1, a._2) })(fu.asInstanceOf[InvariantFunctor[M]])
      }

  }

  class CanBuild3[A1, A2, A3](m1: M[A1 ~ A2], m2: M[A3]) {

    def ~[A4](m3: M[A4]) = new CanBuild4[A1, A2, A3, A4](canBuild(m1, m2), m3)

    def and[A4](m3: M[A4]) = this.~(m3)

    def apply[B](f: (A1, A2, A3) => B)(implicit fu: Functor[M]): M[B] =
      fu.fmap[A1 ~ A2 ~ A3, B](canBuild(m1, m2), { case a1 ~ a2 ~ a3 => f(a1, a2, a3) })

    def apply[B](f: B => (A1, A2, A3))(implicit fu: ContravariantFunctor[M]): M[B] =
      fu.contramap(canBuild(m1, m2), (b: B) => { val (a1, a2, a3) = f(b); new ~(new ~(a1, a2), a3) })

    def apply[B](f1: (A1, A2, A3) => B, f2: B => (A1, A2, A3))(implicit fu: InvariantFunctor[M]): M[B] =
      fu.inmap[A1 ~ A2 ~ A3, B](
        canBuild(m1, m2), { case a1 ~ a2 ~ a3 => f1(a1, a2, a3) },
        (b: B) => { val (a1, a2, a3) = f2(b); new ~(new ~(a1, a2), a3) }
      )

    def join[A >: A1](implicit witness1: <:<[A, A1], witness2: <:<[A, A2], witness3: <:<[A, A3], fu: ContravariantFunctor[M]): M[A] =
      apply[A]((a: A) => (a: A1, a: A2, a: A3))(fu)

    def reduce[A >: A1, B](implicit witness1: <:<[A1, A], witness2: <:<[A2, A], witness3: <:<[A3, A], fu: Functor[M], reducer: Reducer[A, B]): M[B] =
      apply[B]((a1: A1, a2: A2, a3: A3) => reducer.append(reducer.append(reducer.unit(a1: A), a2: A), a3: A))(fu)

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3)] =
      // SO UGLY UGLY UGLY workaround for unchecked type erasure warning... no cleaner way found...
      (v: Any) match {
        case fu: Functor[_] => apply { (a1: A1, a2: A2, a3: A3) => (a1, a2, a3) }(fu.asInstanceOf[Functor[M]])
        case fu: ContravariantFunctor[_] => apply[(A1, A2, A3)] { (a: (A1, A2, A3)) => (a._1, a._2, a._3) }(fu.asInstanceOf[ContravariantFunctor[M]])
        case fu: InvariantFunctor[_] => apply[(A1, A2, A3)]({ (a1: A1, a2: A2, a3: A3) => (a1, a2, a3) }, { (a: (A1, A2, A3)) => (a._1, a._2, a._3) })(fu.asInstanceOf[InvariantFunctor[M]])
      }

  }

  class CanBuild4[A1, A2, A3, A4](m1: M[A1 ~ A2 ~ A3], m2: M[A4]) {

    def ~[A5](m3: M[A5]) = new CanBuild5[A1, A2, A3, A4, A5](canBuild(m1, m2), m3)

    def and[A5](m3: M[A5]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4) => B)(implicit fu: Functor[M]): M[B] =
      fu.fmap[A1 ~ A2 ~ A3 ~ A4, B](canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 => f(a1, a2, a3, a4) })

    def apply[B](f: B => (A1, A2, A3, A4))(implicit fu: ContravariantFunctor[M]): M[B] =
      fu.contramap(canBuild(m1, m2), (b: B) => { val (a1, a2, a3, a4) = f(b); new ~(new ~(new ~(a1, a2), a3), a4) })

    def apply[B](f1: (A1, A2, A3, A4) => B, f2: B => (A1, A2, A3, A4))(implicit fu: InvariantFunctor[M]): M[B] =
      fu.inmap[A1 ~ A2 ~ A3 ~ A4, B](
        canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 => f1(a1, a2, a3, a4) },
        (b: B) => { val (a1, a2, a3, a4) = f2(b); new ~(new ~(new ~(a1, a2), a3), a4) }
      )

    def join[A >: A1](implicit witness1: <:<[A, A1], witness2: <:<[A, A2], witness3: <:<[A, A3], witness4: <:<[A, A4], fu: ContravariantFunctor[M]): M[A] =
      apply[A]((a: A) => (a: A1, a: A2, a: A3, a: A4))(fu)

    def reduce[A >: A1, B](implicit witness1: <:<[A1, A], witness2: <:<[A2, A], witness3: <:<[A3, A], witness4: <:<[A4, A], fu: Functor[M], reducer: Reducer[A, B]): M[B] =
      apply[B]((a1: A1, a2: A2, a3: A3, a4: A4) => reducer.append(reducer.append(reducer.append(reducer.unit(a1: A), a2: A), a3: A), a4: A))(fu)

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4)] =
      // SO UGLY UGLY UGLY workaround for unchecked type erasure warning... no cleaner way found...
      (v: Any) match {
        case fu: Functor[_] => apply { (a1: A1, a2: A2, a3: A3, a4: A4) => (a1, a2, a3, a4) }(fu.asInstanceOf[Functor[M]])
        case fu: ContravariantFunctor[_] => apply[(A1, A2, A3, A4)] { (a: (A1, A2, A3, A4)) => (a._1, a._2, a._3, a._4) }(fu.asInstanceOf[ContravariantFunctor[M]])
        case fu: InvariantFunctor[_] => apply[(A1, A2, A3, A4)]({ (a1: A1, a2: A2, a3: A3, a4: A4) => (a1, a2, a3, a4) }, { (a: (A1, A2, A3, A4)) => (a._1, a._2, a._3, a._4) })(fu.asInstanceOf[InvariantFunctor[M]])
      }

  }

  class CanBuild5[A1, A2, A3, A4, A5](m1: M[A1 ~ A2 ~ A3 ~ A4], m2: M[A5]) {

    def ~[A6](m3: M[A6]) = new CanBuild6[A1, A2, A3, A4, A5, A6](canBuild(m1, m2), m3)

    def and[A6](m3: M[A6]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5) => B)(implicit fu: Functor[M]): M[B] =
      fu.fmap[A1 ~ A2 ~ A3 ~ A4 ~ A5, B](canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 => f(a1, a2, a3, a4, a5) })

    def apply[B](f: B => (A1, A2, A3, A4, A5))(implicit fu: ContravariantFunctor[M]): M[B] =
      fu.contramap(canBuild(m1, m2), (b: B) => { val (a1, a2, a3, a4, a5) = f(b); new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5) })

    def apply[B](f1: (A1, A2, A3, A4, A5) => B, f2: B => (A1, A2, A3, A4, A5))(implicit fu: InvariantFunctor[M]): M[B] =
      fu.inmap[A1 ~ A2 ~ A3 ~ A4 ~ A5, B](
        canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 => f1(a1, a2, a3, a4, a5) },
        (b: B) => { val (a1, a2, a3, a4, a5) = f2(b); new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5) }
      )

    def join[A >: A1](implicit witness1: <:<[A, A1], witness2: <:<[A, A2], witness3: <:<[A, A3], witness4: <:<[A, A4], witness5: <:<[A, A5], fu: ContravariantFunctor[M]): M[A] =
      apply[A]((a: A) => (a: A1, a: A2, a: A3, a: A4, a: A5))(fu)

    def reduce[A >: A1, B](implicit witness1: <:<[A1, A], witness2: <:<[A2, A], witness3: <:<[A3, A], witness4: <:<[A4, A], witness5: <:<[A5, A], fu: Functor[M], reducer: Reducer[A, B]): M[B] =
      apply[B]((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5) => reducer.append(reducer.append(reducer.append(reducer.append(reducer.unit(a1: A), a2: A), a3: A), a4: A), a5: A))(fu)

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5)] =
      // SO UGLY UGLY UGLY workaround for unchecked type erasure warning... no cleaner way found...
      (v: Any) match {
        case fu: Functor[_] => apply { (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5) => (a1, a2, a3, a4, a5) }(fu.asInstanceOf[Functor[M]])
        case fu: ContravariantFunctor[_] => apply[(A1, A2, A3, A4, A5)] { (a: (A1, A2, A3, A4, A5)) => (a._1, a._2, a._3, a._4, a._5) }(fu.asInstanceOf[ContravariantFunctor[M]])
        case fu: InvariantFunctor[_] => apply[(A1, A2, A3, A4, A5)]({ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5) => (a1, a2, a3, a4, a5) }, { (a: (A1, A2, A3, A4, A5)) => (a._1, a._2, a._3, a._4, a._5) })(fu.asInstanceOf[InvariantFunctor[M]])
      }

  }

  class CanBuild6[A1, A2, A3, A4, A5, A6](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5], m2: M[A6]) {

    def ~[A7](m3: M[A7]) = new CanBuild7[A1, A2, A3, A4, A5, A6, A7](canBuild(m1, m2), m3)

    def and[A7](m3: M[A7]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5, A6) => B)(implicit fu: Functor[M]): M[B] =
      fu.fmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6, B](canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 => f(a1, a2, a3, a4, a5, a6) })

    def apply[B](f: B => (A1, A2, A3, A4, A5, A6))(implicit fu: ContravariantFunctor[M]): M[B] =
      fu.contramap(canBuild(m1, m2), (b: B) => { val (a1, a2, a3, a4, a5, a6) = f(b); new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6) })

    def apply[B](f1: (A1, A2, A3, A4, A5, A6) => B, f2: B => (A1, A2, A3, A4, A5, A6))(implicit fu: InvariantFunctor[M]): M[B] =
      fu.inmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6, B](
        canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 => f1(a1, a2, a3, a4, a5, a6) },
        (b: B) => { val (a1, a2, a3, a4, a5, a6) = f2(b); new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6) }
      )

    def join[A >: A1](implicit witness1: <:<[A, A1], witness2: <:<[A, A2], witness3: <:<[A, A3], witness4: <:<[A, A4], witness5: <:<[A, A5], witness6: <:<[A, A6], fu: ContravariantFunctor[M]): M[A] =
      apply[A]((a: A) => (a: A1, a: A2, a: A3, a: A4, a: A5, a: A6))(fu)

    def reduce[A >: A1, B](implicit witness1: <:<[A1, A], witness2: <:<[A2, A], witness3: <:<[A3, A], witness4: <:<[A4, A], witness5: <:<[A5, A], witness6: <:<[A6, A], fu: Functor[M], reducer: Reducer[A, B]): M[B] =
      apply[B]((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6) => reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.unit(a1: A), a2: A), a3: A), a4: A), a5: A), a6: A))(fu)

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5, A6)] =
      // SO UGLY UGLY UGLY workaround for unchecked type erasure warning... no cleaner way found...
      (v: Any) match {
        case fu: Functor[_] => apply { (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6) => (a1, a2, a3, a4, a5, a6) }(fu.asInstanceOf[Functor[M]])
        case fu: ContravariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6)] { (a: (A1, A2, A3, A4, A5, A6)) => (a._1, a._2, a._3, a._4, a._5, a._6) }(fu.asInstanceOf[ContravariantFunctor[M]])
        case fu: InvariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6)]({ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6) => (a1, a2, a3, a4, a5, a6) }, { (a: (A1, A2, A3, A4, A5, A6)) => (a._1, a._2, a._3, a._4, a._5, a._6) })(fu.asInstanceOf[InvariantFunctor[M]])
      }

  }

  class CanBuild7[A1, A2, A3, A4, A5, A6, A7](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6], m2: M[A7]) {

    def ~[A8](m3: M[A8]) = new CanBuild8[A1, A2, A3, A4, A5, A6, A7, A8](canBuild(m1, m2), m3)

    def and[A8](m3: M[A8]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5, A6, A7) => B)(implicit fu: Functor[M]): M[B] =
      fu.fmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7, B](canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 => f(a1, a2, a3, a4, a5, a6, a7) })

    def apply[B](f: B => (A1, A2, A3, A4, A5, A6, A7))(implicit fu: ContravariantFunctor[M]): M[B] =
      fu.contramap(canBuild(m1, m2), (b: B) => { val (a1, a2, a3, a4, a5, a6, a7) = f(b); new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7) })

    def apply[B](f1: (A1, A2, A3, A4, A5, A6, A7) => B, f2: B => (A1, A2, A3, A4, A5, A6, A7))(implicit fu: InvariantFunctor[M]): M[B] =
      fu.inmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7, B](
        canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 => f1(a1, a2, a3, a4, a5, a6, a7) },
        (b: B) => { val (a1, a2, a3, a4, a5, a6, a7) = f2(b); new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7) }
      )

    def join[A >: A1](implicit witness1: <:<[A, A1], witness2: <:<[A, A2], witness3: <:<[A, A3], witness4: <:<[A, A4], witness5: <:<[A, A5], witness6: <:<[A, A6], witness7: <:<[A, A7], fu: ContravariantFunctor[M]): M[A] =
      apply[A]((a: A) => (a: A1, a: A2, a: A3, a: A4, a: A5, a: A6, a: A7))(fu)

    def reduce[A >: A1, B](implicit witness1: <:<[A1, A], witness2: <:<[A2, A], witness3: <:<[A3, A], witness4: <:<[A4, A], witness5: <:<[A5, A], witness6: <:<[A6, A], witness7: <:<[A7, A], fu: Functor[M], reducer: Reducer[A, B]): M[B] =
      apply[B]((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7) => reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.unit(a1: A), a2: A), a3: A), a4: A), a5: A), a6: A), a7: A))(fu)

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5, A6, A7)] =
      // SO UGLY UGLY UGLY workaround for unchecked type erasure warning... no cleaner way found...
      (v: Any) match {
        case fu: Functor[_] => apply { (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7) => (a1, a2, a3, a4, a5, a6, a7) }(fu.asInstanceOf[Functor[M]])
        case fu: ContravariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7)] { (a: (A1, A2, A3, A4, A5, A6, A7)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7) }(fu.asInstanceOf[ContravariantFunctor[M]])
        case fu: InvariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7)]({ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7) => (a1, a2, a3, a4, a5, a6, a7) }, { (a: (A1, A2, A3, A4, A5, A6, A7)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7) })(fu.asInstanceOf[InvariantFunctor[M]])
      }

  }

  class CanBuild8[A1, A2, A3, A4, A5, A6, A7, A8](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7], m2: M[A8]) {

    def ~[A9](m3: M[A9]) = new CanBuild9[A1, A2, A3, A4, A5, A6, A7, A8, A9](canBuild(m1, m2), m3)

    def and[A9](m3: M[A9]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5, A6, A7, A8) => B)(implicit fu: Functor[M]): M[B] =
      fu.fmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8, B](canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 => f(a1, a2, a3, a4, a5, a6, a7, a8) })

    def apply[B](f: B => (A1, A2, A3, A4, A5, A6, A7, A8))(implicit fu: ContravariantFunctor[M]): M[B] =
      fu.contramap(canBuild(m1, m2), (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8) = f(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8) })

    def apply[B](f1: (A1, A2, A3, A4, A5, A6, A7, A8) => B, f2: B => (A1, A2, A3, A4, A5, A6, A7, A8))(implicit fu: InvariantFunctor[M]): M[B] =
      fu.inmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8, B](
        canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 => f1(a1, a2, a3, a4, a5, a6, a7, a8) },
        (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8) = f2(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8) }
      )

    def join[A >: A1](implicit witness1: <:<[A, A1], witness2: <:<[A, A2], witness3: <:<[A, A3], witness4: <:<[A, A4], witness5: <:<[A, A5], witness6: <:<[A, A6], witness7: <:<[A, A7], witness8: <:<[A, A8], fu: ContravariantFunctor[M]): M[A] =
      apply[A]((a: A) => (a: A1, a: A2, a: A3, a: A4, a: A5, a: A6, a: A7, a: A8))(fu)

    def reduce[A >: A1, B](implicit witness1: <:<[A1, A], witness2: <:<[A2, A], witness3: <:<[A3, A], witness4: <:<[A4, A], witness5: <:<[A5, A], witness6: <:<[A6, A], witness7: <:<[A7, A], witness8: <:<[A8, A], fu: Functor[M], reducer: Reducer[A, B]): M[B] =
      apply[B]((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8) => reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.unit(a1: A), a2: A), a3: A), a4: A), a5: A), a6: A), a7: A), a8: A))(fu)

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5, A6, A7, A8)] =
      // SO UGLY UGLY UGLY workaround for unchecked type erasure warning... no cleaner way found...
      (v: Any) match {
        case fu: Functor[_] => apply { (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8) => (a1, a2, a3, a4, a5, a6, a7, a8) }(fu.asInstanceOf[Functor[M]])
        case fu: ContravariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8)] { (a: (A1, A2, A3, A4, A5, A6, A7, A8)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8) }(fu.asInstanceOf[ContravariantFunctor[M]])
        case fu: InvariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8)]({ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8) => (a1, a2, a3, a4, a5, a6, a7, a8) }, { (a: (A1, A2, A3, A4, A5, A6, A7, A8)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8) })(fu.asInstanceOf[InvariantFunctor[M]])
      }

  }

  class CanBuild9[A1, A2, A3, A4, A5, A6, A7, A8, A9](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8], m2: M[A9]) {

    def ~[A10](m3: M[A10]) = new CanBuild10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10](canBuild(m1, m2), m3)

    def and[A10](m3: M[A10]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5, A6, A7, A8, A9) => B)(implicit fu: Functor[M]): M[B] =
      fu.fmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9, B](canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 => f(a1, a2, a3, a4, a5, a6, a7, a8, a9) })

    def apply[B](f: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9))(implicit fu: ContravariantFunctor[M]): M[B] =
      fu.contramap(canBuild(m1, m2), (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9) = f(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9) })

    def apply[B](f1: (A1, A2, A3, A4, A5, A6, A7, A8, A9) => B, f2: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9))(implicit fu: InvariantFunctor[M]): M[B] =
      fu.inmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9, B](
        canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 => f1(a1, a2, a3, a4, a5, a6, a7, a8, a9) },
        (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9) = f2(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9) }
      )

    def join[A >: A1](implicit witness1: <:<[A, A1], witness2: <:<[A, A2], witness3: <:<[A, A3], witness4: <:<[A, A4], witness5: <:<[A, A5], witness6: <:<[A, A6], witness7: <:<[A, A7], witness8: <:<[A, A8], witness9: <:<[A, A9], fu: ContravariantFunctor[M]): M[A] =
      apply[A]((a: A) => (a: A1, a: A2, a: A3, a: A4, a: A5, a: A6, a: A7, a: A8, a: A9))(fu)

    def reduce[A >: A1, B](implicit witness1: <:<[A1, A], witness2: <:<[A2, A], witness3: <:<[A3, A], witness4: <:<[A4, A], witness5: <:<[A5, A], witness6: <:<[A6, A], witness7: <:<[A7, A], witness8: <:<[A8, A], witness9: <:<[A9, A], fu: Functor[M], reducer: Reducer[A, B]): M[B] =
      apply[B]((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9) => reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.unit(a1: A), a2: A), a3: A), a4: A), a5: A), a6: A), a7: A), a8: A), a9: A))(fu)

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5, A6, A7, A8, A9)] =
      // SO UGLY UGLY UGLY workaround for unchecked type erasure warning... no cleaner way found...
      (v: Any) match {
        case fu: Functor[_] => apply { (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9) => (a1, a2, a3, a4, a5, a6, a7, a8, a9) }(fu.asInstanceOf[Functor[M]])
        case fu: ContravariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9)] { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9) }(fu.asInstanceOf[ContravariantFunctor[M]])
        case fu: InvariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9)]({ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9) => (a1, a2, a3, a4, a5, a6, a7, a8, a9) }, { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9) })(fu.asInstanceOf[InvariantFunctor[M]])
      }

  }

  class CanBuild10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9], m2: M[A10]) {

    def ~[A11](m3: M[A11]) = new CanBuild11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11](canBuild(m1, m2), m3)

    def and[A11](m3: M[A11]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10) => B)(implicit fu: Functor[M]): M[B] =
      fu.fmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10, B](canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 => f(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) })

    def apply[B](f: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10))(implicit fu: ContravariantFunctor[M]): M[B] =
      fu.contramap(canBuild(m1, m2), (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) = f(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10) })

    def apply[B](f1: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10) => B, f2: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10))(implicit fu: InvariantFunctor[M]): M[B] =
      fu.inmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10, B](
        canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 => f1(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) },
        (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) = f2(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10) }
      )

    def join[A >: A1](implicit witness1: <:<[A, A1], witness2: <:<[A, A2], witness3: <:<[A, A3], witness4: <:<[A, A4], witness5: <:<[A, A5], witness6: <:<[A, A6], witness7: <:<[A, A7], witness8: <:<[A, A8], witness9: <:<[A, A9], witness10: <:<[A, A10], fu: ContravariantFunctor[M]): M[A] =
      apply[A]((a: A) => (a: A1, a: A2, a: A3, a: A4, a: A5, a: A6, a: A7, a: A8, a: A9, a: A10))(fu)

    def reduce[A >: A1, B](implicit witness1: <:<[A1, A], witness2: <:<[A2, A], witness3: <:<[A3, A], witness4: <:<[A4, A], witness5: <:<[A5, A], witness6: <:<[A6, A], witness7: <:<[A7, A], witness8: <:<[A8, A], witness9: <:<[A9, A], witness10: <:<[A10, A], fu: Functor[M], reducer: Reducer[A, B]): M[B] =
      apply[B]((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10) => reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.unit(a1: A), a2: A), a3: A), a4: A), a5: A), a6: A), a7: A), a8: A), a9: A), a10: A))(fu)

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10)] =
      // SO UGLY UGLY UGLY workaround for unchecked type erasure warning... no cleaner way found...
      (v: Any) match {
        case fu: Functor[_] => apply { (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) }(fu.asInstanceOf[Functor[M]])
        case fu: ContravariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10)] { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10) }(fu.asInstanceOf[ContravariantFunctor[M]])
        case fu: InvariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10)]({ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) }, { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10) })(fu.asInstanceOf[InvariantFunctor[M]])
      }

  }

  class CanBuild11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10], m2: M[A11]) {

    def ~[A12](m3: M[A12]) = new CanBuild12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12](canBuild(m1, m2), m3)

    def and[A12](m3: M[A12]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11) => B)(implicit fu: Functor[M]): M[B] =
      fu.fmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11, B](canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 ~ a11 => f(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11) })

    def apply[B](f: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11))(implicit fu: ContravariantFunctor[M]): M[B] =
      fu.contramap(canBuild(m1, m2), (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11) = f(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10), a11) })

    def apply[B](f1: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11) => B, f2: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11))(implicit fu: InvariantFunctor[M]): M[B] =
      fu.inmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11, B](
        canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 ~ a11 => f1(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11) },
        (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11) = f2(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10), a11) }
      )

    def join[A >: A1](implicit witness1: <:<[A, A1], witness2: <:<[A, A2], witness3: <:<[A, A3], witness4: <:<[A, A4], witness5: <:<[A, A5], witness6: <:<[A, A6], witness7: <:<[A, A7], witness8: <:<[A, A8], witness9: <:<[A, A9], witness10: <:<[A, A10], witness11: <:<[A, A11], fu: ContravariantFunctor[M]): M[A] =
      apply[A]((a: A) => (a: A1, a: A2, a: A3, a: A4, a: A5, a: A6, a: A7, a: A8, a: A9, a: A10, a: A11))(fu)

    def reduce[A >: A1, B](implicit witness1: <:<[A1, A], witness2: <:<[A2, A], witness3: <:<[A3, A], witness4: <:<[A4, A], witness5: <:<[A5, A], witness6: <:<[A6, A], witness7: <:<[A7, A], witness8: <:<[A8, A], witness9: <:<[A9, A], witness10: <:<[A10, A], witness11: <:<[A11, A], fu: Functor[M], reducer: Reducer[A, B]): M[B] =
      apply[B]((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11) => reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.unit(a1: A), a2: A), a3: A), a4: A), a5: A), a6: A), a7: A), a8: A), a9: A), a10: A), a11: A))(fu)

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11)] =
      // SO UGLY UGLY UGLY workaround for unchecked type erasure warning... no cleaner way found...
      (v: Any) match {
        case fu: Functor[_] => apply { (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11) }(fu.asInstanceOf[Functor[M]])
        case fu: ContravariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11)] { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11) }(fu.asInstanceOf[ContravariantFunctor[M]])
        case fu: InvariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11)]({ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11) }, { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11) })(fu.asInstanceOf[InvariantFunctor[M]])
      }

  }

  class CanBuild12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11], m2: M[A12]) {

    def ~[A13](m3: M[A13]) = new CanBuild13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13](canBuild(m1, m2), m3)

    def and[A13](m3: M[A13]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12) => B)(implicit fu: Functor[M]): M[B] =
      fu.fmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12, B](canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 ~ a11 ~ a12 => f(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12) })

    def apply[B](f: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12))(implicit fu: ContravariantFunctor[M]): M[B] =
      fu.contramap(canBuild(m1, m2), (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12) = f(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10), a11), a12) })

    def apply[B](f1: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12) => B, f2: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12))(implicit fu: InvariantFunctor[M]): M[B] =
      fu.inmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12, B](
        canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 ~ a11 ~ a12 => f1(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12) },
        (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12) = f2(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10), a11), a12) }
      )

    def join[A >: A1](implicit witness1: <:<[A, A1], witness2: <:<[A, A2], witness3: <:<[A, A3], witness4: <:<[A, A4], witness5: <:<[A, A5], witness6: <:<[A, A6], witness7: <:<[A, A7], witness8: <:<[A, A8], witness9: <:<[A, A9], witness10: <:<[A, A10], witness11: <:<[A, A11], witness12: <:<[A, A12], fu: ContravariantFunctor[M]): M[A] =
      apply[A]((a: A) => (a: A1, a: A2, a: A3, a: A4, a: A5, a: A6, a: A7, a: A8, a: A9, a: A10, a: A11, a: A12))(fu)

    def reduce[A >: A1, B](implicit witness1: <:<[A1, A], witness2: <:<[A2, A], witness3: <:<[A3, A], witness4: <:<[A4, A], witness5: <:<[A5, A], witness6: <:<[A6, A], witness7: <:<[A7, A], witness8: <:<[A8, A], witness9: <:<[A9, A], witness10: <:<[A10, A], witness11: <:<[A11, A], witness12: <:<[A12, A], fu: Functor[M], reducer: Reducer[A, B]): M[B] =
      apply[B]((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12) => reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.unit(a1: A), a2: A), a3: A), a4: A), a5: A), a6: A), a7: A), a8: A), a9: A), a10: A), a11: A), a12: A))(fu)

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12)] =
      // SO UGLY UGLY UGLY workaround for unchecked type erasure warning... no cleaner way found...
      (v: Any) match {
        case fu: Functor[_] => apply { (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12) }(fu.asInstanceOf[Functor[M]])
        case fu: ContravariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12)] { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11, a._12) }(fu.asInstanceOf[ContravariantFunctor[M]])
        case fu: InvariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12)]({ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12) }, { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11, a._12) })(fu.asInstanceOf[InvariantFunctor[M]])
      }

  }

  class CanBuild13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12], m2: M[A13]) {

    def ~[A14](m3: M[A14]) = new CanBuild14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14](canBuild(m1, m2), m3)

    def and[A14](m3: M[A14]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13) => B)(implicit fu: Functor[M]): M[B] =
      fu.fmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13, B](canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 ~ a11 ~ a12 ~ a13 => f(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13) })

    def apply[B](f: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13))(implicit fu: ContravariantFunctor[M]): M[B] =
      fu.contramap(canBuild(m1, m2), (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13) = f(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10), a11), a12), a13) })

    def apply[B](f1: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13) => B, f2: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13))(implicit fu: InvariantFunctor[M]): M[B] =
      fu.inmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13, B](
        canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 ~ a11 ~ a12 ~ a13 => f1(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13) },
        (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13) = f2(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10), a11), a12), a13) }
      )

    def join[A >: A1](implicit witness1: <:<[A, A1], witness2: <:<[A, A2], witness3: <:<[A, A3], witness4: <:<[A, A4], witness5: <:<[A, A5], witness6: <:<[A, A6], witness7: <:<[A, A7], witness8: <:<[A, A8], witness9: <:<[A, A9], witness10: <:<[A, A10], witness11: <:<[A, A11], witness12: <:<[A, A12], witness13: <:<[A, A13], fu: ContravariantFunctor[M]): M[A] =
      apply[A]((a: A) => (a: A1, a: A2, a: A3, a: A4, a: A5, a: A6, a: A7, a: A8, a: A9, a: A10, a: A11, a: A12, a: A13))(fu)

    def reduce[A >: A1, B](implicit witness1: <:<[A1, A], witness2: <:<[A2, A], witness3: <:<[A3, A], witness4: <:<[A4, A], witness5: <:<[A5, A], witness6: <:<[A6, A], witness7: <:<[A7, A], witness8: <:<[A8, A], witness9: <:<[A9, A], witness10: <:<[A10, A], witness11: <:<[A11, A], witness12: <:<[A12, A], witness13: <:<[A13, A], fu: Functor[M], reducer: Reducer[A, B]): M[B] =
      apply[B]((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13) => reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.unit(a1: A), a2: A), a3: A), a4: A), a5: A), a6: A), a7: A), a8: A), a9: A), a10: A), a11: A), a12: A), a13: A))(fu)

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13)] =
      // SO UGLY UGLY UGLY workaround for unchecked type erasure warning... no cleaner way found...
      (v: Any) match {
        case fu: Functor[_] => apply { (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13) }(fu.asInstanceOf[Functor[M]])
        case fu: ContravariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13)] { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11, a._12, a._13) }(fu.asInstanceOf[ContravariantFunctor[M]])
        case fu: InvariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13)]({ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13) }, { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11, a._12, a._13) })(fu.asInstanceOf[InvariantFunctor[M]])
      }

  }

  class CanBuild14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13], m2: M[A14]) {

    def ~[A15](m3: M[A15]) = new CanBuild15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15](canBuild(m1, m2), m3)

    def and[A15](m3: M[A15]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14) => B)(implicit fu: Functor[M]): M[B] =
      fu.fmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13 ~ A14, B](canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 ~ a11 ~ a12 ~ a13 ~ a14 => f(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14) })

    def apply[B](f: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14))(implicit fu: ContravariantFunctor[M]): M[B] =
      fu.contramap(canBuild(m1, m2), (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14) = f(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10), a11), a12), a13), a14) })

    def apply[B](f1: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14) => B, f2: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14))(implicit fu: InvariantFunctor[M]): M[B] =
      fu.inmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13 ~ A14, B](
        canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 ~ a11 ~ a12 ~ a13 ~ a14 => f1(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14) },
        (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14) = f2(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10), a11), a12), a13), a14) }
      )

    def join[A >: A1](implicit witness1: <:<[A, A1], witness2: <:<[A, A2], witness3: <:<[A, A3], witness4: <:<[A, A4], witness5: <:<[A, A5], witness6: <:<[A, A6], witness7: <:<[A, A7], witness8: <:<[A, A8], witness9: <:<[A, A9], witness10: <:<[A, A10], witness11: <:<[A, A11], witness12: <:<[A, A12], witness13: <:<[A, A13], witness14: <:<[A, A14], fu: ContravariantFunctor[M]): M[A] =
      apply[A]((a: A) => (a: A1, a: A2, a: A3, a: A4, a: A5, a: A6, a: A7, a: A8, a: A9, a: A10, a: A11, a: A12, a: A13, a: A14))(fu)

    def reduce[A >: A1, B](implicit witness1: <:<[A1, A], witness2: <:<[A2, A], witness3: <:<[A3, A], witness4: <:<[A4, A], witness5: <:<[A5, A], witness6: <:<[A6, A], witness7: <:<[A7, A], witness8: <:<[A8, A], witness9: <:<[A9, A], witness10: <:<[A10, A], witness11: <:<[A11, A], witness12: <:<[A12, A], witness13: <:<[A13, A], witness14: <:<[A14, A], fu: Functor[M], reducer: Reducer[A, B]): M[B] =
      apply[B]((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14) => reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.unit(a1: A), a2: A), a3: A), a4: A), a5: A), a6: A), a7: A), a8: A), a9: A), a10: A), a11: A), a12: A), a13: A), a14: A))(fu)

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14)] =
      // SO UGLY UGLY UGLY workaround for unchecked type erasure warning... no cleaner way found...
      (v: Any) match {
        case fu: Functor[_] => apply { (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14) }(fu.asInstanceOf[Functor[M]])
        case fu: ContravariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14)] { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11, a._12, a._13, a._14) }(fu.asInstanceOf[ContravariantFunctor[M]])
        case fu: InvariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14)]({ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14) }, { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11, a._12, a._13, a._14) })(fu.asInstanceOf[InvariantFunctor[M]])
      }

  }

  class CanBuild15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13 ~ A14], m2: M[A15]) {

    def ~[A16](m3: M[A16]) = new CanBuild16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16](canBuild(m1, m2), m3)

    def and[A16](m3: M[A16]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15) => B)(implicit fu: Functor[M]): M[B] =
      fu.fmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13 ~ A14 ~ A15, B](canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 ~ a11 ~ a12 ~ a13 ~ a14 ~ a15 => f(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15) })

    def apply[B](f: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15))(implicit fu: ContravariantFunctor[M]): M[B] =
      fu.contramap(canBuild(m1, m2), (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15) = f(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10), a11), a12), a13), a14), a15) })

    def apply[B](f1: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15) => B, f2: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15))(implicit fu: InvariantFunctor[M]): M[B] =
      fu.inmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13 ~ A14 ~ A15, B](
        canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 ~ a11 ~ a12 ~ a13 ~ a14 ~ a15 => f1(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15) },
        (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15) = f2(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10), a11), a12), a13), a14), a15) }
      )

    def join[A >: A1](implicit witness1: <:<[A, A1], witness2: <:<[A, A2], witness3: <:<[A, A3], witness4: <:<[A, A4], witness5: <:<[A, A5], witness6: <:<[A, A6], witness7: <:<[A, A7], witness8: <:<[A, A8], witness9: <:<[A, A9], witness10: <:<[A, A10], witness11: <:<[A, A11], witness12: <:<[A, A12], witness13: <:<[A, A13], witness14: <:<[A, A14], witness15: <:<[A, A15], fu: ContravariantFunctor[M]): M[A] =
      apply[A]((a: A) => (a: A1, a: A2, a: A3, a: A4, a: A5, a: A6, a: A7, a: A8, a: A9, a: A10, a: A11, a: A12, a: A13, a: A14, a: A15))(fu)

    def reduce[A >: A1, B](implicit witness1: <:<[A1, A], witness2: <:<[A2, A], witness3: <:<[A3, A], witness4: <:<[A4, A], witness5: <:<[A5, A], witness6: <:<[A6, A], witness7: <:<[A7, A], witness8: <:<[A8, A], witness9: <:<[A9, A], witness10: <:<[A10, A], witness11: <:<[A11, A], witness12: <:<[A12, A], witness13: <:<[A13, A], witness14: <:<[A14, A], witness15: <:<[A15, A], fu: Functor[M], reducer: Reducer[A, B]): M[B] =
      apply[B]((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15) => reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.unit(a1: A), a2: A), a3: A), a4: A), a5: A), a6: A), a7: A), a8: A), a9: A), a10: A), a11: A), a12: A), a13: A), a14: A), a15: A))(fu)

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15)] =
      // SO UGLY UGLY UGLY workaround for unchecked type erasure warning... no cleaner way found...
      (v: Any) match {
        case fu: Functor[_] => apply { (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15) }(fu.asInstanceOf[Functor[M]])
        case fu: ContravariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15)] { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11, a._12, a._13, a._14, a._15) }(fu.asInstanceOf[ContravariantFunctor[M]])
        case fu: InvariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15)]({ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15) }, { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11, a._12, a._13, a._14, a._15) })(fu.asInstanceOf[InvariantFunctor[M]])
      }

  }

  class CanBuild16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13 ~ A14 ~ A15], m2: M[A16]) {

    def ~[A17](m3: M[A17]) = new CanBuild17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17](canBuild(m1, m2), m3)

    def and[A17](m3: M[A17]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16) => B)(implicit fu: Functor[M]): M[B] =
      fu.fmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13 ~ A14 ~ A15 ~ A16, B](canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 ~ a11 ~ a12 ~ a13 ~ a14 ~ a15 ~ a16 => f(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16) })

    def apply[B](f: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16))(implicit fu: ContravariantFunctor[M]): M[B] =
      fu.contramap(canBuild(m1, m2), (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16) = f(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10), a11), a12), a13), a14), a15), a16) })

    def apply[B](f1: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16) => B, f2: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16))(implicit fu: InvariantFunctor[M]): M[B] =
      fu.inmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13 ~ A14 ~ A15 ~ A16, B](
        canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 ~ a11 ~ a12 ~ a13 ~ a14 ~ a15 ~ a16 => f1(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16) },
        (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16) = f2(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10), a11), a12), a13), a14), a15), a16) }
      )

    def join[A >: A1](implicit witness1: <:<[A, A1], witness2: <:<[A, A2], witness3: <:<[A, A3], witness4: <:<[A, A4], witness5: <:<[A, A5], witness6: <:<[A, A6], witness7: <:<[A, A7], witness8: <:<[A, A8], witness9: <:<[A, A9], witness10: <:<[A, A10], witness11: <:<[A, A11], witness12: <:<[A, A12], witness13: <:<[A, A13], witness14: <:<[A, A14], witness15: <:<[A, A15], witness16: <:<[A, A16], fu: ContravariantFunctor[M]): M[A] =
      apply[A]((a: A) => (a: A1, a: A2, a: A3, a: A4, a: A5, a: A6, a: A7, a: A8, a: A9, a: A10, a: A11, a: A12, a: A13, a: A14, a: A15, a: A16))(fu)

    def reduce[A >: A1, B](implicit witness1: <:<[A1, A], witness2: <:<[A2, A], witness3: <:<[A3, A], witness4: <:<[A4, A], witness5: <:<[A5, A], witness6: <:<[A6, A], witness7: <:<[A7, A], witness8: <:<[A8, A], witness9: <:<[A9, A], witness10: <:<[A10, A], witness11: <:<[A11, A], witness12: <:<[A12, A], witness13: <:<[A13, A], witness14: <:<[A14, A], witness15: <:<[A15, A], witness16: <:<[A16, A], fu: Functor[M], reducer: Reducer[A, B]): M[B] =
      apply[B]((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16) => reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.unit(a1: A), a2: A), a3: A), a4: A), a5: A), a6: A), a7: A), a8: A), a9: A), a10: A), a11: A), a12: A), a13: A), a14: A), a15: A), a16: A))(fu)

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16)] =
      // SO UGLY UGLY UGLY workaround for unchecked type erasure warning... no cleaner way found...
      (v: Any) match {
        case fu: Functor[_] => apply { (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16) }(fu.asInstanceOf[Functor[M]])
        case fu: ContravariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16)] { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11, a._12, a._13, a._14, a._15, a._16) }(fu.asInstanceOf[ContravariantFunctor[M]])
        case fu: InvariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16)]({ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16) }, { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11, a._12, a._13, a._14, a._15, a._16) })(fu.asInstanceOf[InvariantFunctor[M]])
      }

  }

  class CanBuild17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13 ~ A14 ~ A15 ~ A16], m2: M[A17]) {

    def ~[A18](m3: M[A18]) = new CanBuild18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18](canBuild(m1, m2), m3)

    def and[A18](m3: M[A18]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17) => B)(implicit fu: Functor[M]): M[B] =
      fu.fmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13 ~ A14 ~ A15 ~ A16 ~ A17, B](canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 ~ a11 ~ a12 ~ a13 ~ a14 ~ a15 ~ a16 ~ a17 => f(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17) })

    def apply[B](f: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17))(implicit fu: ContravariantFunctor[M]): M[B] =
      fu.contramap(canBuild(m1, m2), (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17) = f(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10), a11), a12), a13), a14), a15), a16), a17) })

    def apply[B](f1: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17) => B, f2: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17))(implicit fu: InvariantFunctor[M]): M[B] =
      fu.inmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13 ~ A14 ~ A15 ~ A16 ~ A17, B](
        canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 ~ a11 ~ a12 ~ a13 ~ a14 ~ a15 ~ a16 ~ a17 => f1(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17) },
        (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17) = f2(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10), a11), a12), a13), a14), a15), a16), a17) }
      )

    def join[A >: A1](implicit witness1: <:<[A, A1], witness2: <:<[A, A2], witness3: <:<[A, A3], witness4: <:<[A, A4], witness5: <:<[A, A5], witness6: <:<[A, A6], witness7: <:<[A, A7], witness8: <:<[A, A8], witness9: <:<[A, A9], witness10: <:<[A, A10], witness11: <:<[A, A11], witness12: <:<[A, A12], witness13: <:<[A, A13], witness14: <:<[A, A14], witness15: <:<[A, A15], witness16: <:<[A, A16], witness17: <:<[A, A17], fu: ContravariantFunctor[M]): M[A] =
      apply[A]((a: A) => (a: A1, a: A2, a: A3, a: A4, a: A5, a: A6, a: A7, a: A8, a: A9, a: A10, a: A11, a: A12, a: A13, a: A14, a: A15, a: A16, a: A17))(fu)

    def reduce[A >: A1, B](implicit witness1: <:<[A1, A], witness2: <:<[A2, A], witness3: <:<[A3, A], witness4: <:<[A4, A], witness5: <:<[A5, A], witness6: <:<[A6, A], witness7: <:<[A7, A], witness8: <:<[A8, A], witness9: <:<[A9, A], witness10: <:<[A10, A], witness11: <:<[A11, A], witness12: <:<[A12, A], witness13: <:<[A13, A], witness14: <:<[A14, A], witness15: <:<[A15, A], witness16: <:<[A16, A], witness17: <:<[A17, A], fu: Functor[M], reducer: Reducer[A, B]): M[B] =
      apply[B]((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17) => reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.unit(a1: A), a2: A), a3: A), a4: A), a5: A), a6: A), a7: A), a8: A), a9: A), a10: A), a11: A), a12: A), a13: A), a14: A), a15: A), a16: A), a17: A))(fu)

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17)] =
      // SO UGLY UGLY UGLY workaround for unchecked type erasure warning... no cleaner way found...
      (v: Any) match {
        case fu: Functor[_] => apply { (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17) }(fu.asInstanceOf[Functor[M]])
        case fu: ContravariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17)] { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11, a._12, a._13, a._14, a._15, a._16, a._17) }(fu.asInstanceOf[ContravariantFunctor[M]])
        case fu: InvariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17)]({ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17) }, { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11, a._12, a._13, a._14, a._15, a._16, a._17) })(fu.asInstanceOf[InvariantFunctor[M]])
      }

  }

  class CanBuild18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13 ~ A14 ~ A15 ~ A16 ~ A17], m2: M[A18]) {

    def ~[A19](m3: M[A19]) = new CanBuild19[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19](canBuild(m1, m2), m3)

    def and[A19](m3: M[A19]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18) => B)(implicit fu: Functor[M]): M[B] =
      fu.fmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13 ~ A14 ~ A15 ~ A16 ~ A17 ~ A18, B](canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 ~ a11 ~ a12 ~ a13 ~ a14 ~ a15 ~ a16 ~ a17 ~ a18 => f(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18) })

    def apply[B](f: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18))(implicit fu: ContravariantFunctor[M]): M[B] =
      fu.contramap(canBuild(m1, m2), (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18) = f(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10), a11), a12), a13), a14), a15), a16), a17), a18) })

    def apply[B](f1: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18) => B, f2: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18))(implicit fu: InvariantFunctor[M]): M[B] =
      fu.inmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13 ~ A14 ~ A15 ~ A16 ~ A17 ~ A18, B](
        canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 ~ a11 ~ a12 ~ a13 ~ a14 ~ a15 ~ a16 ~ a17 ~ a18 => f1(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18) },
        (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18) = f2(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10), a11), a12), a13), a14), a15), a16), a17), a18) }
      )

    def join[A >: A1](implicit witness1: <:<[A, A1], witness2: <:<[A, A2], witness3: <:<[A, A3], witness4: <:<[A, A4], witness5: <:<[A, A5], witness6: <:<[A, A6], witness7: <:<[A, A7], witness8: <:<[A, A8], witness9: <:<[A, A9], witness10: <:<[A, A10], witness11: <:<[A, A11], witness12: <:<[A, A12], witness13: <:<[A, A13], witness14: <:<[A, A14], witness15: <:<[A, A15], witness16: <:<[A, A16], witness17: <:<[A, A17], witness18: <:<[A, A18], fu: ContravariantFunctor[M]): M[A] =
      apply[A]((a: A) => (a: A1, a: A2, a: A3, a: A4, a: A5, a: A6, a: A7, a: A8, a: A9, a: A10, a: A11, a: A12, a: A13, a: A14, a: A15, a: A16, a: A17, a: A18))(fu)

    def reduce[A >: A1, B](implicit witness1: <:<[A1, A], witness2: <:<[A2, A], witness3: <:<[A3, A], witness4: <:<[A4, A], witness5: <:<[A5, A], witness6: <:<[A6, A], witness7: <:<[A7, A], witness8: <:<[A8, A], witness9: <:<[A9, A], witness10: <:<[A10, A], witness11: <:<[A11, A], witness12: <:<[A12, A], witness13: <:<[A13, A], witness14: <:<[A14, A], witness15: <:<[A15, A], witness16: <:<[A16, A], witness17: <:<[A17, A], witness18: <:<[A18, A], fu: Functor[M], reducer: Reducer[A, B]): M[B] =
      apply[B]((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18) => reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.unit(a1: A), a2: A), a3: A), a4: A), a5: A), a6: A), a7: A), a8: A), a9: A), a10: A), a11: A), a12: A), a13: A), a14: A), a15: A), a16: A), a17: A), a18: A))(fu)

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18)] =
      // SO UGLY UGLY UGLY workaround for unchecked type erasure warning... no cleaner way found...
      (v: Any) match {
        case fu: Functor[_] => apply { (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18) }(fu.asInstanceOf[Functor[M]])
        case fu: ContravariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18)] { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11, a._12, a._13, a._14, a._15, a._16, a._17, a._18) }(fu.asInstanceOf[ContravariantFunctor[M]])
        case fu: InvariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18)]({ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18) }, { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11, a._12, a._13, a._14, a._15, a._16, a._17, a._18) })(fu.asInstanceOf[InvariantFunctor[M]])
      }

  }

  class CanBuild19[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13 ~ A14 ~ A15 ~ A16 ~ A17 ~ A18], m2: M[A19]) {

    def ~[A20](m3: M[A20]) = new CanBuild20[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20](canBuild(m1, m2), m3)

    def and[A20](m3: M[A20]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19) => B)(implicit fu: Functor[M]): M[B] =
      fu.fmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13 ~ A14 ~ A15 ~ A16 ~ A17 ~ A18 ~ A19, B](canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 ~ a11 ~ a12 ~ a13 ~ a14 ~ a15 ~ a16 ~ a17 ~ a18 ~ a19 => f(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19) })

    def apply[B](f: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19))(implicit fu: ContravariantFunctor[M]): M[B] =
      fu.contramap(canBuild(m1, m2), (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19) = f(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10), a11), a12), a13), a14), a15), a16), a17), a18), a19) })

    def apply[B](f1: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19) => B, f2: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19))(implicit fu: InvariantFunctor[M]): M[B] =
      fu.inmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13 ~ A14 ~ A15 ~ A16 ~ A17 ~ A18 ~ A19, B](
        canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 ~ a11 ~ a12 ~ a13 ~ a14 ~ a15 ~ a16 ~ a17 ~ a18 ~ a19 => f1(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19) },
        (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19) = f2(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10), a11), a12), a13), a14), a15), a16), a17), a18), a19) }
      )

    def join[A >: A1](implicit witness1: <:<[A, A1], witness2: <:<[A, A2], witness3: <:<[A, A3], witness4: <:<[A, A4], witness5: <:<[A, A5], witness6: <:<[A, A6], witness7: <:<[A, A7], witness8: <:<[A, A8], witness9: <:<[A, A9], witness10: <:<[A, A10], witness11: <:<[A, A11], witness12: <:<[A, A12], witness13: <:<[A, A13], witness14: <:<[A, A14], witness15: <:<[A, A15], witness16: <:<[A, A16], witness17: <:<[A, A17], witness18: <:<[A, A18], witness19: <:<[A, A19], fu: ContravariantFunctor[M]): M[A] =
      apply[A]((a: A) => (a: A1, a: A2, a: A3, a: A4, a: A5, a: A6, a: A7, a: A8, a: A9, a: A10, a: A11, a: A12, a: A13, a: A14, a: A15, a: A16, a: A17, a: A18, a: A19))(fu)

    def reduce[A >: A1, B](implicit witness1: <:<[A1, A], witness2: <:<[A2, A], witness3: <:<[A3, A], witness4: <:<[A4, A], witness5: <:<[A5, A], witness6: <:<[A6, A], witness7: <:<[A7, A], witness8: <:<[A8, A], witness9: <:<[A9, A], witness10: <:<[A10, A], witness11: <:<[A11, A], witness12: <:<[A12, A], witness13: <:<[A13, A], witness14: <:<[A14, A], witness15: <:<[A15, A], witness16: <:<[A16, A], witness17: <:<[A17, A], witness18: <:<[A18, A], witness19: <:<[A19, A], fu: Functor[M], reducer: Reducer[A, B]): M[B] =
      apply[B]((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19) => reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.unit(a1: A), a2: A), a3: A), a4: A), a5: A), a6: A), a7: A), a8: A), a9: A), a10: A), a11: A), a12: A), a13: A), a14: A), a15: A), a16: A), a17: A), a18: A), a19: A))(fu)

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19)] =
      // SO UGLY UGLY UGLY workaround for unchecked type erasure warning... no cleaner way found...
      (v: Any) match {
        case fu: Functor[_] => apply { (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19) }(fu.asInstanceOf[Functor[M]])
        case fu: ContravariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19)] { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11, a._12, a._13, a._14, a._15, a._16, a._17, a._18, a._19) }(fu.asInstanceOf[ContravariantFunctor[M]])
        case fu: InvariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19)]({ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19) }, { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11, a._12, a._13, a._14, a._15, a._16, a._17, a._18, a._19) })(fu.asInstanceOf[InvariantFunctor[M]])
      }

  }

  class CanBuild20[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13 ~ A14 ~ A15 ~ A16 ~ A17 ~ A18 ~ A19], m2: M[A20]) {

    def ~[A21](m3: M[A21]) = new CanBuild21[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21](canBuild(m1, m2), m3)

    def and[A21](m3: M[A21]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20) => B)(implicit fu: Functor[M]): M[B] =
      fu.fmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13 ~ A14 ~ A15 ~ A16 ~ A17 ~ A18 ~ A19 ~ A20, B](canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 ~ a11 ~ a12 ~ a13 ~ a14 ~ a15 ~ a16 ~ a17 ~ a18 ~ a19 ~ a20 => f(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20) })

    def apply[B](f: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20))(implicit fu: ContravariantFunctor[M]): M[B] =
      fu.contramap(canBuild(m1, m2), (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20) = f(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10), a11), a12), a13), a14), a15), a16), a17), a18), a19), a20) })

    def apply[B](f1: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20) => B, f2: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20))(implicit fu: InvariantFunctor[M]): M[B] =
      fu.inmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13 ~ A14 ~ A15 ~ A16 ~ A17 ~ A18 ~ A19 ~ A20, B](
        canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 ~ a11 ~ a12 ~ a13 ~ a14 ~ a15 ~ a16 ~ a17 ~ a18 ~ a19 ~ a20 => f1(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20) },
        (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20) = f2(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10), a11), a12), a13), a14), a15), a16), a17), a18), a19), a20) }
      )

    def join[A >: A1](implicit witness1: <:<[A, A1], witness2: <:<[A, A2], witness3: <:<[A, A3], witness4: <:<[A, A4], witness5: <:<[A, A5], witness6: <:<[A, A6], witness7: <:<[A, A7], witness8: <:<[A, A8], witness9: <:<[A, A9], witness10: <:<[A, A10], witness11: <:<[A, A11], witness12: <:<[A, A12], witness13: <:<[A, A13], witness14: <:<[A, A14], witness15: <:<[A, A15], witness16: <:<[A, A16], witness17: <:<[A, A17], witness18: <:<[A, A18], witness19: <:<[A, A19], witness20: <:<[A, A20], fu: ContravariantFunctor[M]): M[A] =
      apply[A]((a: A) => (a: A1, a: A2, a: A3, a: A4, a: A5, a: A6, a: A7, a: A8, a: A9, a: A10, a: A11, a: A12, a: A13, a: A14, a: A15, a: A16, a: A17, a: A18, a: A19, a: A20))(fu)

    def reduce[A >: A1, B](implicit witness1: <:<[A1, A], witness2: <:<[A2, A], witness3: <:<[A3, A], witness4: <:<[A4, A], witness5: <:<[A5, A], witness6: <:<[A6, A], witness7: <:<[A7, A], witness8: <:<[A8, A], witness9: <:<[A9, A], witness10: <:<[A10, A], witness11: <:<[A11, A], witness12: <:<[A12, A], witness13: <:<[A13, A], witness14: <:<[A14, A], witness15: <:<[A15, A], witness16: <:<[A16, A], witness17: <:<[A17, A], witness18: <:<[A18, A], witness19: <:<[A19, A], witness20: <:<[A20, A], fu: Functor[M], reducer: Reducer[A, B]): M[B] =
      apply[B]((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19, a20: A20) => reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.unit(a1: A), a2: A), a3: A), a4: A), a5: A), a6: A), a7: A), a8: A), a9: A), a10: A), a11: A), a12: A), a13: A), a14: A), a15: A), a16: A), a17: A), a18: A), a19: A), a20: A))(fu)

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20)] =
      // SO UGLY UGLY UGLY workaround for unchecked type erasure warning... no cleaner way found...
      (v: Any) match {
        case fu: Functor[_] => apply { (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19, a20: A20) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20) }(fu.asInstanceOf[Functor[M]])
        case fu: ContravariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20)] { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11, a._12, a._13, a._14, a._15, a._16, a._17, a._18, a._19, a._20) }(fu.asInstanceOf[ContravariantFunctor[M]])
        case fu: InvariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20)]({ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19, a20: A20) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20) }, { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11, a._12, a._13, a._14, a._15, a._16, a._17, a._18, a._19, a._20) })(fu.asInstanceOf[InvariantFunctor[M]])
      }

  }

  class CanBuild21[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13 ~ A14 ~ A15 ~ A16 ~ A17 ~ A18 ~ A19 ~ A20], m2: M[A21]) {
    def ~[A22](m3: M[A22]) = new CanBuild22[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22](canBuild(m1, m2), m3)

    def and[A22](m3: M[A22]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21) => B)(implicit fu: Functor[M]): M[B] =
      fu.fmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13 ~ A14 ~ A15 ~ A16 ~ A17 ~ A18 ~ A19 ~ A20 ~ A21, B](canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 ~ a11 ~ a12 ~ a13 ~ a14 ~ a15 ~ a16 ~ a17 ~ a18 ~ a19 ~ a20 ~ a21 => f(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21) })

    def apply[B](f: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21))(implicit fu: ContravariantFunctor[M]): M[B] =
      fu.contramap(canBuild(m1, m2), (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21) = f(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10), a11), a12), a13), a14), a15), a16), a17), a18), a19), a20), a21) })

    def apply[B](f1: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21) => B, f2: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21))(implicit fu: InvariantFunctor[M]): M[B] =
      fu.inmap[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13 ~ A14 ~ A15 ~ A16 ~ A17 ~ A18 ~ A19 ~ A20 ~ A21, B](
        canBuild(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 ~ a11 ~ a12 ~ a13 ~ a14 ~ a15 ~ a16 ~ a17 ~ a18 ~ a19 ~ a20 ~ a21 => f1(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21) },
        (b: B) => { val (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21) = f2(b); new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10), a11), a12), a13), a14), a15), a16), a17), a18), a19), a20), a21) }
      )

    def join[A >: A1](implicit witness1: <:<[A, A1], witness2: <:<[A, A2], witness3: <:<[A, A3], witness4: <:<[A, A4], witness5: <:<[A, A5], witness6: <:<[A, A6], witness7: <:<[A, A7], witness8: <:<[A, A8], witness9: <:<[A, A9], witness10: <:<[A, A10], witness11: <:<[A, A11], witness12: <:<[A, A12], witness13: <:<[A, A13], witness14: <:<[A, A14], witness15: <:<[A, A15], witness16: <:<[A, A16], witness17: <:<[A, A17], witness18: <:<[A, A18], witness19: <:<[A, A19], witness20: <:<[A, A20], witness21: <:<[A, A21], fu: ContravariantFunctor[M]): M[A] =
      apply[A]((a: A) => (a: A1, a: A2, a: A3, a: A4, a: A5, a: A6, a: A7, a: A8, a: A9, a: A10, a: A11, a: A12, a: A13, a: A14, a: A15, a: A16, a: A17, a: A18, a: A19, a: A20, a: A21))(fu)

    def reduce[A >: A1, B](implicit witness1: <:<[A1, A], witness2: <:<[A2, A], witness3: <:<[A3, A], witness4: <:<[A4, A], witness5: <:<[A5, A], witness6: <:<[A6, A], witness7: <:<[A7, A], witness8: <:<[A8, A], witness9: <:<[A9, A], witness10: <:<[A10, A], witness11: <:<[A11, A], witness12: <:<[A12, A], witness13: <:<[A13, A], witness14: <:<[A14, A], witness15: <:<[A15, A], witness16: <:<[A16, A], witness17: <:<[A17, A], witness18: <:<[A18, A], witness19: <:<[A19, A], witness20: <:<[A20, A], witness21: <:<[A21, A], fu: Functor[M], reducer: Reducer[A, B]): M[B] =
      apply[B]((a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19, a20: A20, a21: A21) => reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.append(reducer.unit(a1: A), a2: A), a3: A), a4: A), a5: A), a6: A), a7: A), a8: A), a9: A), a10: A), a11: A), a12: A), a13: A), a14: A), a15: A), a16: A), a17: A), a18: A), a19: A), a20: A), a21: A))(fu)

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21)] =
      // SO UGLY UGLY UGLY workaround for unchecked type erasure warning... no cleaner way found...
      (v: Any) match {
        case fu: Functor[_] => apply { (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19, a20: A20, a21: A21) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21) }(fu.asInstanceOf[Functor[M]])
        case fu: ContravariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21)] { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11, a._12, a._13, a._14, a._15, a._16, a._17, a._18, a._19, a._20, a._21) }(fu.asInstanceOf[ContravariantFunctor[M]])
        case fu: InvariantFunctor[_] => apply[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21)]({ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19, a20: A20, a21: A21) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21) }, { (a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21)) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11, a._12, a._13, a._14, a._15, a._16, a._17, a._18, a._19, a._20, a._21) })(fu.asInstanceOf[InvariantFunctor[M]])
      }

  }

  class CanBuild22[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13 ~ A14 ~ A15 ~ A16 ~ A17 ~ A18 ~ A19 ~ A20 ~ A21], m2: M[A22]) {
  }

}

/* the terrific scala template that generates scala
@(i: Int)

@mk(i: Int, c: String, sep: String) = @{
  Range(1,i+1).map(c+_).mkString(sep) 
}

@mk2(i: Int, c: String, sep: String) = @{
  Range(1,i+1).map(i => c.format(i, i)).mkString(sep) 
}

@canBuild(i: Int) = {
class CanBuild@(i)[@mk(i, "A", ", ")](m1: M[@mk(i-1, "A", " ~ ")], m2: M[A@(i)]){

  def ~[A@(i+1)](m3: M[A@(i+1)]) = new CanBuild@(i+1)(canBuild(m1,m2),m3)

  def and[A@(i+1)](m3: M[A@(i+1)]) = this.~(m3)

  def apply[B](f: (@mk(i, "A", ", ")) => B)(implicit fu: Functor[M]): M[B] =  
    fu.fmap[@mk(i, "A", " ~ "), B](canBuild(m1, m2), { case @mk(i, "a", " ~ ") => f(@mk(i, "a", ", ")) })

  def apply[B](f: B => (@mk(i, "A", ", ")))(implicit fu:ContravariantFunctor[M]): M[B] = 
    fu.contramap(canBuild(m1, m2), (b: B) => { val (@mk(i, "a", ", ")) = f(b); @controllers.Application.recJsonGenerate(i)})

  def apply[B](f1: (@mk(i, "A", ", ")) => B, f2: B => (@mk(i, "A", ", ")))(implicit fu:InvariantFunctor[M]): M[B] =  
    fu.inmap[@mk(i, "A", " ~ "), B](
      canBuild(m1, m2),  {case @mk(i, "a", " ~ ") => f1(@mk(i, "a", ", "))}, 
      (b: B) => { val (@mk(i, "a", ", ")) = f2(b); @controllers.Application.recJsonGenerate(i) }
    )

  def join[A >: A1](implicit @mk2(i, "witness%d: <:<[A, A%d]", ", "), fu: ContravariantFunctor[M]): M[A] = 
    apply[A]( (a: A) => (@mk(i, "a: A", ", ")) )(fu)

  def reduce[A >: A1, B](implicit @mk2(i, "witness%d: <:<[A%d, A]", ", "), fu: Functor[M], reducer: Reducer[A, B]): M[B] = 
    apply[B]( (@mk2(i, "a%d: A%d", ", ")) => @controllers.Application.recJsonGenerate2(i) )(fu)

  def tupled(implicit v:Variant[M]): M[(@mk(i, "A", ", "))] = 
    // SO UGLY UGLY UGLY workaround for unchecked type erasure warning... no cleaner way found...
    (v: Any) match {
      case fu: Functor[_] => apply{ (@mk2(i, "a%d: A%d", ", ")) => (@mk(i, "a", ", ")) }(fu.asInstanceOf[Functor[M]])
      case fu: ContravariantFunctor[_] => apply[(@mk(i, "A", ", "))]{ (a: (@mk(i, "A", ","))) => (@mk(i, "a._", ", ")) }(fu.asInstanceOf[ContravariantFunctor[M]])
      case fu: InvariantFunctor[_] => apply[(@mk(i, "A", ", "))]({ (@mk2(i, "a%d: A%d", ", ")) => (@mk(i, "a", ", ")) }, { (a: (@mk(i, "A", ", "))) => (@mk(i, "a._", ", ")) })(fu.asInstanceOf[InvariantFunctor[M]])
    } 

}
}

@Range(2,i+1).map(canBuild(_))
*/

/* the terrific Controller to generate code
object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def jsonUtil = Action {
    Ok(views.txt.jsonUtil(21))
  }
  
  def recJsonGenerate(i: Int) = {
    def step(idx: Int, c: String): String = {
      if(idx < i) {
        step(idx+1, "new ~(" + c + ", a" + (idx+1) + ")" )
      } else {
        c
      }
    } 

    step(1, "a1")
  }
  
  // reducer.append(reducer.unit(a1: A), a2: A)
  def recJsonGenerate2(max: Int) = {
    def step(idx: Int, c: String): String = {
      if(idx < max) {
        step(idx+1, "reducer.append(" + c + ", a" + (idx+1) + ": A)" )
      } else {
        c
      }
    } 

    step(1, "reducer.unit(a1: A)")
  }
}
*/
