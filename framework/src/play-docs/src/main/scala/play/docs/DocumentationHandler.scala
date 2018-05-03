/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.docs

import java.io.Closeable

import akka.stream.scaladsl.StreamConverters
import play.api.http._
import play.api.mvc._
import play.core.{ BuildDocHandler, PlayVersion }
import play.doc._

/**
 * Used by the DocumentationApplication class to handle requests for Play documentation.
 * Documentation is located in the given repository - either a JAR file or directly from
 * the filesystem.
 */
class DocumentationHandler(repo: FileRepository, apiRepo: FileRepository, toClose: Closeable) extends BuildDocHandler with Closeable {

  def this(repo: FileRepository, toClose: Closeable) = this(repo, repo, toClose)
  def this(repo: FileRepository, apiRepo: FileRepository) = this(repo, apiRepo, new Closeable() { def close() = () })
  def this(repo: FileRepository) = this(repo, repo)

  private val fileMimeTypes: FileMimeTypes = {
    val mimeTypesConfiguration = FileMimeTypesConfiguration(Map(
      "html" -> "text/html",
      "css" -> "text/css",
      "png" -> "image/png",
      "js" -> "application/javascript",
      "ico" -> "application/javascript",
      "jpg" -> "image/jpeg",
      "ico" -> "ico=image/x-icon"
    ))
    new DefaultFileMimeTypes(mimeTypesConfiguration)
  }

  /**
   * This is a def because we want to reindex the docs each time.
   */
  def playDoc = {
    new PlayDoc(
      markdownRepository = repo,
      codeRepository = repo,
      resources = "resources",
      playVersion = PlayVersion.current,
      pageIndex = PageIndex.parseFrom(repo, "Home", Some("manual")),
      new TranslatedPlayDocTemplates("Next"),
      pageExtension = None
    )
  }

  val locator: String => String = new Memoise(name =>
    repo.findFileWithName(name).orElse(apiRepo.findFileWithName(name)).getOrElse(name)
  )

  // Method without Scala types. Required by BuildDocHandler to allow communication
  // between code compiled by different versions of Scala
  override def maybeHandleDocRequest(request: AnyRef): AnyRef = {
    this.maybeHandleDocRequest(request.asInstanceOf[RequestHeader])
  }

  /**
   * Handle the given request if it is a request for documentation content.
   */
  def maybeHandleDocRequest(request: RequestHeader): Option[Result] = {

    // Assumes caller consumes result, closing entry
    def sendFileInline(repo: FileRepository, path: String): Option[Result] = {
      repo.handleFile(path) { handle =>
        Results.Ok.sendEntity(HttpEntity.Streamed(
          StreamConverters.fromInputStream(() => handle.is).mapMaterializedValue(_ => handle.close),
          Some(handle.size),
          fileMimeTypes.forFileName(handle.name).orElse(Some(ContentTypes.BINARY))
        ))
      }
    }

    import play.api.mvc.Results._

    val documentation = """/@documentation/?""".r
    val apiDoc = """/@documentation/api/(.*)""".r
    val wikiResource = """/@documentation/resources/(.*)""".r
    val wikiPage = """/@documentation/([^/]*)""".r

    request.path match {

      case documentation() => Some(Redirect("/@documentation/Home"))
      case apiDoc(page) => Some(
        sendFileInline(apiRepo, "api/" + page)
          .getOrElse(NotFound(views.html.play20.manual(page, None, None, locator)))
      )
      case wikiResource(path) => Some(
        sendFileInline(repo, path).orElse(sendFileInline(apiRepo, path))
          .getOrElse(NotFound("Resource not found [" + path + "]"))
      )
      case wikiPage(page) => Some(
        playDoc.renderPage(page) match {
          case None => NotFound(views.html.play20.manual(page, None, None, locator))
          case Some(RenderedPage(mainPage, None, _, _)) => Ok(views.html.play20.manual(page, Some(mainPage), None, locator))
          case Some(RenderedPage(mainPage, Some(sidebar), _, _)) => Ok(views.html.play20.manual(page, Some(mainPage), Some(sidebar), locator))
        }
      )
      case _ => None
    }
  }

  def close() = toClose.close()
}

/**
 * Memoise a function.
 */
class Memoise[-T, +R](f: T => R) extends (T => R) {
  private[this] val cache = scala.collection.mutable.Map.empty[T, R]
  def apply(v: T): R = synchronized { cache.getOrElseUpdate(v, f(v)) }
}
