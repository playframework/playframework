/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package controllers

import java.io.File
import java.net.{JarURLConnection, URL, URLConnection}
import java.time.{Instant, ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.regex.Pattern

import play.api.http.ContentTypes
import play.api.libs.{Codecs, MimeTypes}
import play.api.{Configuration, Logger, Mode, Play}
import play.api.mvc.ResponseHeader
import play.utils.Resources

/*
 * Retains meta information regarding an asset that can be readily cached.
 */
private object AssetInfo {

  def config[T](lookup: Configuration => T): Option[T] = Play.maybeApplication.map(app => lookup(app.configuration))

  def isDev = Play.maybeApplication.fold(false)(_.mode == Mode.Dev)

  def isProd = Play.maybeApplication.fold(false)(_.mode == Mode.Prod)

  def resource(name: String): Option[URL] = for {
    app <- Play.maybeApplication
    resource <- app.resource(name)
  } yield resource

  lazy val defaultCharSet = config(_.getDeprecated[String]("play.assets.default.charset", "default.charset")).getOrElse("utf-8")

  lazy val defaultCacheControl = config(_.getDeprecated[String]("play.assets.defaultCache", "assets.defaultCache")).getOrElse("public, max-age=3600")

  lazy val aggressiveCacheControl = config(_.getDeprecated[String]("play.assets.aggressiveCache", "assets.aggressiveCache")).getOrElse("public, max-age=31536000")

  lazy val digestAlgorithm = config(_.getDeprecated[String]("play.assets.digest.algorithm", "assets.digest.algorithm")).getOrElse("md5")

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
      """(\w\w\w \w\w\w \d\d \d\d\d\d \d\d:\d\d:\d\d GMT.\d\d\d\d))(\b.*)""")

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
          Logger.debug(s"An invalid date was received: couldn't parse: $date", e)
          None
      }
    } else {
      Logger.debug(s"An invalid date was received: unrecognized format: $date")
      None
    }
  }
}

/*
 * Retain meta information regarding an asset.
 */
private class AssetInfo(
                         val name: String,
                         val url: URL,
                         val gzipUrl: Option[URL],
                         val digest: Option[String]) {

  import AssetInfo._
  import ResponseHeader._

  def addCharsetIfNeeded(mimeType: String): String =
    if (MimeTypes.isText(mimeType)) s"$mimeType; charset=$defaultCharSet" else mimeType

  val configuredCacheControl = config(_.getOptional[String]("\"play.assets.cache." + name + "\"")).flatten

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
        case urlConnection: T@unchecked =>
          try {
            f(urlConnection)
          } finally {
            Resources.closeUrlConnection(urlConnection)
          }
      }.filterNot(_ == -1).map(millis => httpDateFormat.format(Instant.ofEpochMilli(millis)))
    }

    url.getProtocol match {
      case "file" => Some(httpDateFormat.format(Instant.ofEpochMilli(new File(url.toURI).lastModified)))
      case "jar" => getLastModified[JarURLConnection](c => c.getJarEntry.getTime)
      case "bundle" => getLastModified[URLConnection](c => c.getLastModified)
      case _ => None
    }
  }

  val etag: Option[String] =
    digest orElse {
      lastModified map (m => Codecs.sha1(m + " -> " + url.toExternalForm))
    } map ("\"" + _ + "\"")

  val mimeType: String = MimeTypes.forFileName(name).fold(ContentTypes.BINARY)(addCharsetIfNeeded)

  val parsedLastModified = lastModified flatMap parseModifiedDate

  def url(gzipAvailable: Boolean): URL = {
    gzipUrl match {
      case Some(x) => if (gzipAvailable) x else url
      case None => url
    }
  }
}

