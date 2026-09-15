/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.gzip

import java.util.function.BiFunction
import java.util.zip.Deflater

import scala.jdk.FunctionConverters._

import com.typesafe.config.ConfigMemorySize
import jakarta.inject.Inject
import jakarta.inject.Provider
import jakarta.inject.Singleton
import org.apache.pekko.stream.Materializer
import play.api.http._
import play.api.inject._
import play.api.libs.streams.GzipFlow
import play.api.mvc._
import play.api.Configuration
import play.api.Logger
import play.filters.encoding.ContentEncodingFilter

/**
 * A gzip filter.
 *
 * This filter may gzip the responses for any requests that aren't HEAD requests and specify an accept encoding of gzip.
 *
 * It won't gzip under the following conditions:
 *
 * - The response code is 204 or 304 (these codes MUST NOT contain a body, and an empty gzipped response is 20 bytes
 * long)
 * - The response already defines a Content-Encoding header
 * - The size of the response body is equal or smaller than a given threshold. If the body size cannot be determined,
 *   then it is assumed the response is over the threshold
 * - A custom shouldGzip function is supplied and it returns false
 *
 * Since gzipping changes the content length of the response, this filter may do some buffering - it will buffer any
 * streamed responses that define a content length less than the configured chunked threshold.  Responses that are
 * greater in length, or that don't define a content length, will not be buffered, but will be sent as chunked
 * responses.
 */
@Singleton
class GzipFilter @Inject() (config: GzipFilterConfig)(implicit mat: Materializer) extends EssentialFilter {
  private val contentEncodingFilter = new ContentEncodingFilter(
    encodingName = "gzip",
    createFlow = () => GzipFlow.gzip(config.bufferSize, config.compressionLevel),
    shouldTranscode = config.shouldGzip,
    chunkedThreshold = config.chunkedThreshold,
    threshold = config.threshold
  )

  def this(
      bufferSize: Int = 8192,
      chunkedThreshold: Int = 102400,
      threshold: Int = 0,
      shouldGzip: (RequestHeader, Result) => Boolean = (_, _) => true,
      compressionLevel: Int = Deflater.DEFAULT_COMPRESSION
  )(implicit mat: Materializer) =
    this(GzipFilterConfig(bufferSize, chunkedThreshold, threshold, shouldGzip, compressionLevel))

  def apply(next: EssentialAction): EssentialAction = contentEncodingFilter(next)
}

/**
 * Configuration for the gzip filter
 *
 * @param bufferSize The size of the buffer to use for gzipping.
 * @param chunkedThreshold The content length threshold, after which the filter will switch to chunking the result.
 * @param threshold The byte threshold for the response body size which controls if a response should be gzipped.
 * @param shouldGzip Whether the given request/result should be gzipped.  This can be used, for example, to implement
 *                   black/white lists for gzipping by content type.
 * @param compressionLevel Compression level to use for the underlying [[java.util.zip.Deflater]] instance.
 */
case class GzipFilterConfig(
    bufferSize: Int = 8192,
    chunkedThreshold: Int = 102400,
    threshold: Int = 0,
    shouldGzip: (RequestHeader, Result) => Boolean = (_, _) => true,
    compressionLevel: Int = Deflater.DEFAULT_COMPRESSION
) {
  // alternate constructor and builder methods for Java
  def this() = this(shouldGzip = (_, _) => true)

  def withShouldGzip(shouldGzip: (RequestHeader, Result) => Boolean): GzipFilterConfig = copy(shouldGzip = shouldGzip)

  def withShouldGzip(shouldGzip: BiFunction[play.mvc.Http.RequestHeader, play.mvc.Result, Boolean]): GzipFilterConfig =
    withShouldGzip((req: RequestHeader, res: Result) => shouldGzip.asScala(req.asJava, res.asJava))

  def withChunkedThreshold(threshold: Int): GzipFilterConfig = copy(chunkedThreshold = threshold)

  def withThreshold(threshold: Int): GzipFilterConfig = copy(threshold = threshold)

  def withBufferSize(size: Int): GzipFilterConfig = copy(bufferSize = size)

  def withCompressionLevel(level: Int): GzipFilterConfig = copy(compressionLevel = level)
}

