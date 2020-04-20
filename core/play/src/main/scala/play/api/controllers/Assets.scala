/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package controllers

import java.io._
import java.net.JarURLConnection
import java.net.URL
import java.net.URLConnection
import java.time._
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Date
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

import akka.stream.scaladsl.StreamConverters

import play.api._
import play.api.http._
import play.api.inject.ApplicationLifecycle
import play.api.inject.Module
import play.api.libs._
import play.api.mvc._
import play.core.routing.ReverseRouteContext
import play.utils.InvalidUriEncodingException
import play.utils.Resources
import play.utils.UriEncoding
import play.utils.ExecCtxUtils

import scala.annotation.tailrec
import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.blocking
import scala.util.control.NonFatal
import scala.util.matching.Regex
import scala.util.Failure
import scala.util.Success

class AssetsModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration) = Seq(
    bind[Assets].toSelf,
    bind[AssetsMetadata].toProvider[AssetsMetadataProvider],
    bind[AssetsFinder].toProvider[AssetsFinderProvider],
    bind[AssetsConfiguration].toProvider[AssetsConfigurationProvider]
  )
}

class AssetsFinderProvider @Inject() (assetsMetadata: AssetsMetadata) extends Provider[AssetsFinder] {
  def get = assetsMetadata.finder
}

/**
 * A provider for [[AssetsMetadata]] that sets up necessary static state for reverse routing. The
 * [[play.api.mvc.PathBindable PathBindable]] for assets does additional "magic" using statics so routes
 * like `routes.Assets.versioned("foo.js")` will find the minified and digested version of that asset.
 *
 * It is also possible to avoid this provider and simply inject [[AssetsFinder]]. Then you can call
 * `AssetsFinder.path` to get the final path of an asset according to the path and url prefix in configuration.
 */
@Singleton
class AssetsMetadataProvider @Inject() (
    env: Environment,
    config: AssetsConfiguration,
    fileMimeTypes: FileMimeTypes,
    lifecycle: ApplicationLifecycle
) extends Provider[DefaultAssetsMetadata] {
  lazy val get = {
    import StaticAssetsMetadata.instance
    val assetsMetadata = new DefaultAssetsMetadata(env, config, fileMimeTypes)
    StaticAssetsMetadata.synchronized {
      instance = Some(assetsMetadata)
    }
    lifecycle.addStopHook(() => {
      StaticAssetsMetadata.synchronized {
        // Set instance to None if this application was the last to set the instance.
        // Otherwise it's the responsibility of whoever set it last to unset it.
        // We don't want to break a running application that needs a static instance.
        if (instance contains assetsMetadata) {
          instance = None
        }
      }
      Future.unit
    })
    assetsMetadata
  }
}

trait AssetsComponents {
  def configuration: Configuration
  def environment: Environment
  def httpErrorHandler: HttpErrorHandler
  def fileMimeTypes: FileMimeTypes
  def applicationLifecycle: ApplicationLifecycle

  lazy val assetsConfiguration: AssetsConfiguration =
    AssetsConfiguration.fromConfiguration(configuration, environment.mode)

  lazy val assetsMetadata: AssetsMetadata =
    new AssetsMetadataProvider(environment, assetsConfiguration, fileMimeTypes, applicationLifecycle).get

  def assetsFinder: AssetsFinder = assetsMetadata.finder

  lazy val assets: Assets = new Assets(httpErrorHandler, assetsMetadata)
}

import Execution.trampoline

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
        val f = Future(pf(k))(ExecCtxUtils.prepare(ec))
        f.onComplete {
          case Failure(_) | Success(None) => store.remove(k)
          case _                          => // Do nothing, the asset was successfully found and is now cached
        }
        p.completeWith(f)
        p.future
    }
  }
}

