package play.core

import akka.actor.Actor
import akka.actor.Props
import akka.actor.Actor._
import akka.dispatch.Dispatchers._
import akka.dispatch.Future
import akka.dispatch.Await
import akka.util.duration._
import akka.actor.OneForOneStrategy
import akka.routing.{ DefaultActorPool, FixedCapacityStrategy, SmallestMailboxSelector }
import com.typesafe.config.Config

import play.core.server._
import play.api.libs.iteratee._
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.http.HeaderNames._
import play.api.libs.akka.Akka._

case class HandleAction[A](request: Request[A], response: Response, action: Action[A], app: Application)
class Invoker extends Actor {

  def receive = {

    case (requestHeader: RequestHeader, bodyFunction: BodyParser[_]) => sender ! (bodyFunction(requestHeader))

    case HandleAction(request, response: Response, action, app: Application) =>

      val result = try {
        // Be sure to use the Play classloader in this Thread
        Thread.currentThread.setContextClassLoader(app.classloader)
        try {
          action(request)
        } catch {
          case e: PlayException.UsefulException => throw e
          case e: Throwable => {

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

  def receive = {
    case Invoke(a, k) => k(a)
  }
}

object PromiseInvoker {

  private def getInt(c: Config, key: String) = try { Option(c.getInt(key)) } catch { case e: com.typesafe.config.ConfigException.Missing => None }

  private lazy val c = system.settings.config

  private lazy val invokerLimit = getInt(c, "invoker.limit").getOrElse(5)
  private lazy val withinTime = getInt(c, "invoker.withinTime").getOrElse(1000)
  private lazy val retries = getInt(c, "invoker.max.try").getOrElse(1)
  private lazy val count = getInt(c, "invoker.selection.count").getOrElse(1)

  private val faultHandler = OneForOneStrategy(List(classOf[Exception]), retries, withinTime)

  private lazy val pool = system.actorOf(
    Props(new Actor with DefaultActorPool with FixedCapacityStrategy with SmallestMailboxSelector {
      def instance(defaults: Props) = system.actorOf(defaults.withCreator(new PromiseInvoker).withDispatcher("invoker.promise-dispatcher"))
      def limit = invokerLimit
      def selectionCount = count
      def partialFill = true
      def receive = _route
    }).withFaultHandler(faultHandler))

  val invoker = pool
}

object Agent {

  def apply[A](a: A) = {
    val actor = system.actorOf(Props(new Agent[A](a)).withDispatcher("invoker.socket-dispatcher"))
    new {
      def send(action: (A => A)) { actor ! action }
      def close() = { system.stop(actor) }
    }

  }
  private class Agent[A](var a: A) extends Actor {
    def receive = {
      case action: Function1[A, A] => a = action(a)
    }
  }
}

