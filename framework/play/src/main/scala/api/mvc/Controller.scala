package play.api.mvc

trait Controller extends Results {
    
    final def Action(block: => Result):Action = Action {_ => block}
    def Action(block:Context => Result):Action = new DefaultAction(block)
    
}

trait Action extends Function1[Context,Result]

object Action {
    
    def apply(block:Context => Result):Action = new DefaultAction(block)
    
}

class DefaultAction(block:Context => Result) extends Action {
    def apply(ctx:Context) = block(ctx)
}