case class AssetsConfiguration(
    path: String = "/public",
    urlPrefix: String = "/assets",
    defaultCharSet: String = "utf-8",
    enableCaching: Boolean = true,
    enableCacheControl: Boolean = false,
    configuredCacheControl: Map[String, Option[String]] = Map.empty,
    defaultCacheControl: String = "public, max-age=3600",
    aggressiveCacheControl: String = "public, max-age=31536000, immutable",
    digestAlgorithm: String = "md5",
    checkForMinified: Boolean = true,
    textContentTypes: Set[String] = Set("application/json", "application/javascript"),
    encodings: Seq[AssetEncoding] = Seq(
      AssetEncoding.Brotli,
      AssetEncoding.Gzip,
      AssetEncoding.Xz,
      AssetEncoding.Bzip2
    )
) {
  // Sorts configured cache-control by keys so that we can have from more
  // specific configuration to less specific, where the overall sorting is
  // done lexicographically. For example, given the following keys:
  // - /a
  // - /a/b/c.txt
  // - /a/b
  // - /a/z
  // - /d/e/f.txt
  // - /d
  // - /d/f
  //
  // They will be sorted to:
  // - /a/b/c.txt
  // - /a/b
  // - /a/z
  // - /a
  // - /d/e/f.txt
  // - /d/f
  // - /d
  private lazy val configuredCacheControlDirectivesOrdering = new Ordering[(String, Option[String])] {
    override def compare(first: (String, Option[String]), second: (String, Option[String])) = {
      val firstKey  = first._1
      val secondKey = second._1

      if (firstKey.startsWith(secondKey)) -1
      else if (secondKey.startsWith(firstKey)) 1
      else firstKey.compareTo(secondKey)
    }
  }

  private lazy val configuredCacheControlDirectives: List[(String, Option[String])] = {
    configuredCacheControl.toList.sorted(configuredCacheControlDirectivesOrdering)
  }

  /**
   * Finds the configured Cache-Control directive that needs to be applied to the asset
   * with the given name.
   *
   * This will try to find the most specific directive configured for the asset. For example,
   * given the following configuration:
   *
   * {{{
   *   "play.assets.cache./public/css"="max-age=100"
   *   "play.assets.cache./public/javascript"="max-age=200"
   *   "play.assets.cache./public/javascript/main.js"="max-age=300"
   * }}}
   *
   * Given asset name "/public/css/main.css", it will find "max-age=100".
   *
   * Given asset name "/public/javascript/other.js" it will find "max-age=200".
   *
   * Given asset name "/public/javascript/main.js" it will find "max-age=300".
   *
   * Given asset name "/public/images/img.png" it will use the [[defaultCacheControl]] since
   * there is no specific directive configured for this asset.
   *
   * @param assetName the asset name
   * @return the optional configured cache-control directive.
   */
  final def findConfiguredCacheControl(assetName: String): Option[String] = {
    configuredCacheControlDirectives.find(c => assetName.startsWith(c._1)).flatMap(_._2)
  }
}

case class AssetEncoding(acceptEncoding: String, extension: String) {
  def forFilename(filename: String): String = if (extension != "") s"$filename.$extension" else filename
}

object AssetEncoding {
  val Brotli = AssetEncoding(ContentEncoding.Brotli, "br")
  val Gzip   = AssetEncoding(ContentEncoding.Gzip, "gz")
  val Bzip2  = AssetEncoding(ContentEncoding.Bzip2, "bz2")
  val Xz     = AssetEncoding(ContentEncoding.Xz, "xz")
}

object AssetsConfiguration {
  private val logger = Logger(getClass)

  def fromConfiguration(c: Configuration, mode: Mode = Mode.Test): AssetsConfiguration = {
    val assetsConfiguration = AssetsConfiguration(
      path = c.get[String]("play.assets.path"),
      urlPrefix = c.get[String]("play.assets.urlPrefix"),
      defaultCharSet = c.getDeprecated[String]("play.assets.default.charset", "default.charset"),
      enableCaching = mode != Mode.Dev,
      enableCacheControl = mode == Mode.Prod,
      configuredCacheControl = c.getOptional[Map[String, Option[String]]]("play.assets.cache").getOrElse(Map.empty),
      defaultCacheControl = c.getDeprecated[String]("play.assets.defaultCache", "assets.defaultCache"),
      aggressiveCacheControl = c.getDeprecated[String]("play.assets.aggressiveCache", "assets.aggressiveCache"),
      digestAlgorithm = c.getDeprecated[String]("play.assets.digest.algorithm", "assets.digest.algorithm"),
      checkForMinified = c
        .getDeprecated[Option[Boolean]]("play.assets.checkForMinified", "assets.checkForMinified")
        .getOrElse(mode != Mode.Dev),
      textContentTypes = c.get[Seq[String]]("play.assets.textContentTypes").toSet,
      encodings = getAssetEncodings(c)
    )
    logAssetsConfiguration(assetsConfiguration)
    assetsConfiguration
  }

