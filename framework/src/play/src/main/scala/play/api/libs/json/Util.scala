package play.api.libs.json.util

trait LazyHelper[M[_], T] {
  def lazyStuff: M[T]
}

object LazyHelper{
  def apply[M[_], T](stuff: M[T]) = new LazyHelper[M, T] {
    override lazy val lazyStuff = stuff
  }
}
