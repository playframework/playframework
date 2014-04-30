/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package controllers

import play.api._
import play.api.mvc._
import play.api.libs._
import play.api.libs.iteratee._
import Play.current
import java.io._
import java.net.{ URL, JarURLConnection }
import org.joda.time.format.{ DateTimeFormatter, DateTimeFormat }
import org.joda.time.DateTimeZone
import play.utils.{ InvalidUriEncodingException, UriEncoding }
import scala.concurrent.{ ExecutionContext, Promise, Future, blocking }
import scala.util.Try
import java.util.Date
import play.api.libs.iteratee.Execution.Implicits
import play.api.http.ContentTypes
import scala.collection.concurrent.TrieMap
import play.core.Router.ReverseRouteContext

/*
 * A map designed to prevent the "thundering herds" issue.
 *
 * This could be factored out into its own thing, improved and made available more widely. We could also
 * use spray-cache once it has been re-worked into the Akka code base.
 *
 * The essential mechanics of the cache are that all asset requests are remembered, unless their lookup fails in which
 * case we don't remember them in order to avoid an exploit where we would otherwise run out of memory.
 *
 * The population function is executed using the passed-in execution context
 * which may mean that it is on a separate thread thus permitting long running operations to occur. Other threads
 * requiring the same resource will be given the future of the result immediately.
 *
 * There are no explicit bounds on the cache as it isn't considered likely that the number of distinct asset requests would
 * result in an overflow of memory. Bounds are implied given the number of distinct assets that are available to be
 * served by the project.
 *
 * Instead of a SelfPopulatingMap, a better strategy would be to furnish the assets controller with all of the asset
 * information on startup. This shouldn't be that difficult as sbt-web has that information available. Such an
 * approach would result in an immutable map being used which in theory should be faster.
 */
private class SelfPopulatingMap[K, V] {
  private val store = TrieMap[K, Future[V]]()

  def putIfAbsent(k: K)(pf: K => V)(implicit ec: ExecutionContext): Future[V] = {
    val p = Promise[V]
    store.putIfAbsent(k, p.future) match {
      case Some(f) => f
      case None =>
        val f = Future(pf(k))(ec.prepare)
        f.onFailure {
          case _ => store.remove(k)
        }
        p.completeWith(f)
        p.future
    }
  }
}

/*
 * Retains meta information regarding an asset that can be readily cached.
 */
private object AssetInfo {

  private lazy val defaultCharSet = Play.configuration.getString("default.charset").getOrElse("utf-8")

  private lazy val defaultCacheControl = Play.configuration.getString("assets.defaultCache").getOrElse("max-age=3600")

  private[controllers] val timeZoneCode = "GMT"

  private val parsableTimezoneCode = " " + timeZoneCode

  private[controllers] val df: DateTimeFormatter =
    DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss '" + timeZoneCode + "'").withLocale(java.util.Locale.ENGLISH).withZone(DateTimeZone.forID(timeZoneCode))

  private[controllers] val dfp: DateTimeFormatter =
    DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss").withLocale(java.util.Locale.ENGLISH).withZone(DateTimeZone.forID(timeZoneCode))

  /*
   * jodatime does not parse timezones, so we handle that manually
   */
  private[controllers] def parseDate(date: String): Option[Date] = try {
    val d = dfp.parseDateTime(date.replace(parsableTimezoneCode, "")).toDate
    Some(d)
  } catch {
    case e: IllegalArgumentException =>
      Logger.debug(s"An invalidate date was received: $date", e)
      None
  }
}

/*
 * Retain meta information regarding an asset.
 */
private class AssetInfo(
    val name: String,
    val url: URL,
    val gzipUrl: Option[URL]) {

  import AssetInfo._

  private def addCharsetIfNeeded(mimeType: String): String =
    if (MimeTypes.isText(mimeType)) s"$mimeType; charset=$defaultCharSet" else mimeType

  val cacheControl: String = {
    Play.configuration.getString("\"assets.cache." + name + "\"").getOrElse(if (Play.isProd) defaultCacheControl else "no-cache")
  }

  val isDirectory: Boolean = new File(url.getFile).isDirectory

  val lastModified: Option[String] = url.getProtocol match {
    case "file" => Some(df.print(new File(url.getPath).lastModified))
    case "jar" => {
      Option(url.openConnection).map {
        case jarUrlConnection: JarURLConnection =>
          try {
            jarUrlConnection.getJarEntry.getTime
          } finally {
            jarUrlConnection.getInputStream.close()
          }
      }.filterNot(_ == -1).map(df.print)
    }
    case _ => None
  }

  val etag: Option[String] = lastModified.map(_ + " -> " + url.toExternalForm).map("\"" + Codecs.sha1(_) + "\"")

  val mimeType: String = MimeTypes.forFileName(name).fold(ContentTypes.BINARY)(addCharsetIfNeeded)

  val parsedLastModified = lastModified.flatMap(parseDate)

  def url(gzipAvailable: Boolean): URL = {
    gzipUrl match {
      case Some(x) => if (gzipAvailable) x else url
      case None => url
    }
  }
}

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
object Assets extends AssetsBuilder {

  /**
   * An asset.
   *
   * @param name The name of the asset.
   */
  case class Asset(name: String)

  object Asset {
    implicit def string2Asset(name: String) = new Asset(name)