  private def logAssetsConfiguration(assetsConfiguration: AssetsConfiguration): Unit = {
    val msg = new StringBuffer()
    msg.append("Using the following cache configuration for assets:\n")
    msg.append(s"\t enableCaching = ${assetsConfiguration.enableCaching}\n")
    msg.append(s"\t enableCacheControl = ${assetsConfiguration.enableCacheControl}\n")
    msg.append(s"\t defaultCacheControl = ${assetsConfiguration.defaultCacheControl}\n")
    msg.append(s"\t aggressiveCacheControl = ${assetsConfiguration.aggressiveCacheControl}\n")
    msg.append(s"\t configuredCacheControl:")
    msg.append(
      assetsConfiguration.configuredCacheControl.map(c => s"\t\t ${c._1} = ${c._2}").mkString("\n", "\n", "\n")
    )
    logger.debug(msg.toString)
  }

  private def getAssetEncodings(c: Configuration): Seq[AssetEncoding] = {
    c.get[Seq[Configuration]]("play.assets.encodings")
      .map(configs => AssetEncoding(configs.get[String]("accept"), configs.get[String]("extension")))
  }
}

case class AssetsConfigurationProvider @Inject() (env: Environment, conf: Configuration)
    extends Provider[AssetsConfiguration] {
  def get = AssetsConfiguration.fromConfiguration(conf, env.mode)
}

/**
 * INTERNAL API: provides static access to AssetsMetadata for legacy global state and reverse routing.
 */
private[controllers] object StaticAssetsMetadata extends AssetsMetadata {
  @volatile private[controllers] var instance: Option[AssetsMetadata] = None

  private[this] lazy val defaultAssetsMetadata: AssetsMetadata = {
    val environment   = Environment.simple()
    val configuration = Configuration.reference
    val assetsConfig  = AssetsConfiguration.fromConfiguration(configuration, environment.mode)
    val httpConfig    = HttpConfiguration.fromConfiguration(configuration, environment)
    val fileMimeTypes = new DefaultFileMimeTypes(httpConfig.fileMimeTypes)

    new DefaultAssetsMetadata(environment, assetsConfig, fileMimeTypes)
  }

  private[this] def delegate: AssetsMetadata = instance.getOrElse(defaultAssetsMetadata)

  /**
   * The configured assets path
   */
  override def finder = delegate.finder
  private[controllers] override def digest(path: String) =
    delegate.digest(path)
  private[controllers] override def assetInfoForRequest(request: RequestHeader, name: String) =
    delegate.assetInfoForRequest(request, name)
}

/**
 * INTERNAL API: Retains metadata for assets that can be readily cached.
 */
trait AssetsMetadata {
  def finder: AssetsFinder
  private[controllers] def digest(path: String): Option[String]
  private[controllers] def assetInfoForRequest(
      request: RequestHeader,
      name: String
  ): Future[Option[(AssetInfo, AcceptEncoding)]]
}

/**
 * Can be used to find assets according to configured base path and URL base.
 */
trait AssetsFinder { self =>

  /**
   * The configured assets path
   */
  def assetsBasePath: String

  /**
   * The configured assets prefix
   */
  def assetsUrlPrefix: String

  /**
   * Get the final path, unprefixed, for a given base assets directory.
   *
   * @param basePath the location to look for the assets
   * @param rawPath the initial path of the asset
   * @return
   */
  def findAssetPath(basePath: String, rawPath: String): String

