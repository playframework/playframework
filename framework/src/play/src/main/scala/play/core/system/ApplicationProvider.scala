package play.core

import java.io._
import java.net._

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._

import play.api._
import play.api.mvc._

/**
 * provides source code to be displayed on error pages
 */
trait SourceMapper {

  def sourceOf(className: String, line: Option[Int] = None): Option[(File, Option[Int])]

  def sourceFor(e: Throwable): Option[(File, Option[Int])] = {
    e.getStackTrace.find(element => sourceOf(element.getClassName).isDefined).flatMap { interestingStackTrace =>
      sourceOf(interestingStackTrace.getClassName, Option(interestingStackTrace.getLineNumber))
    }
  }

}

/**
 * generic layout for initialized Applications
 */
trait ApplicationProvider {
  def path: File
  def get: Either[Throwable, Application]
  def handleWebCommand(requestHeader: play.api.mvc.RequestHeader): Option[Result] = None
}

trait HandleWebCommandSupport {
  def handleWebCommand(request: play.api.mvc.RequestHeader, sbtLink: play.core.SBTLink, path: java.io.File): Option[Result]
}

/**
 * creates and initializes an Application
 * @param applicationPath location of an Application
 */
class StaticApplication(applicationPath: File) extends ApplicationProvider {

  val application = new DefaultApplication(applicationPath, this.getClass.getClassLoader, None, Mode.Prod)

  Play.start(application)

  def get = Right(application)
  def path = applicationPath
}

/**
 * wraps and starts a fake application (used in tests)
 * @param application fake Application
 */
class TestApplication(application: Application) extends ApplicationProvider {

  Play.start(application)

  def get = Right(application)
  def path = application.path
}

/**
 * represents an application that can be reloaded in Dev Mode
 */
class ReloadableApplication(sbtLink: SBTLink) extends ApplicationProvider {

  // Use plain Java call here in case of scala classloader mess
  {
    if(System.getProperty("play.debug.classpath") == "true") {
      System.out.println("\n---- Current ClassLoader ----\n")
      System.out.println(this.getClass.getClassLoader) 
      System.out.println("\n---- The where is Scala? test ----\n")
      System.out.println(this.getClass.getClassLoader.getResource("scala/Predef$.class"))
    }
  }

  lazy val path = sbtLink.projectPath

  println(play.utils.Colors.magenta("--- (Running the application from SBT, auto-reloading is enabled) ---"))
  println()

  var lastState: Either[Throwable, Application] = Left(new PlayException("Not initialized", "?"))

  def get = {

    synchronized {

        val reloaded = sbtLink.reload match {
          case t: Throwable => Left(t)
          case cl: ClassLoader => Right(Some(cl))
          case null => Right(None)
        }

        reloaded.right.flatMap { maybeClassLoader =>

          val maybeApplication: Option[Either[Throwable, Application]] = maybeClassLoader.map { projectClassloader =>
            try {

              if (lastState.isRight) {
                println()
                println(play.utils.Colors.magenta("--- (RELOAD) ---"))
                println()
              }

              val reloadable = this

              // First, stop the old application if it exists
              Play.stop()

              val newApplication = new DefaultApplication(reloadable.path, projectClassloader, Some(new SourceMapper {
                def sourceOf(className: String, line: Option[Int]) = {
                  Option(sbtLink.findSource(className, line.map(_.asInstanceOf[java.lang.Integer]).orNull)).flatMap {
                    case Array(file: java.io.File, null) => Some((file, None))
                    case Array(file: java.io.File, line: java.lang.Integer) => Some((file, Some(line)))
                    case _ => None
                  }
                }
              }),Mode.Dev)

              Play.start(newApplication)

              Right(newApplication)
            } catch {
              case e: PlayException => {
                lastState = Left(e)
                lastState
              }
              case e: Exception => {
                lastState = Left(UnexpectedException(unexpected = Some(e)))
                lastState
              }
              case e: LinkageError => {
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

    }
  }

  override def handleWebCommand(request: play.api.mvc.RequestHeader): Option[Result] = {

    import play.api.mvc.Results._

    val documentation = """/@documentation""".r
    val book = """/@documentation/Book""".r
    val apiDoc = """/@documentation/api/(.*)""".r
    val wikiResource = """/@documentation/resources/(.*)""".r
    val wikiPage = """/@documentation/([^/]*)""".r

    val documentationHome = Option(System.getProperty("play.home")).map(ph => new java.io.File(ph + "/../documentation"))

    request.path match {

      case documentation() => {

        Some {
          Redirect("/@documentation/Home")
        }

      }

      case book() => {

        import scalax.file._

        Some {
          documentationHome.flatMap { home =>
            Option(new java.io.File(home, "manual/book/Book")).filter(_.exists)
          }.map { book =>
            val pages = Path(book).string.split('\n').toSeq.map(_.trim)
            Ok(views.html.play20.book(pages))
          }.getOrElse(NotFound("Resource not found [Book]"))
        }

      }

      case apiDoc(page) => {

        Some {
          documentationHome.flatMap { home =>
            Option(new java.io.File(home, "api/" + page)).filter(f => f.exists && f.isFile)
          }.map { file =>
            Ok.sendFile(file, inline = true)
          }.getOrElse {
            NotFound(views.html.play20.manual(page, None, None))
          }
        }

      }

      case wikiResource(path) => {

        Some {
          documentationHome.flatMap { home =>
            Option(new java.io.File(home, path)).filter(_.exists)
          }.map { file =>
            Ok.sendFile(file, inline = true)
          }.getOrElse(NotFound("Resource not found [" + path + "]"))
        }

      }

      case wikiPage(page) => {

        import scalax.file._

        Some {

          val pageWithSidebar = documentationHome.flatMap { home =>
            Path(home).descendants().find(_.name == page + ".md").map { pageSource =>

              // Recursively search for Sidebar
              lazy val findSideBar: (Option[Path] => Option[Path]) = _ match {
                case None => None
                case Some(parent) => {
                  val maybeSideBar = parent \ "_Sidebar.md"
                  if (maybeSideBar.exists) {
                    Some(maybeSideBar)
                  } else {
                    findSideBar(parent.parent)
                  }
                }
              }

              pageSource -> findSideBar(pageSource.parent)
            }
          }

          pageWithSidebar.map {
            case (pageSource, maybeSidebar) => {
              val relativePath = pageSource.parent.get.relativize(Path(documentationHome.get)).path
              Ok(
                views.html.play20.manual(
                  page,
                  Some(sbtLink.markdownToHtml(pageSource.string, relativePath)),
                  maybeSidebar.map(s => sbtLink.markdownToHtml(s.string, relativePath))
                )
              )
            }
          }.getOrElse {
            NotFound(views.html.play20.manual(page, None, None))
          }

        }

      }

      // Delegate to plugins
      case _ => Play.maybeApplication.flatMap { app =>
        app.plugins.foldLeft(Option.empty[play.api.mvc.Result]) { 
          case (None, plugin: HandleWebCommandSupport) => plugin.handleWebCommand(request, sbtLink, path)
          case (result, _) => result
        }
      }

    }
  }
}

