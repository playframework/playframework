package play.docs

import play.api.mvc._
import play.api._
import play.api.http.Status
import play.api.http.HeaderNames
import play.api.libs.concurrent.Execution
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Enumeratee
import play.core.{ PlayVersion, SBTDocHandler }
import play.doc.{ FileRepository, PlayDoc }
import org.apache.commons.io.IOUtils

/**
 * Used by the DocumentationApplication class to handle requests for Play documentation.
 * Documentation is located in the given repository - either a JAR file or directly from
 * the filesystem.
 */
class DocumentationHandler(repo: FileRepository, apiRepo: FileRepository) extends SBTDocHandler {

  def this(repo: FileRepository) = this(repo, repo)

  val markdownRenderer = new PlayDoc(repo, repo, "resources", PlayVersion.current)

  // Method without Scala types. Required by SBTDocHandler to allow communication
  // between code compiled by different versions of Scala
  override def maybeHandleDocRequest(request: AnyRef): AnyRef = {
    this.maybeHandleDocRequest(request.asInstanceOf[RequestHeader])
  }

  /**
   * Handle the given request if it is a request for documentation content.
   */
  def maybeHandleDocRequest(request: RequestHeader): Option[SimpleResult] = {

    // Assumes caller consumes result, closing entry
    def sendFileInline(repo: FileRepository, path: String): Option[SimpleResult] = {
      import play.api.libs.concurrent.Execution.Implicits.defaultContext
      repo.handleFile(path) { handle =>
        SimpleResult(
          ResponseHeader(Status.OK, Map(
            HeaderNames.CONTENT_LENGTH -> handle.size.toString,
            HeaderNames.CONTENT_TYPE -> play.api.libs.MimeTypes.forFileName(handle.name).getOrElse(play.api.http.ContentTypes.BINARY)
          )),
          Enumerator.fromStream(handle.is) &> Enumeratee.onIterateeDone(handle.close)
        )
      }
    }

    import play.api.mvc.Results._

    val documentation = """/@documentation/?""".r
    val book = """/@documentation/Book""".r
    val apiDoc = """/@documentation/api/(.*)""".r
    val wikiResource = """/@documentation/resources/(.*)""".r
    val wikiPage = """/@documentation/([^/]*)""".r

    request.path match {

      case documentation() => {

        Some {
          Redirect("/@documentation/Home")
        }

      }

      case book() => {

        import scalax.file._

        Some {
          repo.loadFile("manual/book/Book") { is =>
            val lines = IOUtils.toString(is).split('\n').toSeq.map(_.trim)
            Ok(views.html.play20.book(lines))
          }.getOrElse(NotFound("Resource not found [Book]"))
        }

      }

      case apiDoc(page) => {

        Some {
          sendFileInline(apiRepo, "api/" + page).getOrElse(NotFound(views.html.play20.manual(page, None, None)))
        }

      }

      case wikiResource(path) => {

        Some {
          sendFileInline(repo, path).getOrElse(NotFound("Resource not found [" + path + "]"))
        }

      }

      case wikiPage(page) => {

        Some {
          markdownRenderer.renderPage(page) match {
            case None => NotFound(views.html.play20.manual(page, None, None))
            case Some((mainPage, None)) => Ok(views.html.play20.manual(page, Some(mainPage), None))
            case Some((mainPage, Some(sidebar))) => Ok(views.html.play20.manual(page, Some(mainPage), Some(sidebar)))
          }
        }
      }
      case _ => None
    }
  }
}
