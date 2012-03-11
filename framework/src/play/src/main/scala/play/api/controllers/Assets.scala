package controllers

import play.api._
import play.api.mvc._
import play.api.libs._
import play.api.libs.iteratee._

import Play.current

import java.io._
import scalax.io.{ Resource }
import java.text.SimpleDateFormat
import collection.JavaConverters._

/**
 * Controller that serves static resources.
 *
 * Resources are searched in the classpath.
 *
 * It handles Last-Modified and ETag header automatically.
 * If a gzipped version of a resource is found (Same resource name with the .gz suffix), it is served instead.
 *
 * You can set a custom Cache directive for a particular resource if needed. For example in your application.conf file:
 *
 * {{{
 * "assets.cache./public/images/logo.png" = "max-age=3600"
 * }}}
 *
 * You can use this controller in any application, just by declaring the appropriate route. For example:
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
  def at(path: String, file: String): Action[AnyContent] = Action { request =>

    val resourceName = Option(path + "/" + file).map(name => if (name.startsWith("/")) name else ("/" + name)).get

    if (new File(resourceName).isDirectory || !new File(resourceName).getCanonicalPath.startsWith(new File(path).getCanonicalPath)) {
      NotFound
    } else {

      val resource = {
        Play.resource(resourceName + ".gz").map(_ -> true)
          .filter(_ => request.headers.get(ACCEPT_ENCODING).map(_.split(',').exists(_ == "gzip" && Play.isProd)).getOrElse(false))
          .orElse(Play.resource(resourceName).map(_ -> false))
      }

      resource.map {

        case (url, _) if new File(url.getFile).isDirectory => NotFound

        case (url, isGzipped) => {

          lazy val (length, resourceData) = {
            val stream = url.openStream()
            try {
              (stream.available, Enumerator.fromStream(stream))
            } catch {
              case _ => (0, Enumerator[Array[Byte]]())
            }
          }
          
          if(length == 0) {
            NotFound
          } else {
            
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

              // Add Etag if we are able to compute it
              val taggedResponse = etagFor(url).map(etag => gzippedResponse.withHeaders(ETAG -> etag)).getOrElse(gzippedResponse)

              // Add Cache directive if configured
              val cachedResponse = taggedResponse.withHeaders(CACHE_CONTROL -> {
                Play.configuration.getString("\"assets.cache." + resourceName + "\"").getOrElse(Play.mode match {
                  case Mode.Prod => Play.configuration.getString("assets.defaultCache").getOrElse("max-age=3600")
                  case _ => "no-cache"
                })
              })

              cachedResponse

            }

          }
            
        }  

      }.getOrElse(NotFound)

    }

  }

  // -- LastModified handling

  private val dateFormatter = {
    val formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz")
    formatter.setTimeZone(java.util.TimeZone.getTimeZone("UTC"))
    formatter
  }

  private val lastModifieds = (new java.util.concurrent.ConcurrentHashMap[String,String]()).asScala

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

  private val etags = (new java.util.concurrent.ConcurrentHashMap[String,String]()).asScala

  private def etagFor(resource: java.net.URL): Option[String] = {
    etags.get(resource.toExternalForm).filter(_ => Play.isProd).orElse {
      val maybeEtag = lastModifiedFor(resource).map(_ + " -> " + resource.toExternalForm).map(Codecs.sha1)
      maybeEtag.foreach(etags.put(resource.toExternalForm, _))
      maybeEtag
    }
  }

}

