package play.api.mvc

trait ControllerLike {
  def Action[A](bodyParser: BodyParser[A], block: Request[A] => Result): Action[A]
}

trait Controller extends ControllerLike with Results with play.api.http.HeaderNames {

  final def Action(block: => Result): Action[AnyContent] = this.Action((ctx: Request[AnyContent]) => block)
  final def Action(block: Request[AnyContent] => Result): Action[AnyContent] = this.Action[AnyContent](play.api.data.RequestData.urlEncoded("UTF-8" /* should get charset from content type */ ), block)

  def Action[A](bodyParser: BodyParser[A], block: Request[A] => Result): Action[A] = play.api.mvc.Action[A](bodyParser, block)

  val TODO = Action { NotImplemented(views.html.defaultpages.todo()) }

  implicit def session(implicit request: RequestHeader) = request.session

}

