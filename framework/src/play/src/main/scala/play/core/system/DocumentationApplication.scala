package play.core.system

import play.api.mvc._
import play.core._
import play.api._
import play.api.libs.concurrent.Execution
import server.NettyServer

/**
 * An application that exists purely for documentation
 */
class DocumentationHandler(markdownRenderer: (String) => Array[String]) {

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

        Some {
          val results = markdownRenderer(page)
          results match {
            case Array() => NotFound(views.html.play20.manual(page, None, None))
            case Array(mainPage) => Ok(views.html.play20.manual(page, Some(mainPage), None))
            case Array(mainPage, sidebar) => Ok(views.html.play20.manual(page, Some(mainPage), Some(sidebar)))
            case _ => {
              Play.logger.warn("Unexpected result out of markdownRenderer: " + results)
              InternalServerError("Unexpected result out of markdownRenderer")
            }
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
