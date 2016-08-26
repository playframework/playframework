/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
import java.io.File

import akka.stream.scaladsl.StreamConverters
import play.api.http.HttpErrorHandler
import play.api.mvc._
import play.utils.{InvalidUriEncodingException, Resources, UriEncoding}

import scala.concurrent.{Future, _}
import scala.util.control.NonFatal

package controllers {

  class AssetsBuilder(errorHandler: HttpErrorHandler) extends Controller {
    import Execution.trampoline

    import Assets._
    import AssetInfo._

    private def maybeNotModified(request: RequestHeader, assetInfo: AssetInfo, aggressiveCaching: Boolean): Option[Result] = {
      // First check etag. Important, if there is an If-None-Match header, we MUST not check the
      // If-Modified-Since header, regardless of whether If-None-Match matches or not. This is in
      // accordance with section 14.26 of RFC2616.
      request.headers.get(IF_NONE_MATCH) match {
        case Some(etags) =>
          assetInfo.etag.filter(someEtag => etags.split(',').exists(_.trim == someEtag)).flatMap(_ => Some(cacheableResult(assetInfo, aggressiveCaching, NotModified)))
        case None =>
          for {
            ifModifiedSinceStr <- request.headers.get(IF_MODIFIED_SINCE)
            ifModifiedSince <- parseModifiedDate(ifModifiedSinceStr)
            lastModified <- assetInfo.parsedLastModified
            if !lastModified.after(ifModifiedSince)
          } yield {
            NotModified
          }
      }
    }

    private def cacheableResult[A <: Result](assetInfo: AssetInfo, aggressiveCaching: Boolean, r: A): Result = {

      def addHeaderIfValue(name: String, maybeValue: Option[String], response: Result): Result = {
        maybeValue.fold(response)(v => response.withHeaders(name -> v))
      }

      val r1 = addHeaderIfValue(ETAG, assetInfo.etag, r)
      val r2 = addHeaderIfValue(LAST_MODIFIED, assetInfo.lastModified, r1)

      r2.withHeaders(CACHE_CONTROL -> assetInfo.cacheControl(aggressiveCaching))
    }

    private def asGzipResult(response: Result, gzipRequested: Boolean, gzipAvailable: Boolean): Result = {
      if (gzipRequested && gzipAvailable) {
        response.withHeaders(VARY -> ACCEPT_ENCODING, CONTENT_ENCODING -> "gzip")
      } else if (gzipAvailable) {
        response.withHeaders(VARY -> ACCEPT_ENCODING)
      } else {
        response
      }
    }

    /**
     * Generates an `Action` that serves a versioned static resource.
     */
    def versioned(path: String, file: Asset): Action[AnyContent] = Action.async { implicit request =>
      val f = new File(file.name)
      // We want to detect if it's a fingerprinted asset, because if it's fingerprinted, we can aggressively cache it,
      // otherwise we can't.
      val requestedDigest = f.getName.takeWhile(_ != '-')
      if (!requestedDigest.isEmpty) {
        val bareFile = new File(f.getParent, f.getName.drop(requestedDigest.length + 1)).getPath.replace('\\', '/')
        val bareFullPath = path + "/" + bareFile
        blocking(digest(bareFullPath)) match {
          case Some(`requestedDigest`) => assetAt(path, bareFile, aggressiveCaching = true)
          case _ => assetAt(path, file.name, aggressiveCaching = false)
        }
      } else {
        assetAt(path, file.name, aggressiveCaching = false)
      }
    }

    /**
     * Generates an `Action` that serves a static resource.
     *
     * @param path the root folder for searching the static resource files, such as `"/public"`. Not URL encoded.
     * @param file the file part extracted from the URL. May be URL encoded (note that %2F decodes to literal /).
     * @param aggressiveCaching if true then an aggressive set of caching directives will be used. Defaults to false.
     */
    def at(path: String, file: String, aggressiveCaching: Boolean = false): Action[AnyContent] = Action.async { implicit request =>
      assetAt(path, file, aggressiveCaching)
    }

    private def assetAt(path: String, file: String, aggressiveCaching: Boolean)(implicit request: RequestHeader): Future[Result] = {
      val assetName: Option[String] = resourceNameAt(path, file)
      val assetInfoFuture: Future[Option[(AssetInfo, Boolean)]] = assetName.map { name =>
        assetInfoForRequest(request, name)
      } getOrElse Future.successful(None)

      def notFound = errorHandler.onClientError(request, NOT_FOUND, "Resource not found by Assets controller")

      val pendingResult: Future[Result] = assetInfoFuture.flatMap {
        case Some((assetInfo, gzipRequested)) =>
          val connection = assetInfo.url(gzipRequested).openConnection()
          // Make sure it's not a directory
          if (Resources.isUrlConnectionADirectory(connection)) {
            Resources.closeUrlConnection(connection)
            notFound
          } else {
            val stream = connection.getInputStream
            val source = StreamConverters.fromInputStream(() => stream)
            // FIXME stream.available does not necessarily return the length of the file. According to the docs "It is never
            // correct to use the return value of this method to allocate a buffer intended to hold all data in this stream."
            val result = RangeResult.ofSource(stream.available(), source, request.headers.get(RANGE), None, Option(assetInfo.mimeType))

            Future.successful(maybeNotModified(request, assetInfo, aggressiveCaching).getOrElse {
              cacheableResult(
                assetInfo,
                aggressiveCaching,
                asGzipResult(result, gzipRequested, assetInfo.gzipUrl.isDefined)
              )
            })
          }
        case None => notFound
      }

      pendingResult.recoverWith {
        case e: InvalidUriEncodingException =>
          errorHandler.onClientError(request, BAD_REQUEST, s"Invalid URI encoding for $file at $path: " + e.getMessage)
        case NonFatal(e) =>
          // Add a bit more information to the exception for better error reporting later
          errorHandler.onServerError(request, new RuntimeException(s"Unexpected error while serving $file at $path: " + e.getMessage, e))
      }
    }

    /**
     * Get the name of the resource for a static resource. Used by `at`.
     *
     * @param path the root folder for searching the static resource files, such as `"/public"`. Not URL encoded.
     * @param file the file part extracted from the URL. May be URL encoded (note that %2F decodes to literal /).
     */
    private[controllers] def resourceNameAt(path: String, file: String): Option[String] = {
      val decodedFile = UriEncoding.decodePath(file, "utf-8")
      def dblSlashRemover(input: String): String = dblSlashPattern.replaceAllIn(input, "/")
      val resourceName = dblSlashRemover(s"/$path/$decodedFile")
      val resourceFile = new File(resourceName)
      val pathFile = new File(path)
      if (!resourceFile.getCanonicalPath.startsWith(pathFile.getCanonicalPath)) {
        None
      } else {
        Some(resourceName)
      }
    }

    private val dblSlashPattern = """//+""".r
  }

}