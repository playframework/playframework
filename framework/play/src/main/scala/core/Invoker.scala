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

class Invoker(i:Int) extends Actor {
    self.dispatcher = DispatchStrategy.d

    def receive = {
        case (request:Request, response:Response,appProvider:ApplicationProvider) => {
            val result =
                appProvider.get.fold(
                    error => InternalServerError(error),
                    application => try {
                        application.actionFor(request).map { action =>
                            action(Context(application, request))
                        }.getOrElse(NotFound)
                    } catch {
                          case e:PlayException => InternalServerError(e)
                          case e => InternalServerError(UnexpectedException(unexpected = Some(e)))
                    } )
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

object Promise1 {
import scala.ref.WeakReference
class ActorPromise[A] extends Promise[A]{

    private[Promise1] var actions: Option[List[A => Unit]] = Some(List())
    private[Promise1] var redeemed: Option[A] = None

    class InternalPromise extends Promise[A] {

        def onRedeem(k: A => Unit) = actor ! OnRedeem(k)

        def redeem(a:A) = actor ! Redeem(a)

        def map[B](f: A => B): Promise[B] = {
            val result = new ActorPromise[B]()
            this.onRedeem(a => result.redeem(f(a)))
            result
        }
        def flatMap[B](f: A => Promise[B]) = {
            val result = new ActorPromise[B]()
            this.onRedeem(a => f(a).onRedeem(result.redeem(_)))
            result
        }

        override def finalize() = println("at least")
    }

    var actualPromise:Promise[A] = new InternalPromise()

    override def finalize() = { println("finalizing promise"); actor ! Shutdown }

    import akka.actor.Actor._

    val actor = {
        val wr = new WeakReference(this)
        actorOf(new PromiseActor(wr)).start() }

    def onRedeem(k: A => Unit) = actualPromise.onRedeem(k)
    def redeem(a:A) = actualPromise.redeem(a)
    def map[B](f: A => B): Promise[B] = actualPromise.map(f)
    def flatMap[B](f: A => Promise[B]) = actualPromise.flatMap(f)


}
    case class Redeem[A](a:A)
    case class OnRedeem[A](k: A => Unit )
    case object Shutdown

   private[Promise1] class PromiseActor[A](promise:WeakReference[ActorPromise[A]]) extends Actor {

        self.dispatcher = DispatchStrategy.promises


        def receive = {

            case OnRedeem(k) => 
                val p = promise.get.get
                p.redeemed.map(k)
                        .getOrElse(p.actions = p.actions.map(as => as :+ k)
                                         .orElse(error("the promise is in un inconsistent state")))
            case Redeem(a:A) =>
                val p = promise.get.get
                p.actualPromise = PurePromise(a)
                p.redeemed = Some(a)
                p.actions.flatten.foreach(a => self ! OnRedeem(a) )
                self ! Shutdown

            case Shutdown => 
                println("shutting down actor")
                self.exit()

        }
    
      override def finalize() = println("finalizing promise actor... ")
    }
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
