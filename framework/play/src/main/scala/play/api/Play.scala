package play.api

import play.core._

import play.api.mvc._

import java.io._

import scala.collection.JavaConverters._

object Play {

  def unsafeApplication = _currentApp
  def maybeApplication = Option(_currentApp)
  implicit def currentApplication = maybeApplication.get

  object Mode extends Enumeration {
    type Mode = Value
    val Dev, Prod = Value
  }

  private[play] var _currentApp: Application = _

  def start(app: Application) {

    // First stop previous app if exists
    Option(_currentApp).map {
      _.plugins.values.foreach { p =>
        try { p.onStop } catch { case _ => }
      }
    }

    _currentApp = app

    println("Application has restarted")

    app.plugins.values.foreach(_.onStart)

  }

  def resourceAsStream(name: String)(implicit app: Application): Option[InputStream] = {
    Option(app.classloader.getResourceAsStream(Option(name).map {
      case s if s.startsWith("/") => s.drop(1)
      case s => s
    }.get))
  }

  def getFile(subPath: String)(implicit app: Application) = app.getFile(subPath)

  def application(implicit app: Application) = app
  def classloader(implicit app: Application) = app.classloader
  def configuration(implicit app: Application) = app.configuration
  def routes(implicit app: Application) = app.routes
  def mode(implicit app: Application) = app.mode

  def isDev(implicit app: Application) = app.mode == Play.Mode.Dev
  def isProd(implicit app: Application) = app.mode == Play.Mode.Prod

}
