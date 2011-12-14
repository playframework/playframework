package play.core

import play.core.server._
import play.api.libs.iteratee._

import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.http.HeaderNames._

import akka.actor.Actor
import akka.dispatch.Dispatchers._

object DispatchStrategy {

  val d = newExecutorBasedEventDrivenWorkStealingDispatcher("name")
    .withNewThreadPoolWithLinkedBlockingQueueWithCapacity(1000)
    .setCorePoolSize(3)
    .setMaxPoolSize(3)
    .build

  val sockets = newExecutorBasedEventDrivenDispatcher("name")
    .withNewThreadPoolWithLinkedBlockingQueueWithCapacity(1000)
    .setCorePoolSize(3)
    .setMaxPoolSize(3)
    .build

  val promises = newExecutorBasedEventDrivenDispatcher("name")
    .withNewThreadPoolWithLinkedBlockingQueueWithCapacity(1000)
    .setCorePoolSize(3)
    .setMaxPoolSize(3)
    .build

}

case class HandleAction[A](request: Request[A], response: Response, action: Action[A], app: Application)
class Invoker extends Actor {
  self.dispatcher = DispatchStrategy.d

  def receive = {

    case (requestHeader: RequestHeader, bodyFunction: BodyParser[_]) => self.reply(bodyFunction(requestHeader))

    case HandleAction(request, response: Response, action, app: Application) =>

      val result = try {
        // Be sure to use the Play classloader in this Thread
        Thread.currentThread.setContextClassLoader(app.classloader)
        try {
          action(request)
        } catch {
          case e: Exception => {

            val source = app.sources.flatMap(_.sourceFor(e))

            throw new PlayException(
              "Execution exception",
              "[%s: %s]".format(e.getClass.getSimpleName, e.getMessage),
              Some(e)) with PlayException.ExceptionSource {
              def line = source.map(_._2)
              def position = None
              def input = source.map(_._1).map(scalax.file.Path(_))
              def sourceName = source.map(_._1.getAbsolutePath)
            }

          }

        }
      } catch {
        case e => try {

          Logger.error(
            """
            |
            |! %sInternal server error, for request [%s] ->
            |""".stripMargin.format(e match {
              case p: PlayException => "@" + p.id + " - "
              case _ => ""
            }, request),
            e)

          app.global.onError(request, e)
        } catch {
          case e => DefaultGlobal.onError(request, e)
        }
      }

      response.handle {

        // Handle Flash Scope (probably not the good place to do it)
        result match {
          case r @ SimpleResult(header, _) => {

            val flashCookie = {
              header.headers.get(SET_COOKIE)
                .map(Cookies.decode(_))
                .flatMap(_.find(_.name == Flash.COOKIE_NAME)).orElse {
                  Option(request.flash).filterNot(_.isEmpty).map { _ =>
                    Cookie(Flash.COOKIE_NAME, "", 0)
                  }
                }
            }

            flashCookie.map { newCookie =>
              r.copy(header = header.copy(headers = header.headers + (SET_COOKIE -> Cookies.merge(header.headers.get(SET_COOKIE).getOrElse(""), Seq(newCookie)))))
            }.getOrElse(r)

          }
          case r => r
        }

      }

  }
}

case class Invoke[A](a: A, k: A => Unit)
class PromiseInvoker extends Actor {

  self.dispatcher = DispatchStrategy.promises

  def receive = {
    case Invoke(a, k) => k(a)
  }
}
object PromiseInvoker {
  import akka.actor.Actor._
  import akka.routing.Routing._
  import akka.routing.SmallestMailboxFirstIterator

  private def newInvoker() = { val inv = actorOf(new PromiseInvoker()); inv.start(); inv }

  val invoker = loadBalancerActor(new SmallestMailboxFirstIterator(List.fill(2)(newInvoker()))).start()
}

object Agent {
  def apply[A](a: A) = {
    import akka.actor.Actor._
    var actor = actorOf(new Agent[A](a));
    actor.start()
    new {
      def send(action: (A => A)) { actor ! action }

      def close() = { actor.exit(); actor = null; }
    }
  }
}

private class Agent[A](var a: A) extends Actor {

  self.dispatcher = DispatchStrategy.sockets

  def receive = {

    case action: Function1[A, A] => a = action(a)

  }

}
