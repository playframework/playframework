package anorm

/** Parsed SQL result. */
trait SqlResult[+A] { self =>

  def flatMap[B](k: A => SqlResult[B]): SqlResult[B] = self match {
    case Success(a) => k(a)
    case e @ Error(_) => e
  }

  def map[B](f: A => B): SqlResult[B] = self match {
    case Success(a) => Success(f(a))
    case e @ Error(_) => e
  }
}

/** Successfully parsed result. */
case class Success[A](a: A) extends SqlResult[A]

/** Erroneous result (failure while parsing). */
case class Error(msg: SqlRequestError) extends SqlResult[Nothing]
