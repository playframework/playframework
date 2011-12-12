package play.core

import play.api._
import play.api.mvc._

import java.io._
import java.net._

class ApplicationClassLoader(parent: ClassLoader, urls: Array[URL] = Array.empty) extends URLClassLoader(urls, parent) {

  def loadClassParentLast(name: String) = try {
    findClass(name)
  } catch {
    case e => loadClass(name)
  }

}

trait SourceMapper {

  def sourceOf(className: String): Option[File]

  def sourceFor(e: Throwable): Option[(File, Int)] = {
    e.getStackTrace.find(element => sourceOf(element.getClassName).isDefined).map { interestingStackTrace =>
      sourceOf(interestingStackTrace.getClassName).get -> interestingStackTrace.getLineNumber
    }.map {
      case (source, line) => {
        play.templates.MaybeGeneratedSource.unapply(source).map { generatedSource =>
          generatedSource.source.get -> generatedSource.mapLine(line)
        }.getOrElse(source -> line)
      }
    }
  }

}

trait ApplicationProvider {
  def path: File
  def get: Either[PlayException, Application]
  def handleWebCommand(requestHeader: play.api.mvc.RequestHeader): Option[Result] = None
}

class StaticApplication(applicationPath: File) extends ApplicationProvider {
  val application = Application(applicationPath, new ApplicationClassLoader(classOf[StaticApplication].getClassLoader), None, Play.Mode.Prod)

  Play.start(application)

  def get = Right(application)
  def path = applicationPath
}

abstract class ReloadableApplication(applicationPath: File) extends ApplicationProvider {

  println()
  println(play.console.Colors.magenta("--- (Running the application from SBT, auto-reloading is enabled) ---"))

  var lastState: Either[PlayException, Application] = Left(PlayException("Not initialized", "?"))

  def get = {

    synchronized {

      // Let's load the application on another thread
      // as we are now on the Netty IO thread.
      //
      // Because we are on DEV mode here, it doesn't really matter
      // but it's more coherent with the way it works in PROD mode.
      akka.dispatch.Future({

        reload.right.flatMap { maybeClassloader =>

          val maybeApplication: Option[Either[PlayException, Application]] = maybeClassloader.map { classloader =>
            try {

              if (lastState.isRight) {
                println()
                println(play.console.Colors.magenta("--- (RELOAD) ---"))
                println()
              }

              val newApplication = Application(applicationPath, classloader, Some(new SourceMapper {
                def sourceOf(className: String) = findSource(className)
              }), Play.Mode.Dev)

              Play.start(newApplication)

              Right(newApplication)
            } catch {
              case e: PlayException => {
                lastState = Left(e)
                lastState
              }
              case e => {
                lastState = Left(UnexpectedException(unexpected = Some(e)))
                lastState
              }
            }
          }

          maybeApplication.flatMap(_.right.toOption).foreach { app =>
            lastState = Right(app)
          }

          maybeApplication.getOrElse(lastState)
        }

      }, 60000).get

    }
  }

  def path = applicationPath

  // Abstract
  def reload: Either[PlayException, Option[ApplicationClassLoader]]
  def findSource(className: String): Option[File]

}
