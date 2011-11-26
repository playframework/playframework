package play.core.server

import play.api._
import play.core._
import play.api.mvc._
import play.api.libs.iteratee._

trait Server {

  // First delete the default log file for a fresh start
  try {
    scalax.file.Path(new java.io.File(applicationProvider.path, "logs/application.log")).delete()
  } catch {
    case _ =>
  }

  // Configure the logger for the first time
  Logger.configure(
    Option(new java.io.File(applicationProvider.path, "conf/logger.xml")).filter(_.exists).map(_.toURI.toURL).getOrElse {
      this.getClass.getClassLoader.getResource("conf/logger.xml")
    },
    Map("application.home" -> applicationProvider.path.getAbsolutePath))

  import akka.actor._
  import akka.actor.Actor._
  import akka.routing.Routing._
  import akka.routing.SmallestMailboxFirstIterator
  import akka.config._
  import akka.config.Supervision._

  def newInvoker = { val inv = actorOf[Invoker]; inv.start(); inv }

  val invoker = loadBalancerActor(new SmallestMailboxFirstIterator(List.fill(3)(newInvoker))).start()

  def getHandlerFor(request: RequestHeader): Either[Result, (Handler, Application)] = {
    def sendHandler: Either[Throwable, (Handler, Application)] =
      applicationProvider.get.right.map { application =>
        val maybeAction = application.global.onRouteRequest(request)
        (maybeAction.getOrElse(Action(_ => application.global.onHandlerNotFound(request))), application)
      }

    import scala.util.control.Exception
    applicationProvider.handleWebCommand(request).toLeft {
      Exception.allCatch[Either[Throwable, (Handler, Application)]]
        .either(sendHandler)
        .joinRight
        .left.map { e =>

          Logger.error(
            """
            |
            |! %sInternal server error, for request [%s] ->
            |""".stripMargin.format(e match {
              case p: PlayException => "@" + p.id + " - "
              case _ => ""
            }, request),
            e)

          DefaultGlobal.onError(request, e)
        }
    }.joinRight

  }

  def invoke[A](request: Request[A], response: Response, action: Action[A], app: Application) = invoker ! HandleAction(request, response, action, app)

  import play.api.libs.concurrent._
  def getBodyParser[A](requestHeaders: RequestHeader, bodyFunction: BodyParser[A]): Promise[Iteratee[Array[Byte], A]] = {
    new AkkaPromise((invoker ? ((requestHeaders, bodyFunction))).map(_.asInstanceOf[Iteratee[Array[Byte], A]]))

  }

  def applicationProvider: ApplicationProvider

}
