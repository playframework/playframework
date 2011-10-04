package play.api.mvc

trait Controller extends Results {
    
    final def Action(block: => Result):Action[Map[String,Seq[String]]] = Action {(_:Context[Map[String,Seq[String]]]) => block}
    def Action[A](bodyParser:BodyParser[A], block:Context[A] => Result):Action[A] = Action[A]( bodyParser,block)

    final def Action(block:Context[Map[String,Seq[String]]] => Result):Action[Map[String,Seq[String]]] =  Action[Map[String,Seq[String]]]( play.api.data.RequestData.urlEncoded("UTF-8" /* should get charset from content type */), block)

    val TODO:Action[Map[String,Seq[String]]] = Action { NotImplemented(play.core.views.html.todo()) }

}

import play.core.Iteratee._

case class ActionParams[A](parser:BodyParser[A], f:Context[A] => Result ){
    type B = A
}
trait Action[A]{ def con:ActionParams[A]}
trait BodyParser[+T] extends Function1[RequestHeader, Iteratee[Array[Byte],T]]
object BodyParser {

    def apply[T](f:Function1[RequestHeader, Iteratee[Array[Byte],T]]) = new BodyParser[T] {
        def apply(rh:RequestHeader) = f(rh)
    }

}


object Action {

    def apply[A](bodyParser:BodyParser[A],block:Context[A] => Result):Action[A] =  new Action[A]{ def con:ActionParams[A] = ActionParams[A](bodyParser,block) }

    def apply(block:Context[Map[String,Seq[String]]] => Result):Action[Map[String,Seq[String]]] =  Action[Map[String,Seq[String]]](play.api.data.RequestData.urlEncoded("UTF-8" /* should get charset from content type */), block)
}

trait DefaultAction extends (Context[Map[String,Seq[String]]] => Result) with Action[Map[String,Seq[String]]] {
    def con = ActionParams( play.api.data.RequestData.urlEncoded("UTF-8" /* should get charset from content type */),this)
}

object DefaultAction{
    def apply(block:(Context[Map[String,Seq[String]]] => Result)) = new DefaultAction{ def apply(c:Context[Map[String,Seq[String]]])= block(c)}
}
