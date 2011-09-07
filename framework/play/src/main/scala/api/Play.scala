package play.api

import play.core._

import play.api.mvc._

import java.io._

case class Application(path:File, classloader:ClassLoader, sources:SourceMapper) {
    
    def actionFor(request:Request):Option[Action] = {
        import java.lang.reflect._
        
        try {
            classloader.loadClass("Routes").getDeclaredMethod("actionFor", classOf[Request]).invoke(null, request).asInstanceOf[Option[Action]]
        } catch {
            case e:InvocationTargetException if e.getTargetException.isInstanceOf[PlayException] => throw e.getTargetException
            case e:InvocationTargetException => {
                throw ExecutionException(e.getTargetException, sources.sourceFor(e.getTargetException))
            }
            case e => throw ExecutionException(e, sources.sourceFor(e))
        }
        
    }
    
    def resourceAsStream(name:String):Option[InputStream] = {
        Option(classloader.getResourceAsStream(Option(name).map {
            case s if s.startsWith("/") => s.drop(1)
            case s => s
        }.get))
    }
    
}