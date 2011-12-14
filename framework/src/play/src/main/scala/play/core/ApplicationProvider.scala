package play.core

import play.api._
import play.api.mvc._

import java.io._
import java.net._

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
  def get: Either[Throwable, Application]
  def handleWebCommand(requestHeader: play.api.mvc.RequestHeader): Option[Result] = None
}

class StaticApplication(applicationPath: File) extends ApplicationProvider {
  val application = Application(applicationPath, classOf[StaticApplication].getClassLoader, None, Play.Mode.Prod)

  Play.start(application)

  def get = Right(application)
  def path = applicationPath
}

trait SBTLink {
  def reload: Either[Throwable, Option[Array[java.net.URL]]]
  def findSource(className: String): Option[File]
  def projectPath: File
  def runTask(name: String): Option[Any]
  def forceReload()
  def definedTests: Seq[String]
  def runTests(only: Seq[String], callback: Any => Unit): Either[String, Boolean]
}

class ReloadableApplication(sbtLink: SBTLink) extends ApplicationProvider {

  lazy val path = sbtLink.projectPath

  println(play.utils.Colors.magenta("--- (Running the application from SBT, auto-reloading is enabled) ---"))
  println()

  var lastState: Either[Throwable, Application] = Left(PlayException("Not initialized", "?"))

  def get = {

    synchronized {

      // Let's load the application on another thread
      // since we are still on the Netty IO thread.
      //
      // Because we are on DEV mode here, it doesn't really matter
      // but it's more coherent with the way it works in PROD mode.
      akka.dispatch.Future({

        sbtLink.reload.right.flatMap { maybeClasspath =>

          val maybeApplication: Option[Either[Throwable, Application]] = maybeClasspath.map { classpath =>
            try {

              if (lastState.isRight) {
                println()
                println(play.utils.Colors.magenta("--- (RELOAD) ---"))
                println()
              }

              val classloader = new java.net.URLClassLoader(classpath, this.getClass.getClassLoader)

              val newApplication = Application(path, classloader, Some(new SourceMapper {
                def sourceOf(className: String) = sbtLink.findSource(className)
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

  override def handleWebCommand(request: play.api.mvc.RequestHeader): Option[Result] = {

    import play.api.mvc.Results._

    val applyEvolutions = """/@evolutions/apply/([a-zA-Z0-9_]+)""".r
    val testPath = """/@tests""".r
    val runTestPath = """/@run-test""".r

    request.path match {

      case applyEvolutions(db) => {
        import play.api.db._
        import play.api.db.evolutions._

        OfflineEvolutions.applyScript(path, Play.current.classloader, db)

        sbtLink.forceReload()

        Some(Redirect(request.queryString.get("redirect").filterNot(_.isEmpty).map(_(0)).getOrElse("/")))
      }

      case testPath() => {

        val r = <ul>
                  { sbtLink.definedTests.map(name => <li><a href={ "/@run-test?className=" + name }>{ name }</a></li>) }
                </ul>

        Some(Ok(r).as("text/html"))

      }

      case runTestPath() => {

        val classNames = request.queryString.get("className").getOrElse(Seq.empty)

        Some({
          sbtLink.runTests(classNames, _ => ()).fold(
            msg => InternalServerError("Test failed... " + msg),
            _ => Ok("Test successful!"))
        })

      }

      case _ => None

    }

  }

}
