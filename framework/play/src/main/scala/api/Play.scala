package play.api

import play.core._

import play.api.mvc._

import java.io._

import scala.collection.JavaConverters._

object Play {
    
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
    
    def configuration = application.configuration
    
}

trait Content {
    def body:String
    def contentType:String
}
