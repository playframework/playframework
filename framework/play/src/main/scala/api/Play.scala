package play.api

import play.core._

import play.api.mvc._

import java.io._

import scala.collection.JavaConverters._

object Play {
    
    object Mode extends Enumeration {
        type Mode = Value
        val Dev, Prod = Value
    }
    
    private[play] var application:Application = _
    
    def start(app:Application) {
        
        // First stop previous app if exists
        Option(application).map { 
            _.plugins.foreach { p =>
                try { p.onStop } catch { case _ => }
            }
        }
        
        Play.application = app
        
        app.plugins.foreach(_.onStart)
        
    }
    
    def resourceAsStream(name:String):Option[InputStream] = {
        Option(application.classloader.getResourceAsStream(Option(name).map {
            case s if s.startsWith("/") => s.drop(1)
            case s => s
        }.get))
    }
    
    def configuration   = application.configuration
    def routes          = application.routes
    def mode            = application.mode
    
    def isDev = application.mode == Play.Mode.Dev
    def isProd = application.mode == Play.Mode.Prod
    
}

trait GlobalSettings {
    
    import Results._
    
    def onStart {
    }
    
    def onStop {
    }
        
    def onRouteRequest(request:Request):Option[Action] = Play.application.routes.flatMap { router =>
        router.actionFor(request)
    }
    
    def onError(ex:Throwable):Result = {
        InternalServerError(Option(Play.application).map {
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
        NotFound(Option(Play.application).map {
            case app if app.mode == Play.Mode.Dev => core.views.html.devNotFound.f
            case app => core.views.html.notFound.f
        }.getOrElse(core.views.html.devNotFound.f)(request, Option(Play.application).flatMap(_.routes)))
    }
    
}

object DefaultGlobal extends GlobalSettings

trait Content {
    def body:String
    def contentType:String
}
