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

case class Application(path: File, classloader: ApplicationClassLoader, sources: SourceMapper, mode: Play.Mode.Mode) {

  val global: GlobalSettings = try {
    classloader.loadClassParentLast("Global$").getDeclaredField("MODULE$").get(null).asInstanceOf[GlobalSettings]
  } catch {
    case e: ClassNotFoundException => DefaultGlobal
    case e => throw e
  }

  global.beforeStart(this)

  val routes: Option[Router.Routes] = try {
    Some(classloader.loadClassParentLast("Routes$").getDeclaredField("MODULE$").get(null).asInstanceOf[Router.Routes])
  } catch {
    case e: ClassNotFoundException => None
    case e => throw e
  }

  val configuration = Configuration.fromFile(new File(path, "conf/application.conf"))

  val plugins: Map[Class[_], Plugin] = {

    import scalax.file._
    import scalax.io.Input.asInputConverter

    import scala.collection.JavaConverters._

    val PluginDeclaration = """([0-9_]+):(.*)""".r

    classloader.getResources("play.plugins").asScala.toList.distinct.map { plugins =>
      plugins.asInput.slurpString.split("\n").map(_.trim).filterNot(_.isEmpty).map {
        case PluginDeclaration(priority, className) => {
          try {
            Integer.parseInt(priority) -> classloader.loadClass(className).getConstructor(classOf[Application]).newInstance(this).asInstanceOf[Plugin]
          } catch {
            case e: java.lang.NoSuchMethodException => {
              try {
                Integer.parseInt(priority) -> classloader.loadClass(className).getConstructor(classOf[play.Application]).newInstance(new play.Application(this)).asInstanceOf[Plugin]
              } catch {
                case e => throw PlayException(
                  "Cannot load plugin",
                  "Plugin [" + className + "] cannot been instantiated.",
                  Some(e))
              }
            }
            case e => throw PlayException(
              "Cannot load plugin",
              "Plugin [" + className + "] cannot been instantiated.",
              Some(e))
          }
        }
      }
    }.flatten.toList.sortBy(_._1).map(_._2).map(p => p.getClass -> p).toMap

  }

  def plugin[T](implicit m: Manifest[T]): T = plugin(m.erasure).asInstanceOf[T]
  def plugin[T](c: Class[T]): T = plugins.get(c).get.asInstanceOf[T]

  def getFile(subPath: String) = new File(path, subPath)

  def getTypesAnnotatedWith[T <: java.lang.annotation.Annotation](packageName: String, annotation: Class[T]): Set[String] = {
    import org.reflections._
    new Reflections(
      new util.ConfigurationBuilder()
        .addUrls(util.ClasspathHelper.forPackage(packageName, classloader))
        .setScanners(new scanners.TypeAnnotationsScanner())).getStore.getTypesAnnotatedWith(annotation.getName).asScala.toSet
  }

}

trait GlobalSettings {

  import Results._

  def beforeStart(app: Application) {
  }

  def onStart(app: Application) {
  }

  def onStop(app: Application) {
  }

  def onRouteRequest(request: RequestHeader): Option[Action[_]] = Play._currentApp.routes.flatMap { router =>
    router.actionFor(request)
  }

  def onError(ex: Throwable): Result = {
    InternalServerError(Option(Play._currentApp).map {
      case app if app.mode == Play.Mode.Dev => html.views.defaultpages.devError.f
      case app => html.views.defaultpages.error.f
    }.getOrElse(html.views.defaultpages.devError.f) {
      ex match {
        case e: PlayException => e
        case e => UnexpectedException(unexpected = Some(e))
      }
    })
  }

  def onActionNotFound(request: RequestHeader): Result = {
    NotFound(Option(Play._currentApp).map {
      case app if app.mode == Play.Mode.Dev => html.views.defaultpages.devNotFound.f
      case app => html.views.defaultpages.notFound.f
    }.getOrElse(html.views.defaultpages.devNotFound.f)(request, Option(Play._currentApp).flatMap(_.routes)))
  }

}

trait Content {
  def body: String
  def contentType: String
}