    implicit def assetPathBindable(implicit rrc: ReverseRouteContext): PathBindable[Asset] = new PathBindable[Asset] {
      def bind(key: String, value: String) = Right(new Asset(value))
      def unbind(key: String, value: Asset) = {
        val path = rrc.fixedParams.get("path").getOrElse(
          throw new RuntimeException("Asset path bindable must be used in combination with an action that accepts a path parameter")
        )
        // todo, convert the returned path to one that has the hash in it
        value.name
      }
    }
  }

  private[controllers] lazy val assetInfoCache = new SelfPopulatingMap[String, AssetInfo]()
}

class AssetsBuilder extends Controller {

  import Assets._
  import AssetInfo._

  private def assetInfoFromResource(name: String): AssetInfo = {
    blocking {
      val url: URL = Play.resource(name).getOrElse(throw new RuntimeException("no resource"))
      val gzipUrl: Option[URL] = Play.resource(name + ".gz")
      new AssetInfo(name, url, gzipUrl)
    }
  }

  private def assetInfo(name: String): Future[AssetInfo] = {
    if (Play.isDev) {
      Future.successful(assetInfoFromResource(name))
    } else {
      assetInfoCache.putIfAbsent(name)(assetInfoFromResource)(Implicits.trampoline)
    }
  }

  private def assetInfoForRequest(request: Request[_], name: String): Future[(AssetInfo, Boolean)] = {
    val gzipRequested = request.headers.get(ACCEPT_ENCODING).exists(_.split(',').exists(_.trim == "gzip"))
    assetInfo(name).map(_ -> gzipRequested)(Implicits.trampoline)
  }

  private def currentTimeFormatted: String = df.print((new Date).getTime)

  private def maybeNotModified(request: Request[_], assetInfo: AssetInfo): Option[Result] = {
    // First check etag. Important, if there is an If-None-Match header, we MUST not check the
    // If-Modified-Since header, regardless of whether If-None-Match matches or not. This is in
    // accordance with section 14.26 of RFC2616.
    request.headers.get(IF_NONE_MATCH) match {
      case Some(etags) =>
        assetInfo.etag.filter(someEtag => etags.split(',').exists(_.trim == someEtag)).flatMap(_ => Some(cacheableResult(assetInfo, NotModified)))
      case None =>
        for {
          ifModifiedSinceStr <- request.headers.get(IF_MODIFIED_SINCE)
          ifModifiedSince <- parseDate(ifModifiedSinceStr)
          lastModified <- assetInfo.parsedLastModified
          if !lastModified.after(ifModifiedSince)
        } yield {
          NotModified.withHeaders(DATE -> currentTimeFormatted)
        }
    }
  }

  private def cacheableResult[A <: Result](assetInfo: AssetInfo, r: A): Result = {

    def addHeaderIfValue(name: String, maybeValue: Option[String], response: Result): Result = {
      maybeValue.fold(response)(v => response.withHeaders(name -> v))
    }

    val r1 = addHeaderIfValue(ETAG, assetInfo.etag, r)
    val r2 = addHeaderIfValue(LAST_MODIFIED, assetInfo.lastModified, r1)

    r2.withHeaders(CACHE_CONTROL -> assetInfo.cacheControl)
  }

  private def result(file: String,
    length: Int,
    mimeType: String,
    resourceData: Enumerator[Array[Byte]],
    gzipRequested: Boolean,
    gzipAvailable: Boolean): Result = {

    val response = Result(
      ResponseHeader(
        OK,
        Map(
          CONTENT_LENGTH -> length.toString,
          CONTENT_TYPE -> mimeType,
          DATE -> currentTimeFormatted
        )
      ),
      resourceData)
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
  def versioned(path: String, file: Asset) = at(path, file.name)

  /**
   * Generates an `Action` that serves a static resource.
   *
   * @param path the root folder for searching the static resource files, such as `"/public"`. Not URL encoded.
   * @param file the file part extracted from the URL. May be URL encoded (note that %2F decodes to literal /).
   */
  def at(path: String, file: String): Action[AnyContent] = Action.async {
    implicit request =>

      import Implicits.trampoline
      val pendingResult: Future[Result] = for {
        Some(name) <- Future.successful(resourceNameAt(path, file))
        (assetInfo, gzipRequested) <- assetInfoForRequest(request, name) if !assetInfo.isDirectory
      } yield {
        val stream = assetInfo.url(gzipRequested).openStream()
        Try(stream.available -> Enumerator.fromStream(stream)(Implicits.defaultExecutionContext)).map {
          case (length, resourceData) =>
            maybeNotModified(request, assetInfo).getOrElse {
              cacheableResult(
                assetInfo,
                result(file, length, assetInfo.mimeType, resourceData, gzipRequested, assetInfo.gzipUrl.isDefined)
              )
            }
        }.getOrElse(NotFound)
      }

      pendingResult.recover {
        case e: InvalidUriEncodingException =>
          Logger.debug(s"Invalid URI encoding for $file at $path", e)
          BadRequest
        case e: Throwable =>
          Logger.debug(s"Unforseen error for $file at $path", e)
          NotFound
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
    if (resourceFile.isDirectory || !resourceFile.getCanonicalPath.startsWith(pathFile.getCanonicalPath)) {
      None
    } else {
      Some(resourceName)
    }
  }

  private val dblSlashPattern = """//+""".r
}

