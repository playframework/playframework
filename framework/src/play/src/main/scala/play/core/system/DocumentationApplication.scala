package play.core.system

import play.api.mvc._
import java.io.File
import play.core._
import play.api._
import play.api.libs.concurrent.Execution
import libs.iteratee.Iteratee
import com.typesafe.config.ConfigFactory
import server.NettyServer

/**
 * An application that exists purely for documentation
 */
class DocumentationHandler(markdownRenderer: (String, String, File) => String) {

  def maybeHandleDocumentationRequest(request: RequestHeader): Option[SimpleResult] = {

    import play.api.mvc.Results._

    val documentation = """/@documentation/?""".r
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
              val home = documentationHome.get
              val relativePath = pageSource.parent.get.relativize(Path(home)).path
              Ok(
                views.html.play20.manual(
                  page,
                  Some(markdownRenderer(pageSource.string, relativePath, home)),
                  maybeSidebar.map(s => markdownRenderer(s.string, relativePath, home))
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

/**
 * Provides a very simple application that does nothing but renders documentation
 */
case class DocumentationApplication(sbtLink: SBTLink) extends ApplicationProvider {
  val documentationHandler = new DocumentationHandler(sbtLink.markdownToHtml _)

  val application = new Application with WithDefaultConfiguration {
    def path = sbtLink.projectPath()
    def classloader = this.getClass.getClassLoader
    def sources = None
    def mode = Mode.Dev
    def global = new GlobalSettings() {}
    def plugins = Nil
    override lazy val routes = None
  }

  Play.start(application)

  def path = sbtLink.projectPath()
  def get = Right(application)

  override def handleWebCommand(request: RequestHeader) =
    documentationHandler.maybeHandleDocumentationRequest(request).orElse(
      Some(Results.Redirect("/@documentation"))
    )
}

class DocumentationServer(sbtLink: SBTLink, port: java.lang.Integer) extends NettyServer(DocumentationApplication(sbtLink), Some(port),
  mode = Mode.Dev
)
