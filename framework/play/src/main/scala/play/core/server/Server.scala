package play.core.server

import play.api._
import play.core._
import play.api.mvc._

trait Server {

  // First delete the default log file for a fresh start
  scalax.file.Path(new java.io.File(applicationProvider.path, "logs/application.log")).delete()

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

  def getActionFor(request: RequestHeader): Either[Result, (Action[_], Application)] = {
    def sendAction: Either[Throwable, (Action[_], Application)] =
      applicationProvider.get.right.map { application =>
        val maybeAction = application.global.onRouteRequest(request)
        (maybeAction.getOrElse(Action(_ => application.global.onActionNotFound(request))), application)
      }

    import scala.util.control.Exception
    applicationProvider.handleWebCommand(request).toLeft {
      Exception.allCatch[Either[Throwable, (Action[Any], Application)]]
        .either(sendAction)
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

  def applicationProvider: ApplicationProvider

}
