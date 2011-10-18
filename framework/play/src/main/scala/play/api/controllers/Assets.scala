package controllers

import play.api._
import play.api.mvc._
import play.api.libs._

import Play.currentApplication

import scalax.io._

/**
 * Controller for serving static resources.
 *
 * You can use this controller in any application, just be declaring the appropriate route:
 * {{{
 * GET     /assets/\uFEFF*file               controllers.Assets.at(path="/public", file)
 * }}}
 */
object Assets extends Controller {

  /**
   * Generate an Action that serve a static resource.
   *
   * @param path The root folder for searching the static resources files (for example "/public").
   * @param file The file part extracted from the URL.
   */
  def at(path: String, file: String) = Action {
    Play.resourceAsStream(path + "/" + file).map { is =>
      Binary(is, length = Some(is.available), contentType = MimeTypes.forFileName(file).getOrElse("application/octet-stream")).withHeaders(CACHE_CONTROL -> "max-age=3600")
    }.getOrElse(NotFound)
  }

}

