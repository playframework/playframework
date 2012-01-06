package controllers

import play.api._
import play.api.mvc._
import play.api.libs._
import play.api.libs.iteratee._

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

    val resourceName = new File(
      Option(path + "/" + file).map(name => if (name.startsWith("/")) name else ("/" + name)).get
    ).getCanonicalPath

    if (!resourceName.startsWith(new File(path).getCanonicalPath)) {
      Forbidden
    } else {

      val resource = {
        Play.resource(resourceName + ".gz").map(_ -> true)
          .filter(_ => request.headers.get(ACCEPT_ENCODING).map(_.split(',').exists(_ == "gzip" && Play.isProd)).getOrElse(false))
          .orElse(Play.resource(resourceName).map(_ -> false))
      }

      resource.map {

        case (url, isGzipped) => {

          // TODO replace by an Enumerator
          lazy val resourceData = Enumerator.enumerateStream(url.openStream())

          request.headers.get(IF_NONE_MATCH).filter(Some(_) == etagFor(url)).map(_ => NotModified).getOrElse {

            // Prepare a chunked response
            val response = Ok.stream(resourceData).as(MimeTypes.forFileName(file).getOrElse(BINARY))

            // Is Gzipped?
            val gzippedResponse = if (isGzipped) {
              response.withHeaders(CONTENT_ENCODING -> "gzip")
            } else {
              response
            }

            // Add Etag if we are able to compute it
            val taggedResponse = etagFor(url).map(etag => gzippedResponse.withHeaders(ETAG -> etag)).getOrElse(gzippedResponse)

            // Add Cache directive if configured
            val cachedResponse = Play.configuration.getString("\"assets.cache." + resourceName + "\"").map { cacheControl =>
              taggedResponse.withHeaders(CACHE_CONTROL -> cacheControl)
            }.getOrElse(taggedResponse)

            cachedResponse
          }

        }

      }.getOrElse(NotFound)

    }

  }

  // -- ETags handling

  private val etags = scala.collection.mutable.HashMap.empty[String, String]

  private def etagFor(resource: java.net.URL): Option[String] = {
    etags.get(resource.toExternalForm).filter(_ => Play.isProd).orElse {
      val maybeEtag = resource.getProtocol match {
        case "file" => Some(new java.io.File(resource.getPath).lastModified.toString)
        case "jar" => new java.net.URL(resource.getPath) match {
          case url if url.getProtocol == "file" => Some(new java.io.File(url.getPath.takeWhile(c => !(c == '!'))).lastModified.toString)
          case _ => None
        }
        case _ => None
      }
      maybeEtag.foreach(etags.put(resource.toExternalForm, _))
      maybeEtag
    }
  }

}

