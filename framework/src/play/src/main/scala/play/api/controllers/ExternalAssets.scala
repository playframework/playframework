package controllers

import play.api._
import play.api.mvc._
import play.api.libs._
import play.api.libs.iteratee._

import Play.current

import java.io._
import java.net.JarURLConnection
import collection.JavaConverters._

/**
 * Controller that serves static resources from an external folder
 *
 *
 * If a gzipped version of a resource is found (Same resource name with the .gz suffix), it is served instead.
 *
 * The default is to serve all assets with max-age=3600, this can be overwritten in application.conf file via
 *
 * {{{
 * assets.defaultCache = "no-cache"
 * }}}
 *
 * You can also set a custom Cache directive for a particular resource if needed 
 * 
 * For example in your application.conf file:
 *
 * {{{
 * assets.cache./public/images/logo.png = "max-age=5200"
 * }}}
 *
 * You can use this controller in any application, just by declaring the appropriate route. For example:
 * {{{
 * GET     /assets/\uFEFF*file               controllers.ExternalAssets.at(path="/home/peter/myplayapp/external", file)
 * }}}
 */
object ExternalAssets extends Controller {

  /**
   * Generates an `Action` that serves a static resource from an external folder
   *
   * @param absoluteRootPath the root folder for searching the static resource files such as `"/home/peter/public"` 
   * (this can be overwritten in Prod mode by configuring `"assets.production.external.dir"` in application.conf)
   * @param file the file part extracted from the URL
   */
  def at(absoluteRootPath: String, file: String): Action[AnyContent] = Action { request =>
    val root = Play.mode match {
        case Mode.Prod => Play.configuration.getString("assets.production.external.dir").getOrElse(absoluteRootPath)
        case _ => absoluteRootPath
    }    
    val relativeResourcePath = if (file.startsWith("/")) file else "/" + file
    
    val resourceName = root + relativeResourcePath
    
    val fileToServe = new File(resourceName)
    
    val defaultCache = Play.configuration.getString("assets.defaultCache").getOrElse("max-age=3600")
    
    if (fileToServe.isDirectory || !fileToServe.exists) {
      NotFound
    } else {
      val url = {
        val gzippedFileName = resourceName + ".gz"
        val gz = new File(gzippedFileName)
        if (gz.exists && request.headers.get(ACCEPT_ENCODING).map(_.split(',').exists(_ == "gzip")).getOrElse(false)) {
          new java.net.URL("file://"+gzippedFileName)
        } else {
          new java.net.URL("file://"+resourceName)
        }
      }


      lazy val (length, resourceData) = {
        val stream = url.openStream()
        try {
          (stream.available, Enumerator.fromStream(stream))
        } catch {
          case _ => (0, Enumerator[Array[Byte]]())
        }
      }
      if (length == 0) 
        NotFound
      else {
        val response = SimpleResult(
        header = ResponseHeader(OK, Map(
          CONTENT_LENGTH -> length.toString,
          CONTENT_TYPE -> MimeTypes.forFileName(file).getOrElse(BINARY)
        )), resourceData )
        response.withHeaders(CACHE_CONTROL -> Play.configuration.getString("\"assets.cache." + resourceName + "\"").getOrElse(defaultCache))
      }  
    }  
  }
}