  /**
   * Used to obtain the final path of an asset according to assets configuration. This returns the minified path,
   * if exists, with a digest if it exists. It is possible to use this in cases where minification and digests
   * are used and where they are not. If no alternative file is found, the original filename is returned.
   *
   * This method is like unprefixedPath, but it prepends the prefix defined in configuration.
   *
   * Note: to get the path without a URL prefix, you can use `this.unprefixed.path(rawPath)`.
   *
   * @param rawPath The original path of the asset
   */
  def path(rawPath: String): String = {
    val base = assetsBasePath
    s"$assetsUrlPrefix/${findAssetPath(base, s"$base/$rawPath")}"
  }

  /**
   * @return an AssetsFinder with no URL prefix
   */
  lazy val unprefixed: AssetsFinder = this.withUrlPrefix("")

  /**
   * Create an AssetsFinder with a custom URL prefix (replacing the current prefix)
   */
  def withUrlPrefix(newPrefix: String): AssetsFinder = new AssetsFinder {
    override def findAssetPath(base: String, path: String) = self.findAssetPath(base, path)
    override def assetsUrlPrefix                           = newPrefix
    override def assetsBasePath                            = self.assetsBasePath
  }

  /**
   * Create an AssetsFinder with a custom assets location (replacing the current assets base path)
   */
  def withAssetsPath(newPath: String): AssetsFinder = new AssetsFinder {
    override def findAssetPath(base: String, path: String) = self.findAssetPath(base, path)
    override def assetsUrlPrefix                           = self.assetsUrlPrefix
    override def assetsBasePath                            = newPath
  }
}

/**
 * Default implementation of [[AssetsMetadata]].
 *
 * If your application uses reverse routing with assets or the [[Assets]] static object, you should use the
 * [[AssetsMetadataProvider]] to set up needed statics.
 */
@Singleton
class DefaultAssetsMetadata(
    config: AssetsConfiguration,
    resource: String => Option[URL],
    fileMimeTypes: FileMimeTypes
) extends AssetsMetadata {
  @Inject
  def this(env: Environment, config: AssetsConfiguration, fileMimeTypes: FileMimeTypes) =
    this(config, env.resource _, fileMimeTypes)

  lazy val finder: AssetsFinder = new AssetsFinder {
    val assetsBasePath  = config.path
    val assetsUrlPrefix = config.urlPrefix

    def findAssetPath(base: String, path: String): String = blocking {
      val minPath = minifiedPath(path)
      digest(minPath)
        .fold(minPath) { dgst =>
          val lastSep = minPath.lastIndexOf("/")
          minPath.take(lastSep + 1) + dgst + "-" + minPath.drop(lastSep + 1)
        }
        .drop(base.length + 1)
    }
  }

  // Caching. It is unfortunate that we require both a digestCache and an assetInfo cache given that digest info is
  // part of asset information. The reason for this is that the assetInfo cache returns a Future[AssetInfo] in order to
  // avoid any thundering herds issue. The unbind method of the assetPathBindable doesn't support the return of a
  // Future - unbinds are expected to be blocking. Thus we separate out the caching of a digest from the caching of
  // full asset information. At least the determination of the digest should be relatively quick (certainly not as
  // involved as determining the full asset info).

  private lazy val digestCache = TrieMap[String, Option[String]]()

  private[controllers] def digest(path: String): Option[String] = {
    digestCache.getOrElse(
      path, {
        val maybeDigestUrl: Option[URL] = resource(path + "." + config.digestAlgorithm)
        val maybeDigest: Option[String] = maybeDigestUrl.map(scala.io.Source.fromURL(_).mkString.trim)
        if (config.enableCaching && maybeDigest.isDefined) digestCache.put(path, maybeDigest)
        maybeDigest
      }
    )
  }

  // Sames goes for the minified paths cache.
  private lazy val minifiedPathsCache = TrieMap[String, String]()

  private def minifiedPath(path: String): String = {
    minifiedPathsCache.getOrElse(
      path, {
        def minifiedPathFor(delim: Char): Option[String] = {
          val ext       = path.reverse.takeWhile(_ != '.').reverse
          val noextPath = path.dropRight(ext.length + 1)
          val minPath   = noextPath + delim + "min." + ext
          resource(minPath).map(_ => minPath)
        }
        val maybeMinifiedPath = if (config.checkForMinified) {
          minifiedPathFor('.').orElse(minifiedPathFor('-')).getOrElse(path)
        } else {
          path
        }
        if (config.enableCaching) minifiedPathsCache.put(path, maybeMinifiedPath)
        maybeMinifiedPath
      }
    )
  }

  private lazy val assetInfoCache = new SelfPopulatingMap[String, AssetInfo]()

  private def assetInfoFromResource(name: String): Option[AssetInfo] = blocking {
    for (url <- resource(name)) yield {
      val compressionUrls: Seq[(String, URL)] = config.encodings
        .map(ae => (ae.acceptEncoding, resource(ae.forFilename(name))))
        .collect { case (key: String, Some(url: URL)) => (key, url) }

      new AssetInfo(name, url, compressionUrls, digest(name), config, fileMimeTypes)
    }
  }

  private def assetInfo(name: String): Future[Option[AssetInfo]] = {
    if (config.enableCaching) {
      assetInfoCache.putIfAbsent(name)(assetInfoFromResource)
    } else {
      Future.successful(assetInfoFromResource(name))
    }
  }

  private[controllers] def assetInfoForRequest(
      request: RequestHeader,
      name: String
  ): Future[Option[(AssetInfo, AcceptEncoding)]] = {
    assetInfo(name).map(_.map(_ -> AcceptEncoding.forRequest(request)))
  }
}

