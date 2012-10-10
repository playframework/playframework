package play.core

import akka.actor._
import akka.actor.Actor._
import akka.routing._

import com.typesafe.config._

import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.iteratee._
import play.api.http.HeaderNames._

import play.utils._
import javax.annotation.concurrent.GuardedBy

/**
 * holds Play's internal invokers
 */
class Invoker(applicationProvider: Option[ApplicationProvider] = None) {

  val system: ActorSystem = applicationProvider.map { a =>
    Invoker.appProviderActorSystem(a)
  }.getOrElse(ActorSystem("play"))

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
object Invoker {

  /**
   * provides an extractor for body parser
   */
  //case class GetBodyParser(request: RequestHeader, bodyParser: BodyParser[_])

  /**
   * provides actor helper
   */
  //case class HandleAction[A](request: Request[A], response: Response, action: Action[A], app: Application)

  private var invokerOption: Option[(Invoker, Array[StackTraceElement])] = None

  private def invoker: Invoker = invokerOption.map(_._1).getOrElse {
    throw new IllegalStateException("Invoker system not started or inited")
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
   * Start an invoker in the global scope, used by tests
   */
  def start() {
    synchronized {
      init(new Invoker())
    }
  }

  /**
   * saves invoker instance in global scope
   */
  def init(invoker: => Invoker) {
    synchronized {
      invokerOption = invokerOption match {
        case Some(existing) => throw new IllegalStateException(existing._2.dropWhile(_.getClassName == "play.core.Invoker$").mkString(
            "Invoker initialised twice, first invocation was: ", "\n\tat ", "\nMake sure you call Invoker.reset() after using it.\n"))
        case None => Some((invoker, new Exception().getStackTrace))
      }
    }
  }

  /**
   * removes invoker instance from global scope
   */
  def reset(): Unit = synchronized {
    invokerOption.map(_._1.stop())
    invokerOption = None
  }

  /**
   * provides actor system
   */
  def system = invoker.system

}

object Agent {

  class Operations[A](actor: ActorRef, c: => Unit) {
    def send(action: (A => A)) { actor ! action }
    def close(): Unit = c
  }

  def apply[A](invoker: Invoker, a: A): Operations[A] = {
    val actor: ActorRef = invoker.system.actorOf(Props(new Agent[A](a)).withDispatcher("akka.actor.websockets-dispatcher"))
    new Operations[A](actor, invoker.system.stop(actor))
  }

  private class Agent[A](var a: A) extends Actor {
    def receive = {
      case action: Function1[_, _] => a = action.asInstanceOf[Function1[A, A]](a)
    }
  }

}

