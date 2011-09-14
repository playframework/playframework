package play.core

import play.core.server._
import play.core.Iteratee._

import play.api._
import play.api.mvc._
import play.api.mvc.Results._

import akka.actor.Actor
import akka.dispatch.Dispatchers._

object DispatchStrategy{

    val d  = newExecutorBasedEventDrivenWorkStealingDispatcher("name")
                .withNewThreadPoolWithLinkedBlockingQueueWithCapacity(1000)
                .setCorePoolSize(3)
                .setMaxPoolSize(3)
                .build


    val sockets  = newExecutorBasedEventDrivenDispatcher("name")
                .withNewThreadPoolWithLinkedBlockingQueueWithCapacity(1000)
                .setCorePoolSize(3)
                .setMaxPoolSize(3)
                .build

    val promises  = newExecutorBasedEventDrivenDispatcher("name")
                .withNewThreadPoolWithLinkedBlockingQueueWithCapacity(1000)
                .setCorePoolSize(3)
                .setMaxPoolSize(3)
                .build

    val stmPromiseRedeem  = newExecutorBasedEventDrivenWorkStealingDispatcher("name")
                .withNewThreadPoolWithLinkedBlockingQueueWithCapacity(1000)
                .setCorePoolSize(3)
                .setMaxPoolSize(3)
                .build

}

class Invoker extends Actor {
    self.dispatcher = DispatchStrategy.d

    def receive = {
        case (request:Request, response:Response, appProvider:ApplicationProvider) => {

            val result = try {
                appProvider.get.fold(
                    error => DefaultGlobal.onError(error),
                    application => try {
                        application.global.onRouteRequest(request).map { action =>
                            try {
                                action(Context(request))
                            } catch {
                                case e:Exception => throw ExecutionException(e, application.sources.sourceFor(e))
                            }
                        }.getOrElse(application.global.onActionNotFound(request))
                    } catch {
                        case e => application.global.onError(e)
                    }
                )
            } catch {
                case e => DefaultGlobal.onError(e)
            }
                    
            response.handle(result)
        }
    }
    
}

case class Invoke[A](a:A,k: A=>Unit)
class PromiseInvoker extends Actor {

    self.dispatcher = DispatchStrategy.promises

    def receive = {
        case Invoke(a,k) => k(a)
    }
}
object PromiseInvoker {
    import akka.actor.Actor._
    import akka.routing.Routing._
    import akka.routing.SmallestMailboxFirstIterator

    private def newInvoker() = {val inv = actorOf(new PromiseInvoker()); inv.start() ;inv }

    val invoker = loadBalancerActor( new SmallestMailboxFirstIterator(List.fill(2)(newInvoker ()) ) ).start()
}


object Agent{
    def apply[A](a:A)= {
        import akka.actor.Actor._
        var actor = actorOf(new Agent[A](a)); 
        actor.start()
        new {
            def send(action:(A=>A)){ actor ! action}
          
            def close() = { actor.exit(); actor=null; }
        }
    }
}


private class Agent[A](var a:A) extends Actor {

    self.dispatcher = DispatchStrategy.sockets

    def receive = {
        
        case action: Function1[A,A] => a = action(a)
                    
    }
    
}
