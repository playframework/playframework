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
import scala.io.Source

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
private[controllers] object AssetInfo {

  lazy val defaultCharSet = Play.configuration.getString("default.charset").getOrElse("utf-8")

  lazy val defaultCacheControl = Play.configuration.getString("assets.defaultCache").getOrElse("public, max-age=3600")

  lazy val aggressiveCacheControl = Play.configuration.getString("assets.aggressiveCache").getOrElse("public, max-age=31536000")

  lazy val digestAlgorithm = Play.configuration.getString("assets.digest.algorithm").getOrElse("md5")

  val timeZoneCode = "GMT"

  val parsableTimezoneCode = " " + timeZoneCode

  val df: DateTimeFormatter =
    DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss '" + timeZoneCode + "'").withLocale(java.util.Locale.ENGLISH).withZone(DateTimeZone.forID(timeZoneCode))

  val dfp: DateTimeFormatter =
    DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss").withLocale(java.util.Locale.ENGLISH).withZone(DateTimeZone.forID(timeZoneCode))

  /*
   * jodatime does not parse timezones, so we handle that manually
   */
  def parseDate(date: String): Option[Date] = try {
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
private[controllers] class AssetInfo(
    val name: String,
    val url: URL,
    val gzipUrl: Option[URL],
    val digest: Option[String]) {

  import AssetInfo._

  def addCharsetIfNeeded(mimeType: String): String =
    if (MimeTypes.isText(mimeType)) s"$mimeType; charset=$defaultCharSet" else mimeType

  val configuredCacheControl = Play.configuration.getString("\"assets.cache." + name + "\"")

  def cacheControl(aggressiveCaching: Boolean): String = {
    configuredCacheControl.getOrElse {
      if (Play.isProd) {
        if (aggressiveCaching) aggressiveCacheControl else defaultCacheControl
      } else {
        "no-cache"
      }
    }
  }

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

  val etag: Option[String] = digest.orElse(lastModified.map(_ + " -> " + url.toExternalForm).map("\"" + Codecs.sha1(_) + "\""))

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
 * If a gzipped version of a resource is found (Same resource name with the .gz suffix), it is served instead. If a
 * digest file is available for a given asset then its contents are read and used to supply a digest value. This value will be used for
 * serving up ETag values and for the purposes of reverse routing. For example given "a.js", if there is an "a.js.md5"
 * file available then the latter contents will be used to determine the Etag value.
 * The reverse router also uses the digest in order to translate any file to the form &lt;digest&gt;-&lt;asset&gt; for
 * example "a.js" may be also found at "d41d8cd98f00b204e9800998ecf8427e-a.js".
 * If there is no digest file found then digest values for ETags are formed by forming a sha1 digest of the last-modified
 * time.
 *
 * The default digest algorithm to search for is "md5". You can override this quite easily. For example if the SHA-1
 * algorithm is preferred:
 *
 * {{{
 * "assets.digest.algorithm" = "sha1"
 * }}}
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

  import AssetInfo._

  // Caching. It is unfortunate that we require both a digestCache and an assetInfo cache given that digest info is
  // part of asset information. The reason for this is that the assetInfo cache returns a Future[AssetInfo] in order to
  // avoid any thundering herds issue. The unbind method of the assetPathBindable doesn't support the return of a
  // Future - unbinds are expected to be blocking. Thus we separate out the caching of a digest from the caching of
  // full asset information. At least the determination of the digest should be relatively quick (certainly not as
  // involved as determining the full asset info).

  val digestCache = TrieMap[String, Option[String]]()

  private[controllers] def digest(path: String): Option[String] = {
    digestCache.get(path).getOrElse {
      val maybeDigestUrl: Option[URL] = Play.resource(path + "." + digestAlgorithm)
      val maybeDigest: Option[String] = maybeDigestUrl.map(Source.fromURL(_).mkString)
      if (!Play.isDev) digestCache.put(path, maybeDigest)
      maybeDigest
    }
  }

  private[controllers] lazy val assetInfoCache = new SelfPopulatingMap[String, AssetInfo]()

  private def assetInfoFromResource(name: String): AssetInfo = {
    blocking {
      val url: URL = Play.resource(name).getOrElse(throw new RuntimeException("no resource"))
      val gzipUrl: Option[URL] = Play.resource(name + ".gz")
      new AssetInfo(name, url, gzipUrl, digest(name))
    }
  }

  private def assetInfo(name: String): Future[AssetInfo] = {
    if (Play.isDev) {
      Future.successful(assetInfoFromResource(name))
    } else {
      assetInfoCache.putIfAbsent(name)(assetInfoFromResource)(Implicits.trampoline)
    }
  }

  private[controllers] def assetInfoForRequest(request: Request[_], name: String): Future[(AssetInfo, Boolean)] = {
    val gzipRequested = request.headers.get(ACCEPT_ENCODING).exists(_.split(',').exists(_.trim == "gzip"))
    assetInfo(name).map(_ -> gzipRequested)(Implicits.trampoline)
  }

  /**
   * An asset.
   *
   * @param name The name of the asset.
   */
  case class Asset(name: String)

  object Asset {
    import scala.language.implicitConversions

    implicit def string2Asset(name: String) = new Asset(name)

    implicit def assetPathBindable(implicit rrc: ReverseRouteContext) = new PathBindable[Asset] {
      def bind(key: String, value: String) = Right(new Asset(value))
      def unbind(key: String, value: Asset): String = {
        val path = rrc.fixedParams.get("path").getOrElse(
          throw new RuntimeException("Asset path bindable must be used in combination with an action that accepts a path parameter")
        ).toString
        val f = new File(path + File.separator + value.name)
        blocking {
          digest(f.getPath)
        }.fold(value.name)(d => new File(f.getParent, d + "-" + f.getName).getPath.drop(path.size + 1))
      }
    }
  }
}

class AssetsBuilder extends Controller {

  import Assets._
  import AssetInfo._

  private def currentTimeFormatted: String = df.print((new Date).getTime)

  private def maybeNotModified(request: Request[_], assetInfo: AssetInfo, aggressiveCaching: Boolean): Option[Result] = {
    // First check etag. Important, if there is an If-None-Match header, we MUST not check the
    // If-Modified-Since header, regardless of whether If-None-Match matches or not. This is in
    // accordance with section 14.26 of RFC2616.
    request.headers.get(IF_NONE_MATCH) match {
      case Some(etags) =>
        assetInfo.etag.filter(someEtag => etags.split(',').exists(_.trim == someEtag)).flatMap(_ => Some(cacheableResult(assetInfo, aggressiveCaching, NotModified)))
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

  private def cacheableResult[A <: Result](assetInfo: AssetInfo, aggressiveCaching: Boolean, r: A): Result = {

    def addHeaderIfValue(name: String, maybeValue: Option[String], response: Result): Result = {
      maybeValue.fold(response)(v => response.withHeaders(name -> v))
    }

    val r1 = addHeaderIfValue(ETAG, assetInfo.etag, r)
    val r2 = addHeaderIfValue(LAST_MODIFIED, assetInfo.lastModified, r1)

    r2.withHeaders(CACHE_CONTROL -> assetInfo.cacheControl(aggressiveCaching))
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
  def versioned(path: String, file: Asset): Action[AnyContent] = {
    val f = new File(file.name)
    val requestedDigest = f.getName.takeWhile(_ != '-')
    if (!requestedDigest.isEmpty) {
      val bareFile = new File(f.getParent, f.getName.drop(requestedDigest.size + 1)).getPath
      val bareFullPath = new File(path + File.separator + bareFile).getPath
      if (blocking(digest(bareFullPath)) == Some(requestedDigest)) at(path, bareFile, true) else at(path, file.name)
    } else {
      at(path, file.name)
    }
  }

  /**
   * Generates an `Action` that serves a static resource.
   *
   * @param path the root folder for searching the static resource files, such as `"/public"`. Not URL encoded.
   * @param file the file part extracted from the URL. May be URL encoded (note that %2F decodes to literal /).
   * @param aggressiveCaching if true then an aggressive set of caching directives will be used. Defaults to false.
   */
  def at(path: String, file: String, aggressiveCaching: Boolean = false): Action[AnyContent] = Action.async {
    implicit request =>

      import Implicits.trampoline
      val pendingResult: Future[Result] = for {
        Some(name) <- Future.successful(resourceNameAt(path, file))
        (assetInfo, gzipRequested) <- assetInfoForRequest(request, name)
      } yield {
        val stream = assetInfo.url(gzipRequested).openStream()
        Try(stream.available -> Enumerator.fromStream(stream)(Implicits.defaultExecutionContext)).map {
          case (length, resourceData) =>
            maybeNotModified(request, assetInfo, aggressiveCaching).getOrElse {
              cacheableResult(
                assetInfo,
                aggressiveCaching,
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
    if (!resourceFile.getCanonicalPath.startsWith(pathFile.getCanonicalPath)) {
      None
    } else {
      Some(resourceName)
    }
  }

  private val dblSlashPattern = """//+""".r
}

