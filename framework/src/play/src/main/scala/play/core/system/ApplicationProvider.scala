package play.core

import java.io._
import java.net._

import akka.dispatch.Future
import akka.dispatch.Await
import akka.util.duration._

import play.api._
import play.api.mvc._

/**
 * provides source code to be displayed on error pages
 */
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

/**
 * generic layout for initialized Applications
 */
trait ApplicationProvider {
  def path: File
  def get: Either[Throwable, Application]
  def handleWebCommand(requestHeader: play.api.mvc.RequestHeader): Option[Result] = None
}

/**
 * creates and initializes an Application
 * @param applicationPath location of an Application
 */
class StaticApplication(applicationPath: File) extends ApplicationProvider {

  val application = new Application(applicationPath, this.getClass.getClassLoader, None, Mode.Prod)

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
 * generic interface that helps the communication between a Play Application
 * and the underlying SBT infrastructre
 */
trait SBTLink {
  def reload: Either[Throwable, Option[ClassLoader]]
  def findSource(className: String): Option[File]
  def projectPath: File
  def runTask(name: String): Option[Any]
  def forceReload()
  def definedTests: Seq[String]
  def runTests(only: Seq[String], callback: Any => Unit): Either[String, Boolean]
  def markdownToHtml(markdown: String, link: String => (String, String)): String
}

/**
 * represents an application that can be reloaded in Dev Mode
 */
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

      implicit def dispatcher = play.core.Invoker.system.dispatcher

      Await.result(Future {

        sbtLink.reload.right.flatMap { maybeClassLoader =>

          val maybeApplication: Option[Either[Throwable, Application]] = maybeClassLoader.map { classloader =>
            try {

              if (lastState.isRight) {
                println()
                println(play.utils.Colors.magenta("--- (RELOAD) ---"))
                println()
              }

              val newApplication = new Application(path, classloader, Some(new SourceMapper {
                def sourceOf(className: String) = sbtLink.findSource(className)
              }), Mode.Dev)

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

      }, 5.minutes)

    }
  }

  override def handleWebCommand(request: play.api.mvc.RequestHeader): Option[Result] = {

    import play.api.mvc.Results._

    val applyEvolutions = """/@evolutions/apply/([a-zA-Z0-9_]+)""".r
    val resolveEvolutions = """/@evolutions/resolve/([a-zA-Z0-9_]+)/([0-9]+)""".r

    val documentation = """/@documentation""".r
    val book = """/@documentation/Book""".r
    val apiDoc = """/@documentation/api/(.*)""".r
    val wikiResource = """/@documentation/resources/(.*)""".r
    val wikiPage = """/@documentation/([^/]*)""".r

    val documentationHome = Option(System.getProperty("play.home")).map(ph => new java.io.File(ph + "/../documentation"))

    request.path match {

      case applyEvolutions(db) => {

        import play.api.db._
        import play.api.db.evolutions._

        Some {
          OfflineEvolutions.applyScript(path, Play.current.classloader, db)
          sbtLink.forceReload()
          Redirect(request.queryString.get("redirect").filterNot(_.isEmpty).map(_(0)).getOrElse("/"))
        }
      }

      case resolveEvolutions(db, rev) => {

        import play.api.db._
        import play.api.db.evolutions._

        Some {
          OfflineEvolutions.resolve(path, Play.current.classloader, db, rev.toInt)
          sbtLink.forceReload()
          Redirect(request.queryString.get("redirect").filterNot(_.isEmpty).map(_(0)).getOrElse("/"))
        }
      }

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
            val pages = Path(book).slurpString.split('\n').toSeq.map(_.trim)
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

              val linkRender: (String => (String, String)) = _ match {
                case link if link.contains("|") => {
                  val parts = link.split('|')
                  (parts.tail.head, parts.head)
                }
                case image if image.endsWith(".png") => {
                  val link = image match {
                    case full if full.startsWith("http://") => full
                    case absolute if absolute.startsWith("/") => "resources/manual" + absolute
                    case relative => "resources/" + pageSource.parent.get.relativize(Path(documentationHome.get)).path + "/" + relative
                  }
                  (link, """<img src="""" + link + """"/>""")
                }
                case link => {
                  (link, link)
                }
              }

              Ok(
                views.html.play20.manual(
                  page,
                  Some(sbtLink.markdownToHtml(pageSource.slurpString, linkRender)),
                  maybeSidebar.map(s => sbtLink.markdownToHtml(s.slurpString, linkRender))
                )
              )
            }
          }.getOrElse {
            NotFound(views.html.play20.manual(page, None, None))
          }

        }

      }

      case _ => None

    }
  }
}

