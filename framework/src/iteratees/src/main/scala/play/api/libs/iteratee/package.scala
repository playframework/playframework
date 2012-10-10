package play.api.libs {

  /**
   * The Iteratee monad provides strict, safe, and functional I/O.
   */
  package object iteratee {

    type K[E, A] = Input[E] => Iteratee[E, A]

  }

}

package play.api.libs.iteratee {

  object internal {
    private[iteratee] implicit lazy val defaultExecutionContext: scala.concurrent.ExecutionContext =
      scala.concurrent.ExecutionContext.Implicits.global
  }

}
