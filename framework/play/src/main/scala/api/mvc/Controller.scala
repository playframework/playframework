package play.api.mvc

trait Controller extends Results {
    
    final def Action(block: => Result):Action = Action {_ => block}
    def Action(block:Context[Map[String,Seq[String]]] => Result):Action = new DefaultAction{ def apply(c:Context[Map[String,Seq[String]]])= block(c)}



}

import play.core.Iteratee._

case class ActionParams[A](parser:BodyParser[A], f:Context[A] => Result ){
  type B = A
}
trait Action{ def con:ActionParams[a] forSome{type a}}
trait BodyParser[T] extends Function1[RequestHeader, Iteratee[Array[Byte],T]]
object BodyParser {

    def apply[T](f:Function1[RequestHeader, Iteratee[Array[Byte],T]]) = new BodyParser[T] {

        def apply(rh:RequestHeader) = f(rh)
    }

}


object Action {
    
    def apply(block:Context[Map[String,Seq[String]]] => Result):Action =  DefaultAction(block)
    
}
trait DefaultAction extends (Context[Map[String,Seq[String]]] => Result) with Action {
    def con = ActionParams( play.core.data.RequestData.urlEncoded("UTF-8" /* should get charset from content type */),this)

}
object DefaultAction{
    def apply(block:(Context[Map[String,Seq[String]]] => Result)) = new DefaultAction{ def apply(c:Context[Map[String,Seq[String]]])= block(c)}

}
