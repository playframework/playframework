/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package controllers

import play.api._
import play.api.mvc._
import play.api.libs._
import play.api.libs.iteratee._
import java.io._
import java.net.{ URL, URLConnection, JarURLConnection }
import org.joda.time.format.{ DateTimeFormatter, DateTimeFormat }
import org.joda.time.DateTimeZone
import play.utils.{ InvalidUriEncodingException, UriEncoding }
import scala.concurrent.{ ExecutionContext, Promise, Future, blocking }
import scala.util.control.NonFatal
import scala.util.{ Success, Failure }
import java.util.Date
import play.api.libs.iteratee.Execution.Implicits
import play.api.http.{ LazyHttpErrorHandler, HttpErrorHandler, ContentTypes }
import scala.collection.concurrent.TrieMap
import play.core.Router.ReverseRouteContext
import scala.io.Source
import javax.inject.{ Inject, Singleton }

/*
 * A map designed to prevent the "thundering herds" issue.
 *
 * This could be factored out into its own thing, improved and made available more widely. We could also
 * use spray-cache once it has been re-worked into the Akka code base.
 *
 * The essential mechanics of the cache are that all asset requests are remembered, unless their lookup fails or if
 * the asset doesn't exist, in which case we don't remember them in order to avoid an exploit where we would otherwise
 * run out of memory.
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
  private val store = TrieMap[K, Future[Option[V]]]()

  def putIfAbsent(k: K)(pf: K => Option[V])(implicit ec: ExecutionContext): Future[Option[V]] = {
    lazy val p = Promise[Option[V]]()
    store.putIfAbsent(k, p.future) match {
      case Some(f) => f
      case None =>
        val f = Future(pf(k))(ec.prepare())
        f.onComplete {
          case Failure(_) | Success(None) => store.remove(k)
          case _ => // Do nothing, the asset was successfully found and is now cached
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

  def config[T](lookup: Configuration => Option[T]): Option[T] = for {
    app <- Play.maybeApplication
    value <- lookup(app.configuration)
  } yield value

  def isDev = Play.maybeApplication.fold(false)(_.mode == Mode.Dev)
  def isProd = Play.maybeApplication.fold(false)(_.mode == Mode.Prod)

  def resource(name: String): Option[URL] = for {
    app <- Play.maybeApplication
    resource <- app.resource(name)
  } yield resource

  lazy val defaultCharSet = config(_.getString("default.charset")).getOrElse("utf-8")

  lazy val defaultCacheControl = config(_.getString("assets.defaultCache")).getOrElse("public, max-age=3600")

  lazy val aggressiveCacheControl = config(_.getString("assets.aggressiveCache")).getOrElse("public, max-age=31536000")

  lazy val digestAlgorithm = config(_.getString("assets.digest.algorithm")).getOrElse("md5")

  val timeZoneCode = "GMT"

  val parsableTimezoneCode = " " + timeZoneCode

  val df: DateTimeFormatter =
    DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss '" + timeZoneCode + "'").withLocale(java.util.Locale.ENGLISH).withZone(DateTimeZone.forID(timeZoneCode))

  val dfp: DateTimeFormatter =
    DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss").withLocale(java.util.Locale.ENGLISH).withZone(DateTimeZone.forID(timeZoneCode))

  /*
   * jodatime does not parse timezones, so we handle that manually
   */
  def parseModifiedDate(date: String): Option[Date] = try {
    // IE sends "; length=xxx" with If-Modified-Since header
    val d = dfp.parseDateTime(date.replace(parsableTimezoneCode, "").split(";", 2).head).toDate
    Some(d)
  } catch {
    case e: IllegalArgumentException =>
      Logger.debug(s"An invalid date was received: $date", e)
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

  val configuredCacheControl = config(_.getString("\"assets.cache." + name + "\""))

  def cacheControl(aggressiveCaching: Boolean): String = {
    configuredCacheControl.getOrElse {
      if (isProd) {
        if (aggressiveCaching) aggressiveCacheControl else defaultCacheControl
      } else {
        "no-cache"
      }
    }
  }

  val lastModified: Option[String] = {
    def getLastModified[T <: URLConnection](f: (T) => Long): Option[String] = {
      Option(url.openConnection).map {
        case urlConnection: T @unchecked =>
          try {
            f(urlConnection)
          } finally {
            urlConnection.getInputStream.close()
          }
      }.filterNot(_ == -1).map(df.print)
    }

    url.getProtocol match {
      case "file" => Some(df.print(new File(url.toURI).lastModified))
      case "jar" => getLastModified[JarURLConnection](c => c.getJarEntry.getTime)
      case "bundle" => getLastModified[URLConnection](c => c.getLastModified)
      case _ => None
    }
  }

  val etag: Option[String] =
    digest orElse { lastModified map (m => Codecs.sha1(m + " -> " + url.toExternalForm)) } map ("\"" + _ + "\"")

  val mimeType: String = MimeTypes.forFileName(name).fold(ContentTypes.BINARY)(addCharsetIfNeeded)

  val parsedLastModified = lastModified flatMap parseModifiedDate

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
object Assets extends AssetsBuilder(LazyHttpErrorHandler) {

  import AssetInfo._

  // Caching. It is unfortunate that we require both a digestCache and an assetInfo cache given that digest info is
  // part of asset information. The reason for this is that the assetInfo cache returns a Future[AssetInfo] in order to
  // avoid any thundering herds issue. The unbind method of the assetPathBindable doesn't support the return of a
  // Future - unbinds are expected to be blocking. Thus we separate out the caching of a digest from the caching of
  // full asset information. At least the determination of the digest should be relatively quick (certainly not as
  // involved as determining the full asset info).

  val digestCache = TrieMap[String, Option[String]]()

  private[controllers] def digest(path: String): Option[String] = {
    digestCache.getOrElse(path, {
      val maybeDigestUrl: Option[URL] = resource(path + "." + digestAlgorithm)
      val maybeDigest: Option[String] = maybeDigestUrl.map(Source.fromURL(_).mkString)
      if (!isDev && maybeDigest.isDefined) digestCache.put(path, maybeDigest)
      maybeDigest
    })
  }

  // Sames goes for the minified paths cache.
  val minifiedPathsCache = TrieMap[String, String]()

  lazy val checkForMinified = config(_.getBoolean("assets.checkForMinified")).getOrElse(true)

  private[controllers] def minifiedPath(path: String): String = {
    minifiedPathsCache.getOrElse(path, {
      def minifiedPathFor(delim: Char): Option[String] = {
        val ext = path.reverse.takeWhile(_ != '.').reverse
        val noextPath = path.dropRight(ext.size + 1)
        val minPath = noextPath + delim + "min." + ext
        resource(minPath).map(_ => minPath)
      }
      val maybeMinifiedPath = if (checkForMinified) {
        minifiedPathFor('.').orElse(minifiedPathFor('-')).getOrElse(path)
      } else {
        path
      }
      if (!isDev) minifiedPathsCache.put(path, maybeMinifiedPath)
      maybeMinifiedPath
    })
  }

  private[controllers] lazy val assetInfoCache = new SelfPopulatingMap[String, AssetInfo]()

  private def assetInfoFromResource(name: String): Option[AssetInfo] = {
    blocking {
      for {
        url <- resource(name)
      } yield {
        val gzipUrl: Option[URL] = resource(name + ".gz")
        new AssetInfo(name, url, gzipUrl, digest(name))
      }
    }
  }

  private def assetInfo(name: String): Future[Option[AssetInfo]] = {
    if (isDev) {
      Future.successful(assetInfoFromResource(name))
    } else {
      assetInfoCache.putIfAbsent(name)(assetInfoFromResource)(Implicits.trampoline)
    }
  }

  private[controllers] def assetInfoForRequest(request: Request[_], name: String): Future[Option[(AssetInfo, Boolean)]] = {
    val gzipRequested = request.headers.get(ACCEPT_ENCODING).exists(_.split(',').exists(_.trim == "gzip"))
    assetInfo(name).map(_.map(_ -> gzipRequested))(Implicits.trampoline)
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

    private def pathFromParams(rrc: ReverseRouteContext): String = {
      rrc.fixedParams.getOrElse("path",
        throw new RuntimeException("Asset path bindable must be used in combination with an action that accepts a path parameter")
      ).toString
    }

    implicit def assetPathBindable(implicit rrc: ReverseRouteContext) = new PathBindable[Asset] {
      def bind(key: String, value: String) = Right(new Asset(value))

      def unbind(key: String, value: Asset): String = {
        val base = pathFromParams(rrc)
        val path = base + "/" + value.name
        blocking {
          val minPath = minifiedPath(path)
          digest(minPath).fold(minPath) { dgst =>
            val lastSep = minPath.lastIndexOf("/")
            minPath.take(lastSep + 1) + dgst + "-" + minPath.drop(lastSep + 1)
          }.drop(base.size + 1)
        }
      }
    }
  }
}

@Singleton
class Assets @Inject() (errorHandler: HttpErrorHandler) extends AssetsBuilder(errorHandler)

class AssetsBuilder(errorHandler: HttpErrorHandler) extends Controller {

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
          ifModifiedSince <- parseModifiedDate(ifModifiedSinceStr)
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
    // We want to detect if it's a fingerprinted asset, because if it's fingerprinted, we can aggressively cache it,
    // otherwise we can't.
    val requestedDigest = f.getName.takeWhile(_ != '-')
    if (!requestedDigest.isEmpty) {
      val bareFile = new File(f.getParent, f.getName.drop(requestedDigest.size + 1)).getPath
      val bareFullPath = new File(path + File.separator + bareFile).getPath
      blocking(digest(bareFullPath)) match {
        case Some(`requestedDigest`) => at(path, bareFile, aggressiveCaching = true)
        case _ => at(path, file.name)
      }
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
      val assetName: Option[String] = resourceNameAt(path, file)
      val assetInfoFuture: Future[Option[(AssetInfo, Boolean)]] = assetName.map { name =>
        assetInfoForRequest(request, name)
      } getOrElse Future.successful(None)

      val pendingResult: Future[Result] = assetInfoFuture.flatMap {
        case Some((assetInfo, gzipRequested)) =>
          val stream = assetInfo.url(gzipRequested).openStream()
          val length = stream.available
          val resourceData = Enumerator.fromStream(stream)(Implicits.defaultExecutionContext)

          Future.successful(maybeNotModified(request, assetInfo, aggressiveCaching).getOrElse {
            cacheableResult(
              assetInfo,
              aggressiveCaching,
              result(file, length, assetInfo.mimeType, resourceData, gzipRequested, assetInfo.gzipUrl.isDefined)
            )
          })
        case None => errorHandler.onClientError(request, NOT_FOUND, "Resource not found by Assets controller")
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

