package play.api.mvc

trait Action[A] extends (Request[A] => Result) {

  type BODY_CONTENT = A

  def parser: BodyParser[A]
  def apply(ctx: Request[A]): Result

  def compose(composer: (Request[A], Action[A]) => Result) = {
    val self = this
    new Action[A] {
      def parser = self.parser
      def apply(request: Request[A]) = composer(request, self)
    }
  }

  // For better support in the routes file
  def apply() = this

}

trait BodyParser[+T] extends Function1[RequestHeader, play.core.Iteratee.Iteratee[Array[Byte], T]]

object BodyParser {

  def apply[T](f: Function1[RequestHeader, play.core.Iteratee.Iteratee[Array[Byte], T]]) = new BodyParser[T] {
    def apply(rh: RequestHeader) = f(rh)
  }

}

case class AnyContent(urlFormEncoded: Map[String, Seq[String]])

object Action {

  def apply[A](bodyParser: BodyParser[A], block: Request[A] => Result): Action[A] = new Action[A] {
    def parser = bodyParser
    def apply(ctx: Request[A]) = block(ctx)
  }

  def apply(block: Request[AnyContent] => Result): Action[AnyContent] = {
    Action(play.api.data.RequestData.urlEncoded("UTF-8" /* should get charset from content type */ ), block)
  }

  def apply(block: => Result): Action[AnyContent] = {
    this.apply(_ => block)
  }

}
