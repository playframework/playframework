/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.parsers

import java.io.FileOutputStream

import play.api.Play
import play.api.libs.Files.TemporaryFile
import play.api.libs.iteratee.Parsing.MatchInfo
import play.api.libs.iteratee._
import play.api.mvc._
import play.api.mvc.MultipartFormData._
import play.api.http.Status._

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

import play.api.libs.iteratee.Execution.Implicits.trampoline

/**
 * Utilities for handling multipart bodies
 */
object Multipart {

  def multipartParser[A](
    maxDataLength: Int,
    filePartHandler: PartHandler[FilePart[A]]): BodyParser[MultipartFormData[A]] = BodyParser("multipartFormData") { request =>

    val maybeBoundary = for {
      mt <- request.mediaType
      (_, value) <- mt.parameters.find(_._1.equalsIgnoreCase("boundary"))
      boundary <- value
    } yield ("\r\n--" + boundary).getBytes("utf-8")

    maybeBoundary.map { boundary =>
      for {
        // First, we ignore the first boundary.  Note that if the body contains a preamble, this won't work.  But the
        // body never contains a preamble.
        _ <- Traversable.take[Array[Byte]](boundary.size - 2) transform Iteratee.ignore
        // We use the search enumeratee to turn the stream into a stream of data chunks that are either the boundary,
        // or not the boundary, and we parse that
        result <- Parsing.search(boundary) transform parseParts(maxDataLength, filePartHandler)
      } yield {
        result.right.map { reversed =>
          // We built the parts by prepending a list, so we need to reverse them
          val parts = reversed.reverse
          val data = parts.collect {
            case DataPart(key, value) => (key, value)
          }.groupBy(_._1).mapValues(_.map(_._2))
          val files = parts.collect { case file: FilePart[A] => file }
          val bad = parts.collect { case bad: BadPart => bad }
          val missing = parts.collect {
            case missing: MissingFilePart => missing
          }
          MultipartFormData(data, files, bad, missing)
        }
      }
    }.getOrElse {
      Iteratee.flatten(createBadResult("Missing boundary header")(request).
        map(r => Done(Left(r))))
    }
  }

  type PartHandler[A] = PartialFunction[Map[String, String], Iteratee[Array[Byte], A]]

  def handleFilePartAsTemporaryFile: PartHandler[FilePart[TemporaryFile]] = {
    handleFilePart {
      case FileInfo(partName, filename, contentType) =>
        val tempFile = TemporaryFile("multipartBody", "asTemporaryFile")
        import play.core.Execution.Implicits.internalContext
        Iteratee.fold[Array[Byte], FileOutputStream](
          new java.io.FileOutputStream(tempFile.file)) { (os, data) =>
            os.write(data)
            os
          }(internalContext).map { os =>
            os.close()
            tempFile
          }(internalContext)
    }
  }

  private val CRLF = "\r\n".getBytes
  private val CRLFCRLF = CRLF ++ CRLF

  private type ParserInput = MatchInfo[Array[Byte]]
  private type Parser[T] = Iteratee[ParserInput, T]

  /**
   * Recursively parses the parts
   */
  private def parseParts(dataPartLimit: Int,
    filePartHandler: PartHandler[Part],
    parts: List[Part] = Nil): Parser[Either[Result, List[Part]]] = {
    parsePart(dataPartLimit, filePartHandler).flatMap {
      // None, we've reached the end of the body
      case None => Done(Right(parts))
      // The max data part size has been exceeded, return an error
      case Some(MaxDataPartSizeExceeded(_)) => Done(Left(Results.EntityTooLarge))
      // A data part, handled specially so we can calculate the data part limit
      case Some(dp @ DataPart(_, value)) =>
        for {
          // Drop the boundary
          _ <- Iteratee.head
          result <- parseParts(dataPartLimit - value.length, filePartHandler, dp :: parts)
        } yield result
      // All other parts
      case Some(other: Part) =>
        for {
          // Drop the boundary
          _ <- Iteratee.head
          result <- parseParts(dataPartLimit, filePartHandler, other :: parts)
        } yield result
    }
  }

