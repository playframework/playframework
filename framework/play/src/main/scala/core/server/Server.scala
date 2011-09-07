package play.core.server

import play.core._
import play.api.mvc._



trait Server {
    
    import akka.actor.Actor._
    import akka.routing.Routing._
    import akka.routing.SmallestMailboxFirstIterator

    def newInvoker(i:Int) = {val inv = actorOf(new Invoker(i)); inv.start() ;inv }

    val invoker = loadBalancerActor( new SmallestMailboxFirstIterator( Range(0,3).map(newInvoker ) ) ).start()

    def invoke(request:Request, response:Response) {
        invoker ! (request, response,applicationProvider)
    }
    
    def applicationProvider:ApplicationProvider
    
}
