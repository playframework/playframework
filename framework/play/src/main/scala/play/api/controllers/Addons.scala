package controllers

import play.api._
import play.api.mvc._
import play.api.mvc.Results._

case class Secured[A](
    isAuthentified: RequestHeader=>Boolean,
    onUnauthorized: RequestHeader=>Result = (_ => Unauthorized(play.core.views.html.unauthorized()))
)(action: Action[A]) extends Action[A] {

    def parser = action.parser

    def apply(ctx: Context[A]) = {
        if(isAuthentified(ctx.request)) {
            action(ctx)
        } else {
            onUnauthorized(ctx.request)
        }
    }

}

trait AllSecured extends Controller {

    override def Action[A](parser: BodyParser[A],block: Context[A] => Result) = Secured(
        isAuthentified = {req => isAuthentified(req)},
        onUnauthorized = {req => onUnauthorized(req)}
    )(super.Action(parser,block))

    def isAuthentified(request: RequestHeader): Boolean
    def onUnauthorized(request: RequestHeader): Result = Unauthorized(play.core.views.html.unauthorized())

}