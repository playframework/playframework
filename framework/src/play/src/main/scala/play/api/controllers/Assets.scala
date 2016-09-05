/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
import play.api._
import play.api.mvc._
import java.net.URL

import scala.concurrent.Future
import play.api.http.LazyHttpErrorHandler
import scala.collection.concurrent.TrieMap
import play.core.routing.ReverseRouteContext

package play.api.controllers {

  sealed trait TrampolineContextProvider {
    implicit def trampoline = play.core.Execution.Implicits.trampoline
  }

}

package controllers {

  import javax.inject.{ Inject, Singleton }

  import play.api.controllers.TrampolineContextProvider
  import play.api.http.HttpErrorHandler

  object Execution extends TrampolineContextProvider

  @Singleton
  class Assets @Inject() (errorHandler: HttpErrorHandler) extends AssetsBuilder(errorHandler)
  import Execution.trampoline

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
        val maybeDigest: Option[String] = maybeDigestUrl.map(scala.io.Source.fromURL(_).mkString)
        if (!isDev && maybeDigest.isDefined) digestCache.put(path, maybeDigest)
        maybeDigest
      })
    }

    // Sames goes for the minified paths cache.
    val minifiedPathsCache = TrieMap[String, String]()

    lazy val checkForMinified = config(_.getDeprecated[Option[Boolean]]("play.assets.checkForMinified", "assets.checkForMinified").getOrElse(!isDev)).getOrElse(true)

    private[controllers] def minifiedPath(path: String): String = {
      minifiedPathsCache.getOrElse(path, {
        def minifiedPathFor(delim: Char): Option[String] = {
          val ext = path.reverse.takeWhile(_ != '.').reverse
          val noextPath = path.dropRight(ext.length + 1)
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
      scala.concurrent.blocking {
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
        assetInfoCache.putIfAbsent(name)(assetInfoFromResource)
      }
    }

    private[controllers] def assetInfoForRequest(request: RequestHeader, name: String): Future[Option[(AssetInfo, Boolean)]] = {
      val gzipRequested = request.headers.get(ACCEPT_ENCODING).exists(_.split(',').exists(_.trim == "gzip"))
      assetInfo(name).map(_.map(_ -> gzipRequested))
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
        rrc.fixedParams.getOrElse("path",
          throw new RuntimeException("Asset path bindable must be used in combination with an action that accepts a path parameter")
        ).toString
      }

      implicit def assetPathBindable(implicit rrc: ReverseRouteContext) = new PathBindable[Asset] {
        def bind(key: String, value: String) = Right(new Asset(value))

        def unbind(key: String, value: Asset): String = {
          val base = pathFromParams(rrc)
          val path = base + "/" + value.name
          scala.concurrent.blocking {
            val minPath = minifiedPath(path)
            digest(minPath).fold(minPath) { dgst =>
              val lastSep = minPath.lastIndexOf("/")
              minPath.take(lastSep + 1) + dgst + "-" + minPath.drop(lastSep + 1)
            }.drop(base.length + 1)
          }
        }
      }
    }
  }

}