object GzipFilterConfig {
  private val logger = Logger(this.getClass)

  def fromConfiguration(conf: Configuration): GzipFilterConfig = {
    def parseConfigMediaTypes(config: Configuration, key: String): Seq[MediaType] = {
      val mediaTypes = config.get[Seq[String]](key).flatMap {
        case "*" =>
          // "*" wildcards are accepted for backwards compatibility with when "MediaRange" was used for parsing,
          // but they are not part of the MediaType spec as defined in RFC2616.
          logger.warn(
            "Support for '*' wildcards may be removed in future versions of play," +
              " as they don't conform to the specification for MediaType strings. Use */* instead."
          )
          Some(MediaType("*", "*", Seq.empty))

        case MediaType.parse(mediaType) => Some(mediaType)

        case invalid =>
          logger.error(s"Failed to parse the configured MediaType mask '$invalid'")
          None
      }

      mediaTypes.foreach {
        case MediaType("*", "*", _) =>
          logger.warn(
            "Wildcard MediaTypes don't make much sense in a whitelist (too permissive) or " +
              "blacklist (too restrictive), and are not recommended. "
          )
        case _ => () // the configured MediaType mask is valid
      }

      mediaTypes
    }

    def matches(outgoing: MediaType, mask: MediaType): Boolean = {
      def capturedByMask(value: String, mask: String): Boolean = {
        mask == "*" || value.equalsIgnoreCase(mask)
      }

      capturedByMask(outgoing.mediaType, mask.mediaType) && capturedByMask(outgoing.mediaSubType, mask.mediaSubType)
    }

    val config    = conf.get[Configuration]("play.filters.gzip")
    val whiteList = parseConfigMediaTypes(config, "contentType.whiteList")
    val blackList = parseConfigMediaTypes(config, "contentType.blackList")

    GzipFilterConfig(
      bufferSize = config.get[ConfigMemorySize]("bufferSize").toBytes.toInt,
      chunkedThreshold = config.get[ConfigMemorySize]("chunkedThreshold").toBytes.toInt,
      threshold = config.get[ConfigMemorySize]("threshold").toBytes.toInt,
      shouldGzip = (_, res) =>
        if (whiteList.isEmpty) {
          if (blackList.isEmpty) {
            true // default case, both whitelist and blacklist are empty so we gzip it.
          } else {
            // The blacklist is defined, so we gzip the result if it's not blacklisted.
            res.body.contentType match {
              case Some(MediaType.parse(outgoing)) => blackList.forall(mask => !matches(outgoing, mask))
              case _                               => true // Fail open (to gziping), since blacklists have a tendency to fail open.
            }
          }
        } else {
          // The whitelist is defined. We gzip the result if there is a matching whitelist entry.
          res.body.contentType match {
            case Some(MediaType.parse(outgoing)) => whiteList.exists(mask => matches(outgoing, mask))
            case _                               => false // Fail closed (to not gziping), since whitelists are intentionally strict.
          }
        },
      compressionLevel = config.get[Int]("compressionLevel")
    )
  }
}

/**
 * The gzip filter configuration provider.
 */
@Singleton
class GzipFilterConfigProvider @Inject() (config: Configuration) extends Provider[GzipFilterConfig] {
  lazy val get = GzipFilterConfig.fromConfiguration(config)
}

/**
 * The gzip filter module.
 */
class GzipFilterModule
    extends SimpleModule(
      bind[GzipFilterConfig].toProvider[GzipFilterConfigProvider],
      bind[GzipFilter].toSelf
    )

/**
 * The gzip filter components.
 */
trait GzipFilterComponents {
  def configuration: Configuration
  def materializer: Materializer

  lazy val gzipFilterConfig: GzipFilterConfig = GzipFilterConfig.fromConfiguration(configuration)
  lazy val gzipFilter: GzipFilter             = new GzipFilter(gzipFilterConfig)(using materializer)
}
