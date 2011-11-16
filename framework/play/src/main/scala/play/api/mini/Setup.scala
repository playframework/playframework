package play.api.mini

import play.api.GlobalSettings
import play.api.mvc._
import play.api.mvc.Results._

/**
 * an interface that a mini application needs to implement
 */
trait Application {
  def dispatcher: Map[String, Action[_]]
}

/**
 * provides a simple way to use play as a simple http layer (using scala)
 *
 * example:
 * in the global package name space
 * {{{
 * object Global extends play.api.mini.Setup[com.example.App]
 * }}}
 * and then in your own package
 * {{{
 * class App extends play.api.mini.Application {
 *   def dispatcher: Map[String,Action[_]] =  Map("/coco" -> Action{ Ok(<h1>It works!</h1>).as("text/html") })
 * }
 * }}}
 */
class Setup[T <: Application](implicit m: Manifest[T]) extends GlobalSettings {

  private lazy val routes: Map[String, Action[_]] = {
      val cl = Thread.currentThread().getContextClassLoader()
      try {
        val clazz = m.erasure
        clazz.getMethod("dispatcher").invoke(clazz.newInstance()).asInstanceOf[Map[String, Action[_]]]
      } catch { case (ex: Exception) => throw new Exception("could not find Application:" + ex.toString) }
  }

  override def onRouteRequest(request: RequestHeader): Option[Action[_]] = request match {
    case _ => routes.get(request.path)
  }

}