  private def parsePart(dataPartLimit: Int,
    filePartHandler: PartHandler[Part]): Parser[Option[Part]] = {

    // Enumeratee that takes everything up to the boundary
    val takeUpToBoundary = Enumeratee.takeWhile[ParserInput](!_.isMatch)

    // Buffer the header
    val maxHeaderBuffer = Traversable.takeUpTo[Array[Byte]](4 * 1024) transform Iteratee.consume[Array[Byte]]()

    // Collect the headers, returns none if we got the last part, otherwise returns the headers, and the part of the
    // buffer that wasn't part of the headers
    val collectHeaders: Iteratee[Array[Byte], Option[(Map[String, String], Array[Byte])]] = maxHeaderBuffer.map { buffer =>

      // The headers should be split from the part by a double crlf
      val (headerBytes, rest) = Option(buffer).map(b => b.splitAt(b.indexOfSlice(CRLFCRLF))).get

      val headerString = new String(headerBytes, "utf-8").trim
      if (headerString.startsWith("--") || headerString.isEmpty) {
        // It's the last part
        None
      } else {
        val headers = headerString.lines.map { header =>
          val key :: value = header.trim.split(":").toList
          (key.trim.toLowerCase, value.mkString(":").trim)
        }.toMap

        val left = rest.drop(CRLFCRLF.length)
        Some((headers, left))
      }
    }

    // Create a part handler that reads all the different types of parts
    val readPart: PartHandler[Part] = handleDataPart(dataPartLimit)
      .orElse[Map[String, String], Iteratee[Array[Byte], Part]]({
        case FileInfoMatcher(partName, fileName, _) if fileName.trim.isEmpty =>
          Done(MissingFilePart(partName), Input.Empty)
      })
      .orElse(filePartHandler)
      .orElse({
        case headers => Done(BadPart(headers), Input.Empty)
      })

    // Put everything together - take up to the boundary, remove the MatchInfo wrapping, collect headers, and then
    // read the part
    takeUpToBoundary compose Enumeratee.map[MatchInfo[Array[Byte]]](_.content) transform collectHeaders.flatMap {
      case Some((headers, left)) => Iteratee.flatten(readPart(headers).feed(Input.El(left))).map(Some.apply)
      case _ => Done(None)
    }

  }

  case class FileInfo(
    /** Name of the part in HTTP request (e.g. field name) */
    partName: String,

    /** Name of the file */
    fileName: String,

    /** Type of content (e.g. "application/pdf"), or `None` if unspecified. */
    contentType: Option[String])

  private[play] object FileInfoMatcher {

    private def split(str: String) = {
      var buffer = new StringBuffer
      var escape: Boolean = false
      var quote: Boolean = false
      val result = new ListBuffer[String]

      def addPart() = {
        result += buffer.toString.trim
        buffer = new StringBuffer
      }

      str foreach {
        case '\\' =>
          buffer.append('\\')
          escape = true
        case '"' =>
          buffer.append('"')
          if (!escape)
            quote = !quote
          escape = false
        case ';' =>
          if (!quote) {
            addPart()
          } else {
            buffer.append(';')
          }
          escape = false
        case c =>
          buffer.append(c)
          escape = false
      }

      addPart()
      result.toList
    }

    def unapply(headers: Map[String, String]): Option[(String, String, Option[String])] = {

      val KeyValue = """^([a-zA-Z_0-9]+)="(.*)"$""".r

      for {
        values <- headers.get("content-disposition").
          map(split(_).map(_.trim).map {
            // unescape escaped quotes
            case KeyValue(key, v) =>
              (key.trim, v.trim.replaceAll("""\\"""", "\""))
            case key => (key.trim, "")
          }.toMap)

        _ <- values.get("form-data")
        partName <- values.get("name")
        fileName <- values.get("filename")
        contentType = headers.get("content-type")
      } yield (partName, fileName, contentType)
    }
  }

  /**
   * Creates an handler that can work with files in HTTP request parts.
   *
   * {{{
   * import play.core.parsers.Multipart, Multipart.FileInfo
   * import play.api.libs.iteratee.Iteratee
   *
   * val handler = Multipart.handleFilePart[List[Int]] { fileInfo =>
   *   ??? // return corresponding Iteratee[Array[Byte], List[Int]]
   * }
   *
   * // then use it
   * Multipart.multipartParser[List[Int]](1024, handler)
   * }}}
   */
  def handleFilePart[A](handler: FileInfo => Iteratee[Array[Byte], A]): PartHandler[FilePart[A]] = {
    case FileInfoMatcher(partName, fileName, contentType) =>
      val safeFileName = fileName.split('\\').takeRight(1).mkString
      handler(FileInfo(partName, safeFileName, contentType)).
        map(a => FilePart(partName, safeFileName, contentType, a))
  }

  private object PartInfoMatcher {
    def unapply(headers: Map[String, String]): Option[String] = {

      val KeyValue = """^([a-zA-Z_0-9]+)="(.*)"$""".r

      for {
        values <- headers.get("content-disposition").map(
          _.split(";").map(_.trim).map {
            case KeyValue(key, v) => (key.trim, v.trim)
            case key => (key.trim, "")
          }.toMap)
        _ <- values.get("form-data")
        partName <- values.get("name")
      } yield partName
    }
  }

  private def handleDataPart(maxLength: Int): PartHandler[Part] = {
    case headers @ PartInfoMatcher(partName) if !FileInfoMatcher.unapply(headers).isDefined =>
      Traversable.takeUpTo[Array[Byte]](maxLength)
        .transform(Iteratee.consume[Array[Byte]]().map(bytes => DataPart(partName, new String(bytes, "utf-8"))))
        .flatMap { data =>
          Cont({
            case Input.El(_) => Done(MaxDataPartSizeExceeded(partName), Input.Empty)
            case in => Done(data, in)
          })
        }
  }

  private def createBadResult(msg: String): RequestHeader => Future[Result] =
    { request =>
      Play.maybeApplication.fold(Future.successful[Result](Results.BadRequest))(
        _.errorHandler.onClientError(request, BAD_REQUEST, msg))
    }
}
