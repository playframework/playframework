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

/**
 * provides Play's internal actor system and the corresponding actor instances
 */
object Invoker {

  case class GetBodyParser(request: RequestHeader, bodyParser: BodyParser[_])
  case class HandleAction[A](request: Request[A], response: Response, action: Action[A], app: Application)

  // --

  // Call init to register an Actor System properly configured from 
  // this applicationProvider. Otherwise a default ActorSystem will be created.
  def init(applicationProvider: ApplicationProvider) {
    val conf = play.api.Play.maybeApplication.filter(_.mode == Mode.Prod).map(app =>
      ConfigFactory.load()).getOrElse(Configuration.loadDev(applicationProvider.path))
    configuredSystem = ActorSystem("play", conf.getConfig("play"))
    promiseInvoker
    actionInvoker
  }

  private var configuredSystem: ActorSystem = _

  // --

  lazy val system = {
    Option(configuredSystem).getOrElse {
      Logger.warn("Missing configuration for Play ActorSystem. Starting a default one.")
      ActorSystem("play")
    }
  }

  lazy val promiseInvoker = {
    system.actorOf(Props[play.api.libs.concurrent.STMPromise.PromiseInvoker].withDispatcher("akka.actor.promises-dispatcher").withRouter(RoundRobinRouter(100)), name = "promises")
  }

  lazy val actionInvoker = {
    system.actorOf(Props[ActionInvoker].withDispatcher("akka.actor.actions-dispatcher").withRouter(RoundRobinRouter(100)), name = "actions")
  }

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

      val result = try {
        try {
          Threads.withContextClassLoader(app.classloader) {
            action(request)
          }
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
          case r: PlainResult => {

            val header = r.header

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
              r.withHeaders(SET_COOKIE -> Cookies.merge(header.headers.get(SET_COOKIE).getOrElse(""), Seq(newCookie)))
            }.getOrElse(r)

          }
          case r => r
        }

      }

    }
  }
}

object Agent {

  def apply[A](a: A): {
    def send(action: (A => A)): Unit
    def close(): Unit
  } = {
    val actor = Invoker.system.actorOf(Props(new Agent[A](a)).withDispatcher("akka.actor.websockets-dispatcher"))
    new {
      def send(action: (A => A)) { actor ! action }
      def close() = { Invoker.system.stop(actor) }
    }

  }

  private class Agent[A](var a: A) extends Actor {
    def receive = {
      case action: Function1[_, _] => a = action.asInstanceOf[Function1[A, A]](a)
    }
  }

}

