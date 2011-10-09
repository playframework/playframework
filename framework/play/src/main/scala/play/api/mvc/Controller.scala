package play.api.mvc


trait Controller extends Results with play.api.http.HeaderNames {
    
    final def Action(block: => Result): Action[Map[String,Seq[String]]] = this.Action((_: Context[Map[String,Seq[String]]]) => block)
    final def Action(block: Context[Map[String,Seq[String]]] => Result): Action[Map[String,Seq[String]]] = this.Action[Map[String,Seq[String]]]( play.api.data.RequestData.urlEncoded("UTF-8" /* should get charset from content type */), block)
    def Action[A](bodyParser: BodyParser[A], block: Context[A] => Result): Action[A] = play.api.mvc.Action[A](bodyParser,block)

    val TODO: Action[Map[String,Seq[String]]] = Action { NotImplemented(play.core.views.html.todo()) }
    
    implicit def request[A](implicit ctx: Context[A]): Request[A] = ctx.request
    def session(implicit request: RequestHeader): Map[String,String] = request.session

}

trait Action[A] extends (Context[A] => Result) { 
    
    type BODY_CONTENT = A
    
    def parser: BodyParser[A]
    def apply(ctx: Context[A]): Result
}

trait BodyParser[+T] extends Function1[RequestHeader, play.core.Iteratee.Iteratee[Array[Byte],T]]

object BodyParser {

    def apply[T](f: Function1[RequestHeader, play.core.Iteratee.Iteratee[Array[Byte],T]]) = new BodyParser[T] {
        def apply(rh: RequestHeader) = f(rh)
    }

}

object Action {

    def apply[A](bodyParser: BodyParser[A],block: Context[A] => Result): Action[A] = new Action[A] { 
        def parser = bodyParser
        def apply(ctx: Context[A]) = block(ctx)
    }

    def apply(block: Context[Map[String,Seq[String]]] => Result): Action[Map[String,Seq[String]]] = {
        Action[Map[String,Seq[String]]](play.api.data.RequestData.urlEncoded("UTF-8" /* should get charset from content type */), block)
    }
    
}

trait DefaultAction extends (Context[Map[String,Seq[String]]] => Result) with Action[Map[String,Seq[String]]] {
    def parser = play.api.data.RequestData.urlEncoded("UTF-8" /* should get charset from content type */)
}

object DefaultAction{
    
    def apply(block:(Context[Map[String,Seq[String]]] => Result)) = new DefaultAction{ 
        def apply(c: Context[Map[String,Seq[String]]]) = block(c)
    }
    
}
