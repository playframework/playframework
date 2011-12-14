package controllers

import play.api._
import play.api.mvc._
import play.api.libs._

import Play.current

import java.io._
import scalax.io.{ Resource }

/**
 * Controller that serves static resources.
 *
 * You can use this controller in any application, just be declaring the appropriate route. For example:
 * {{{
 * GET     /assets/\uFEFF*file               controllers.Assets.at(path="/public", file)
 * }}}
 */
object Assets extends Controller {

  /**
   * Generates an `Action` that serves a static resource.
   *
   * @param path the root folder for searching the static resource files, such as `"/public"`
   * @param file the file part extracted from the URL
   */
  def at(path: String, file: String) = Action { request =>

    val resourceName = Option(path + "/" + file).map(name => if (name.startsWith("/")) name else ("/" + name)).get
    val resourceStream = Play.resourceAsStream(resourceName)

    resourceStream.map { is =>

      // TODO replace by an Enumerator
      lazy val resourceData = Resource.fromInputStream(is).byteArray

      request.headers.get(IF_NONE_MATCH).filter(_ == etagFor(resourceName, resourceData)).map(_ => NotModified).getOrElse {

        val response = Ok(resourceData)
          .as(MimeTypes.forFileName(file).getOrElse(BINARY))
          .withHeaders(ETAG -> etagFor(resourceName, resourceData))

        Play.configuration.getString("assets.cache." + resourceName).map { cacheControl =>
          response.withHeaders(CACHE_CONTROL -> cacheControl)
        }.getOrElse(response)

      }

    }.getOrElse(NotFound)

  }

  // -- ETags handling

  private val etags = scala.collection.mutable.HashMap.empty[String, String]

  private def etagFor(resourceName: String, data: => Array[Byte]) = {
    etags.get(resourceName).filter(_ => Play.isProd).getOrElse {
      etags.put(resourceName, computeETag(data))
      etags(resourceName)
    }
  }

  private def computeETag(data: Array[Byte]) = Codecs.sha1(data)

}

