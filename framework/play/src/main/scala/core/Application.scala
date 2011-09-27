package play.core

import play.api._
import play.api.mvc._

import play.core.logger._

import java.io._
import java.net._

class ApplicationClassLoader(parent:ClassLoader, urls:Array[URL] = Array.empty) extends URLClassLoader(urls, parent) {
    
    def loadClassParentLast(name:String) = try {
        findClass(name)
    } catch {
        case e => loadClass(name)
    }
    
}

case class Application(path:File, classloader:ApplicationClassLoader, sources:SourceMapper, mode:Play.Mode.Mode) {
    
    val global:GlobalSettings = try {
        classloader.loadClassParentLast("Global$").getDeclaredField("MODULE$").get(null).asInstanceOf[GlobalSettings]
    } catch {
        case e:ClassNotFoundException => DefaultGlobal
        case e => throw e
    }
    
    val routes:Option[Router.Routes] = try {
        Some(classloader.loadClassParentLast("Routes$").getDeclaredField("MODULE$").get(null).asInstanceOf[Router.Routes])
    } catch {
        case e:ClassNotFoundException => None
        case e => throw e
    }
    
    val configuration = Configuration.fromFile(new File(path, "conf/application.conf"))
    
    val plugins:Map[Class[_],Plugin] = {
        
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
                        case e => throw PlayException(
                            "Cannot load plugin",
                            "Plugin [" + className + "] cannot been instantiated.",
                            Some(e)
                        )
                    }
                }
            }
        }.flatten.toList.sortBy(_._1).map(_._2).map(p => p.getClass -> p).toMap
        
    }
    
    def plugin[T](implicit m:Manifest[T]):T = plugin(m.erasure).asInstanceOf[T]
    def plugin[T](c:Class[T]):T = plugins.get(c).get.asInstanceOf[T]
    
    def getFile(subPath:String) = new File(path, subPath)
    
}

trait SourceMapper {
    
    def sourceOf(className:String):Option[File]
    
    def sourceFor(e:Throwable):Option[(File,Int)] = {
        e.getStackTrace.find(element => sourceOf(element.getClassName).isDefined).map { interestingStackTrace =>
            sourceOf(interestingStackTrace.getClassName).get -> interestingStackTrace.getLineNumber
        }.map {
            case (source,line) => {
                play.templates.MaybeGeneratedSource.unapply(source).map { generatedSource =>
                    generatedSource.source.get -> generatedSource.mapLine(line)
                }.getOrElse(source -> line)
            }
        }
    }
    
}

case class NoSourceAvailable() extends SourceMapper {
    def sourceOf(className:String) = None
}

trait ApplicationProvider {
    def path:File
    def get:Either[PlayException,Application]
    def handleWebCommand(requestHeader:play.api.mvc.RequestHeader):Option[Result] = None
}

class StaticApplication(applicationPath:File) extends ApplicationProvider {
    val application = Application(applicationPath, new ApplicationClassLoader(classOf[StaticApplication].getClassLoader), NoSourceAvailable(), Play.Mode.Prod)
    
    Play.start(application)
    
    def get = Right(application)
    def path = applicationPath
}

abstract class ReloadableApplication(applicationPath:File) extends ApplicationProvider {
    
    Logger.log("Running the application from SBT, auto-reloading is enabled")
    
    var lastState:Either[PlayException,Application] = Left(PlayException("Not initialized", "?"))
    
    def get = {
        
        synchronized {
            
            reload.right.flatMap { maybeClassloader =>
            
                val maybeApplication:Option[Either[PlayException,Application]] = maybeClassloader.map { classloader =>
                    try {
                    
                        val newApplication = Application(applicationPath, classloader, new SourceMapper {
                            def sourceOf(className:String) = findSource(className)
                        }, Play.Mode.Dev)
                    
                        Play.start(newApplication)
                    
                        Right(newApplication)
                    } catch {
                        case e:PlayException => {
                            lastState = Left(e)
                            lastState
                        }
                        case e => {
                            lastState = Left(UnexpectedException(unexpected=Some(e)))
                            lastState
                        }
                    }
                }
            
                maybeApplication.flatMap(_.right.toOption).foreach { app => 
                    lastState = Right(app)
                }
                
                maybeApplication.getOrElse(lastState)
            }
        
        }
    }
    def reload:Either[PlayException,Option[ApplicationClassLoader]]
    def path = applicationPath
    def findSource(className:String):Option[File]

}