/*
 * Retain meta information regarding an asset.
 */
private class AssetInfo(
    val name: String,
    val url: URL,
    val compressedUrls: Seq[(String, URL)],
    val digest: Option[String],
    config: AssetsConfiguration,
    fileMimeTypes: FileMimeTypes
) {
  import ResponseHeader._
  import config._

  private val encodingNames: Seq[String]        = compressedUrls.map(_._1)
  private val encodingsByName: Map[String, URL] = compressedUrls.toMap

  // Determines whether we need to Vary: Accept-Encoding on the encoding because there are multiple available
  val varyEncoding: Boolean = compressedUrls.nonEmpty

  /**
   * tells you if mimeType is text or not.
   * Useful to determine whether the charset suffix should be attached to Content-Type or not
   *
   * @param mimeType mimeType to check
   * @return true if mimeType is text
   */
  private def isText(mimeType: String): Boolean = {
    mimeType.trim match {
      case text if text.startsWith("text/")               => true
      case text if config.textContentTypes.contains(text) => true
      case _                                              => false
    }
  }

  def addCharsetIfNeeded(mimeType: String): String =
    if (isText(mimeType)) s"$mimeType; charset=$defaultCharSet" else mimeType

  val configuredCacheControl: Option[String] = config.findConfiguredCacheControl(name)

  def cacheControl(aggressiveCaching: Boolean): String = {
    configuredCacheControl.getOrElse {
      if (enableCacheControl) {
        if (aggressiveCaching) aggressiveCacheControl else defaultCacheControl
      } else {
        "no-cache"
      }
    }
  }

  val lastModified: Option[String] = {
    def getLastModified[T <: URLConnection](f: (T) => Long): Option[String] = {
      Option(url.openConnection)
        .map {
          case urlConnection: T @unchecked =>
            try f(urlConnection)
            finally Resources.closeUrlConnection(urlConnection)
        }
        .filterNot(_ == -1)
        .map(millis => httpDateFormat.format(Instant.ofEpochMilli(millis)))
    }

    url.getProtocol match {
      case "file"   => Some(httpDateFormat.format(Instant.ofEpochMilli(new File(url.toURI).lastModified)))
      case "jar"    => getLastModified[JarURLConnection](c => c.getJarEntry.getTime)
      case "bundle" => getLastModified[URLConnection](c => c.getLastModified)
      case _        => None
    }
  }

  val etag: Option[String] =
    digest
      .orElse(lastModified.map(m => Codecs.sha1(m + " -> " + url.toExternalForm)))
      .map(etag => s""""$etag"""")

  val mimeType: String = fileMimeTypes.forFileName(name).fold(ContentTypes.BINARY)(addCharsetIfNeeded)

  lazy val parsedLastModified = lastModified.flatMap(Assets.parseModifiedDate)

  def bestEncoding(acceptEncoding: AcceptEncoding): Option[String] =
    acceptEncoding
      .preferred(encodingNames)
      .filter(_ != ContentEncoding.Identity) // ignore identity encoding

  // NOTE: we are assuming all clients can accept the unencoded version. Technically the if the `identity` encoding
  // is given a q-value of zero, that's not the case, but in practice that is quite rare so we have chosen not to
  // handle that case.
  def url(acceptEncoding: AcceptEncoding): URL =
    bestEncoding(acceptEncoding).flatMap(encodingsByName.get).getOrElse(url)
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
 * "play.assets.digest.algorithm" = "sha1"
 * }}}
 *
 * You can set a custom Cache directive for a particular resource if needed. For example in your application.conf file:
 *
 * {{{
 * "play.assets.cache./public/images/logo.png" = "max-age=3600"
 * }}}
 *
 * You can use this controller in any application, just by declaring the appropriate route. For example:
 * {{{
 * GET     /assets/\uFEFF*file               controllers.Assets.at(path="/public", file)
 * }}}
 */
