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

    val resourceName = Option(path + "/" + file).map(name => if (name.startsWith("/")) name else ("/" + name)).get

    if (!new File(resourceName).getCanonicalPath.startsWith(new File(path).getCanonicalPath)) {
      Forbidden
    } else {

      val resource = {
        Play.resource(resourceName + ".gz").map(_ -> true)
          .filter(_ => request.headers.get(ACCEPT_ENCODING).map(_.split(',').exists(_ == "gzip" && Play.isProd)).getOrElse(false))
          .orElse(Play.resource(resourceName).map(_ -> false))
      }

      resource.map {

        case (url, isGzipped) => {

          lazy val (length, resourceData) = {
            val stream = url.openStream()
            (stream.available, Enumerator.enumerateStream(stream))
          }

          request.headers.get(IF_NONE_MATCH).filter(Some(_) == etagFor(url)).map(_ => NotModified).getOrElse {

            // Prepare a streamed response
            val response = SimpleResult(
              header = ResponseHeader(OK, Map(
                CONTENT_LENGTH -> length.toString,
                CONTENT_TYPE -> MimeTypes.forFileName(file).getOrElse(BINARY)
              )),
              resourceData
            )

            // Is Gzipped?
            val gzippedResponse = if (isGzipped) {
              response.withHeaders(CONTENT_ENCODING -> "gzip")
            } else {
              response
            }

            // Add Last-Modified if we are able to compute it
            val lastModifiedResponse = lastModifiedFor(url).map(date => gzippedResponse.withHeaders(LAST_MODIFIED -> date)).getOrElse(gzippedResponse)

            // Add Etag if we are able to compute it
            val taggedResponse = etagFor(url).map(etag => lastModifiedResponse.withHeaders(ETAG -> etag)).getOrElse(lastModifiedResponse)

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

  // -- LastModified handling

  private val dateFormatter = {
    val formatter = new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz")
    formatter.setTimeZone(java.util.TimeZone.getTimeZone("UTC"))
    formatter
  }

  private val lastModifieds = scala.collection.mutable.HashMap.empty[String, String]

  private def lastModifiedFor(resource: java.net.URL): Option[String] = {
    lastModifieds.get(resource.toExternalForm).filter(_ => Play.isProd).orElse {
      val maybeLastModified = resource.getProtocol match {
        case "file" => Some(dateFormatter.format(new java.util.Date(new java.io.File(resource.getPath).lastModified)))
        case "jar" => new java.net.URL(resource.getPath) match {
          case url if url.getProtocol == "file" => Some(
            dateFormatter.format(new java.util.Date(new java.io.File(url.getPath.takeWhile(c => !(c == '!'))).lastModified))
          )
          case _ => None
        }
        case _ => None
      }
      maybeLastModified.foreach(lastModifieds.put(resource.toExternalForm, _))
      maybeLastModified
    }
  }

  // -- ETags handling

  private val etags = scala.collection.mutable.HashMap.empty[String, String]

  private def etagFor(resource: java.net.URL): Option[String] = {
    etags.get(resource.toExternalForm).filter(_ => Play.isProd).orElse {
      val maybeEtag = lastModifiedFor(resource).map(_ + " -> " + resource.toExternalForm).map(Codecs.sha1(_))
      maybeEtag.foreach(etags.put(resource.toExternalForm, _))
      maybeEtag
    }
  }

}

