package play.api

import play.core._

import play.api.mvc._

import java.io._

import scala.collection.JavaConverters._

object Play {
    
    implicit def currentApplication = Option(_currentApp).get
    
    object Mode extends Enumeration {
        type Mode = Value
        val Dev, Prod = Value
    }
    
    private[play] var _currentApp:Application = _
    
    def start(app:Application) {
        
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
    
    def unsafeApplication = _currentApp
    
    def resourceAsStream(name:String)(implicit app:Application):Option[InputStream] = {
        Option(app.classloader.getResourceAsStream(Option(name).map {
            case s if s.startsWith("/") => s.drop(1)
            case s => s
        }.get))
    }
    
    def application(implicit app:Application)     = app    
    def classloader(implicit app:Application)     = app.classloader
    def configuration(implicit app:Application)   = app.configuration
    def routes(implicit app:Application)          = app.routes
    def mode(implicit app:Application)            = app.mode
    
    def isDev(implicit app:Application)           = app.mode == Play.Mode.Dev
    def isProd(implicit app:Application)          = app.mode == Play.Mode.Prod
    
}

trait GlobalSettings {
    
    import Results._
    
    def onStart {
    }
    
    def onStop {
    }
        
    def onRouteRequest(request:Request):Option[Action] = Play._currentApp.routes.flatMap { router =>
        router.actionFor(request)
    }
    
    def onError(ex:Throwable):Result = {
        InternalServerError(Option(Play._currentApp).map {
            case app if app.mode == Play.Mode.Dev => core.views.html.devError.f
            case app => core.views.html.error.f
        }.getOrElse(core.views.html.devError.f)  {
            ex match {
                case e:PlayException => e
                case e => UnexpectedException(unexpected = Some(e))
            }
        })
    }
    
    def onActionNotFound(request:Request):Result = {
        NotFound(Option(Play._currentApp).map {
            case app if app.mode == Play.Mode.Dev => core.views.html.devNotFound.f
            case app => core.views.html.notFound.f
        }.getOrElse(core.views.html.devNotFound.f)(request, Option(Play._currentApp).flatMap(_.routes)))
    }
    
}

object DefaultGlobal extends GlobalSettings

trait Content {
    def body:String
    def contentType:String
}
