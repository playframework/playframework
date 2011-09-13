package play.api.mvc

trait Controller extends Results {
    
    def Action(block: => Result):Action = new DefaultAction(_ => block)
    def Action(block:Context => Result):Action = new DefaultAction(block)
    
}

trait Action extends Function1[Context,Result]

class DefaultAction(block:Context => Result) extends Action {
    def apply(ctx:Context) = block(ctx)
}
