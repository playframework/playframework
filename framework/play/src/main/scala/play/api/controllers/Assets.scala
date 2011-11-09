package controllers

import play.api._
import play.api.mvc._
import play.api.libs._

import Play.current

import scalax.io._

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
  def at(path: String, file: String) = Action {
    Play.resourceAsStream(path + "/" + file).map { is =>
      Binary(is, length = Some(is.available), contentType = MimeTypes.forFileName(file).getOrElse("application/octet-stream")).withHeaders(CACHE_CONTROL -> "max-age=3600")
    }.getOrElse(NotFound)
  }

}