object Assets {
  private val logger = Logger(getClass)

  import ResponseHeader.basicDateFormatPattern

  val standardDateParserWithoutTZ: DateTimeFormatter =
    DateTimeFormatter.ofPattern(basicDateFormatPattern).withLocale(java.util.Locale.ENGLISH).withZone(ZoneOffset.UTC)
  val alternativeDateFormatWithTZOffset: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm:ss 'GMT'Z").withLocale(java.util.Locale.ENGLISH)

  /**
   * A regex to find two types of date format. This regex silently ignores any
   * trailing info such as extra header attributes ("; length=123") or
   * timezone names ("(Pacific Standard Time").
   * - "Sat, 18 Oct 2014 20:41:26" and "Sat, 29 Oct 1994 19:43:31 GMT" use the first
   * matcher. (The " GMT" is discarded to give them the same format.)
   * - "Wed Jan 07 2015 22:54:20 GMT-0800" uses the second matcher.
   */
  private val dateRecognizer = Pattern.compile(
    """^(((\w\w\w, \d\d \w\w\w \d\d\d\d \d\d:\d\d:\d\d)(( GMT)?))|""" +
      """(\w\w\w \w\w\w \d\d \d\d\d\d \d\d:\d\d:\d\d GMT.\d\d\d\d))(\b.*)"""
  )

  def parseModifiedDate(date: String): Option[Date] = {
    val matcher = dateRecognizer.matcher(date)
    if (matcher.matches()) {
      val standardDate = matcher.group(3)
      try {
        if (standardDate != null) {
          Some(Date.from(ZonedDateTime.parse(standardDate, standardDateParserWithoutTZ).toInstant))
        } else {
          val alternativeDate = matcher.group(6) // Cannot be null otherwise match would have failed
          Some(Date.from(ZonedDateTime.parse(alternativeDate, alternativeDateFormatWithTZOffset).toInstant))
        }
      } catch {
        case e: IllegalArgumentException =>
          logger.debug(s"An invalid date was received: couldn't parse: $date", e)
          None
        case e: DateTimeParseException =>
          logger.debug(s"An invalid date was received: couldn't parse: $date", e)
          None
      }
    } else {
      logger.debug(s"An invalid date was received: unrecognized format: $date")
      None
    }
  }

  /**
   * An asset.
   *
   * @param name The name of the asset.
   */
  case class Asset(name: String)

  object Asset {
    import scala.language.implicitConversions

    implicit def string2Asset(name: String): Asset = new Asset(name)

