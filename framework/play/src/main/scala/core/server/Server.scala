package play.core.server

import play.core._
import play.api.mvc._

trait Server {
    
    import akka.actor._
    import akka.actor.Actor._
    import akka.routing.Routing._
    import akka.routing.SmallestMailboxFirstIterator
    import akka.config._
    import akka.config.Supervision._

    def newInvoker = {val inv = actorOf[Invoker]; inv.start(); inv}
    
    val invoker = loadBalancerActor(new SmallestMailboxFirstIterator(List.fill(3)(newInvoker))).start()

    def invoke(request:Request, response:Response) {
        invoker ! (request, response, applicationProvider)
    }
    
    def applicationProvider:ApplicationProvider
    
}
