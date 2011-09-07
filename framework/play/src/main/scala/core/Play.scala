package play.core

import java.io._

import play.core._
import play.core.logger._

import play.api._

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
}

class StaticApplication(applicationPath:File) extends ApplicationProvider {
    val application = Application(applicationPath, classOf[StaticApplication].getClassLoader, NoSourceAvailable())
    def get = Right(application)
    def path = applicationPath
}

abstract class ReloadableApplication(applicationPath:File) extends ApplicationProvider {
    
    Logger.log("Running the application from SBT, auto-reloading is enabled")
    
    var application:Application = _
    def get = {
        reload.right.map { maybeClassloader =>
            maybeClassloader.map { classloader =>
                Application(applicationPath, classloader, new SourceMapper {
                    def sourceOf(className:String) = findSource(className)
                })
            }.map { newApp =>
                application = newApp
                newApp
            }.getOrElse(application)
        }
    }
    def reload:Either[PlayException,Option[ClassLoader]]
    def path = applicationPath
    def findSource(className:String):Option[File]
}