    private def pathFromParams(rrc: ReverseRouteContext): String = {
      rrc.fixedParams
        .getOrElse(
          "path",
          throw new RuntimeException(
            "Asset path bindable must be used in combination with an action that accepts a path parameter"
          )
        )
        .toString
    }

    // This uses StaticAssetsMetadata to obtain the full path to the asset.
    implicit def assetPathBindable(implicit rrc: ReverseRouteContext) = new PathBindable[Asset] {
      def bind(key: String, value: String) = Right(new Asset(value))

      def unbind(key: String, value: Asset): String = {
        val base = pathFromParams(rrc)
        val path = base + "/" + value.name
        StaticAssetsMetadata.finder.findAssetPath(base, path)
      }
    }
  }
}

@Singleton
class Assets @Inject() (errorHandler: HttpErrorHandler, meta: AssetsMetadata) extends AssetsBuilder(errorHandler, meta)

class AssetsBuilder(errorHandler: HttpErrorHandler, meta: AssetsMetadata) extends ControllerHelpers {
  import meta._
  import Assets._

  protected val Action: ActionBuilder[Request, AnyContent] = new ActionBuilder.IgnoringBody()(Execution.trampoline)

  private def maybeNotModified(
      request: RequestHeader,
      assetInfo: AssetInfo,
      aggressiveCaching: Boolean
  ): Option[Result] = {
    // First check etag. Important, if there is an If-None-Match header, we MUST not check the
    // If-Modified-Since header, regardless of whether If-None-Match matches or not. This is in
    // accordance with section 14.26 of RFC2616.
    request.headers.get(IF_NONE_MATCH) match {
      case Some(etags) =>
        assetInfo.etag
          .filter(someEtag => etags.split(',').exists(_.trim == someEtag))
          .flatMap(_ => Some(cacheableResult(assetInfo, aggressiveCaching, NotModified)))
      case None =>
        for {
          ifModifiedSinceStr <- request.headers.get(IF_MODIFIED_SINCE)
          ifModifiedSince    <- parseModifiedDate(ifModifiedSinceStr)
          lastModified       <- assetInfo.parsedLastModified
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

  private def asEncodedResult(response: Result, acceptEncoding: AcceptEncoding, assetInfo: AssetInfo): Result = {
    assetInfo
      .bestEncoding(acceptEncoding)
      .map(enc => response.withHeaders(VARY -> ACCEPT_ENCODING, CONTENT_ENCODING -> enc))
      .getOrElse(if (assetInfo.varyEncoding) response.withHeaders(VARY -> ACCEPT_ENCODING) else response)
  }

  /**
   * Generates an `Action` that serves a static resource, using the base asset path from configuration.
   */
  def at(file: String): Action[AnyContent] = at(finder.assetsBasePath, file)

  /**
   * Generates an `Action` that serves a versioned static resource, using the base asset path from configuration.
   */
  def versioned(file: String): Action[AnyContent] = versioned(finder.assetsBasePath, Asset(file))

  /**
   * Generates an `Action` that serves a versioned static resource.
   */
  def versioned(path: String, file: Asset): Action[AnyContent] = Action.async { implicit request =>
    val f = new File(file.name)
    // We want to detect if it's a fingerprinted asset, because if it's fingerprinted, we can aggressively cache it,
    // otherwise we can't.
    val requestedDigest = f.getName.takeWhile(_ != '-')
    if (!requestedDigest.isEmpty) {
      val bareFile     = new File(f.getParent, f.getName.drop(requestedDigest.length + 1)).getPath.replace('\\', '/')
      val bareFullPath = path + "/" + bareFile
      blocking(digest(bareFullPath)) match {
        case Some(`requestedDigest`) => assetAt(path, bareFile, aggressiveCaching = true)
        case _                       => assetAt(path, file.name, aggressiveCaching = false)
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
  def at(path: String, file: String, aggressiveCaching: Boolean = false): Action[AnyContent] = Action.async {
    implicit request =>
      assetAt(path, file, aggressiveCaching)
  }

  private def assetAt(path: String, file: String, aggressiveCaching: Boolean)(
      implicit request: RequestHeader
  ): Future[Result] = {
    val assetName: Option[String] = resourceNameAt(path, file)
    val assetInfoFuture: Future[Option[(AssetInfo, AcceptEncoding)]] = assetName
      .map { name =>
        assetInfoForRequest(request, name)
      }
      .getOrElse(Future.successful(None))

    def notFound = errorHandler.onClientError(request, NOT_FOUND, "Resource not found by Assets controller")

    val pendingResult: Future[Result] = assetInfoFuture.flatMap {
      case Some((assetInfo, acceptEncoding)) =>
        val connection = assetInfo.url(acceptEncoding).openConnection()
        // Make sure it's not a directory
        if (Resources.isUrlConnectionADirectory(connection)) {
          Resources.closeUrlConnection(connection)
          notFound
        } else {
          val stream = connection.getInputStream
          val source = StreamConverters.fromInputStream(() => stream)
          // FIXME stream.available does not necessarily return the length of the file. According to the docs "It is never
          // correct to use the return value of this method to allocate a buffer intended to hold all data in this stream."
          val result = RangeResult.ofSource(
            stream.available(),
            source,
            request.headers.get(RANGE),
            None,
            Option(assetInfo.mimeType)
          )

          Future.successful(maybeNotModified(request, assetInfo, aggressiveCaching).getOrElse {
            cacheableResult(
              assetInfo,
              aggressiveCaching,
              asEncodedResult(result, acceptEncoding, assetInfo)
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
        errorHandler.onServerError(
          request,
          new RuntimeException(s"Unexpected error while serving $file at $path: " + e.getMessage, e)
        )
    }
  }

  /**
   * Get the name of the resource for a static resource. Used by `at`.
   *
   * @param path the root folder for searching the static resource files, such as `"/public"`. Not URL encoded.
   * @param file the file part extracted from the URL. May be URL encoded (note that %2F decodes to literal /).
   */
  private[controllers] def resourceNameAt(path: String, file: String): Option[String] = {
    val decodedFile  = UriEncoding.decodePath(file, "utf-8")
    val resourceName = removeExtraSlashes(s"/$path/$decodedFile")
    if (!fileLikeCanonicalPath(resourceName).startsWith(fileLikeCanonicalPath(path))) {
      None
    } else {
      Some(resourceName)
    }
  }

  /**
   * Like File.getCanonicalPath, but works across platforms. Using File.getCanonicalPath caused inconsistent
   * behavior when tested on Windows.
   */
  private def fileLikeCanonicalPath(path: String): String = {
    @tailrec
    def normalizePathSegments(accumulated: Seq[String], remaining: List[String]): Seq[String] = {
      remaining match {
        case Nil => // Return the accumulated result
          accumulated
        case "." :: rest => // Ignore '.' path segments
          normalizePathSegments(accumulated, rest)
        case ".." :: rest => // Remove last segment (if possible) when '..' is encountered
          val newAccumulated = if (accumulated.isEmpty) Seq("..") else accumulated.dropRight(1)
          normalizePathSegments(newAccumulated, rest)
        case segment :: rest => // Append new segment
          normalizePathSegments(accumulated :+ segment, rest)
      }
    }
    val splitPath: List[String]      = path.split(filePathSeparators).toList
    val splitNormalized: Seq[String] = normalizePathSegments(Vector.empty, splitPath)
    splitNormalized.mkString("/")
  }

  // Ideally, this should be only '/' (which is a valid separator in Windows) and File.separatorChar, but we
  // need to keep '/', '\' and File.separatorChar so that we can test for Windows '\' separator when running
  // the tests on Linux/macOS.
  private val filePathSeparators = Array('/', '\\', File.separatorChar).distinct

  /** Cache this compiled regular expression. */
  private val extraSlashPattern: Regex = """//+""".r

  /** Remove extra slashes in a string, e.g. "/x///y/" becomes "/x/y/". */
  private def removeExtraSlashes(input: String): String = extraSlashPattern.replaceAllIn(input, "/")
}
