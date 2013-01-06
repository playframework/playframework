package play.core

import akka.actor._
import akka.actor.Actor._

import com.typesafe.config._

/**
 * holds Play's internal invokers
 */
class Invoker(applicationProvider: Option[ApplicationProvider] = None) {

  val system: ActorSystem = applicationProvider.map{a =>
    Invoker.appProviderActorSystem(a)
  }.getOrElse(ActorSystem("play")) 

  val promiseDispatcher = {
    system.dispatchers.lookup("akka.actor.promises-dispatcher")
  }

  val actionInvoker = {
    system.actorOf(Props[ActionInvoker].withDispatcher("akka.actor.actions-dispatcher").withRouter(RoundRobinRouter(100)), name = "actions")
  }

  /**
   * kills actor system 
   */
  def stop(): Unit = {
    system.shutdown()
    system.awaitTermination()
  }
}

/**
 * provides Play's internal actor system and the corresponding actor instances
 */
<<<<<<< .merge_file_2OsMDf
private[play] object Invoker {
=======
object Invoker {

  /**
   * provides an extractor for body parser
   */ 
  case class GetBodyParser(request: RequestHeader, bodyParser: BodyParser[_])

  /**
   * provides actor helper
   */ 
  case class HandleAction[A](request: Request[A], response: Response, action: Action[A], app: Application)

  private var invokerOption: Option[Invoker] = None

  private def invoker: Invoker = invokerOption.getOrElse{
      val default = new Invoker()
      invokerOption = Some(default)
      Logger.warn ("Invoker was created outside of Invoker#init - this potentially could lead to initialization problems in production mode")
      default
  }

  private def appProviderActorSystem(applicationProvider: ApplicationProvider) = {
    val conf = play.api.Play.maybeApplication.filter(_.mode == Mode.Prod).map(app =>
      ConfigFactory.load()).getOrElse(Configuration.loadDev(applicationProvider.path))
    ActorSystem("play", conf.getConfig("play"))
  }
  
  /**
   * contructor used by Server
   */
  def apply(applicationProvider: ApplicationProvider): Invoker = new Invoker(Some(applicationProvider))

  /**
   * saves invoker instance in global scope
   */
  def init(invoker: Invoker): Unit = {
    if (invokerOption.isDefined)
      throw new IllegalStateException("Invoker was initialized twice without an intervening uninit; two Server created at once?")
    invokerOption = Some(invoker)
  }

  /**
   * removes invoker instance from global scope
   */
  def uninit(): Unit = {
    invokerOption = None
  }

  /**
   * provides actor system
   */
  def system = invoker.system

  /**
   * provides promise dispatcher
   */
  def promiseDispatcher = invoker.promiseDispatcher

   /**
   * provides invoker used for Action dispatching
   */
  def actionInvoker = invoker.actionInvoker
}

/**
 * an Akka actor responsible for dispatching Actions.
 */
class ActionInvoker extends Actor {

  def receive = {

    case Invoker.GetBodyParser(request, bodyParser) => {
      sender ! (bodyParser(request))
    }

    case Invoker.HandleAction(request, response: Response, action, app: Application) => {
>>>>>>> .merge_file_VS9kxc

  val system: ActorSystem = ActorSystem("play") 

  val executionContext: scala.concurrent.ExecutionContext = system.dispatcher

}
<<<<<<< .merge_file_2OsMDf
=======

object Agent {

  class Operations[A](actor: ActorRef, c: => Unit) {
    def send(action: (A => A)) { actor ! action }
    def close(): Unit = c
  }

  def apply[A](a: A): Operations[A] = {
    val actor: ActorRef = Invoker.system.actorOf(Props(new Agent[A](a)).withDispatcher("akka.actor.websockets-dispatcher"))
    new Operations[A](actor, Invoker.system.stop(actor))
  }

  private class Agent[A](var a: A) extends Actor {
    def receive = {
      case action: Function1[_, _] => a = action.asInstanceOf[Function1[A, A]](a)
    }
  }

}

>>>>>>> .merge_file_VS9kxc
