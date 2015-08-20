/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.parsers

import java.io.FileOutputStream

import akka.util.ByteString
import play.api.Play
import play.api.libs.Files.TemporaryFile
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
    filePartHandler: PartHandler[FilePart[A]]): RequestHeader => Iteratee[ByteString, Either[Result, MultipartFormData[A]]] = { request =>

    val maybeBoundary = for {
      mt <- request.mediaType
      (_, value) <- mt.parameters.find(_._1.equalsIgnoreCase("boundary"))
      boundary <- value
    } yield ByteString("\r\n--" + boundary, "utf-8")

    maybeBoundary.map { boundary =>
      for {
        // First, we ignore the first boundary.  Note that if the body contains a preamble, this won't work.  But the
        // body never contains a preamble.
        _ <- Traversable.take[ByteString](boundary.size - 2) transform Iteratee.ignore
        // We use the search enumeratee to turn the stream into a stream of data chunks that are either the boundary,
        // or not the boundary, and we parse that
        result <- search(boundary) transform parseParts(maxDataLength, filePartHandler)
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

  type PartHandler[A] = PartialFunction[Map[String, String], Iteratee[ByteString, A]]

  def handleFilePartAsTemporaryFile: PartHandler[FilePart[TemporaryFile]] = {
    handleFilePart {
      case FileInfo(partName, filename, contentType) =>
        val tempFile = TemporaryFile("multipartBody", "asTemporaryFile")
        import play.core.Execution.Implicits.internalContext
        Iteratee.fold[ByteString, FileOutputStream](
          new java.io.FileOutputStream(tempFile.file)) { (os, data) =>
            os.write(data.toArray)
            os
          }(internalContext).map { os =>
            os.close()
            tempFile
          }(internalContext)
    }
  }

  private val CRLF = ByteString("\r\n", "utf-8")
  private val CRLFCRLF = CRLF ++ CRLF

  private type ParserInput = MatchInfo[ByteString]
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
    val maxHeaderBuffer = Traversable.takeUpTo[ByteString](4 * 1024) transform Iteratee.consume[ByteString]()

    // Collect the headers, returns none if we got the last part, otherwise returns the headers, and the part of the
    // buffer that wasn't part of the headers
    val collectHeaders: Iteratee[ByteString, Option[(Map[String, String], ByteString)]] = maxHeaderBuffer.map { buffer =>

      // The headers should be split from the part by a double crlf
      val (headerBytes, rest) = Option(buffer).map(b => b.splitAt(b.indexOfSlice(CRLFCRLF))).get

      val headerString = headerBytes.utf8String.trim
      if (headerString.startsWith("--") || headerString.isEmpty) {
        // It's the last part
        None
      } else {
        val headers = headerString.lines.map { header =>
          val key :: value = header.trim.split(":").toList
          (key.trim.toLowerCase(java.util.Locale.ENGLISH), value.mkString(":").trim)
        }.toMap

        val left = rest.drop(CRLFCRLF.length)
        Some((headers, left))
      }
    }

    // Create a part handler that reads all the different types of parts
    val readPart: PartHandler[Part] = handleDataPart(dataPartLimit)
      .orElse[Map[String, String], Iteratee[ByteString, Part]]({
        case FileInfoMatcher(partName, fileName, _) if fileName.trim.isEmpty =>
          Done(MissingFilePart(partName), Input.Empty)
      })
      .orElse(filePartHandler)
      .orElse({
        case headers => Done(BadPart(headers), Input.Empty)
      })

    // Put everything together - take up to the boundary, remove the MatchInfo wrapping, collect headers, and then
    // read the part
    takeUpToBoundary compose Enumeratee.map[MatchInfo[ByteString]](_.content) transform collectHeaders.flatMap {
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

    private def split(str: String): List[String] = {
      var buffer = new java.lang.StringBuilder
      var escape: Boolean = false
      var quote: Boolean = false
      val result = new ListBuffer[String]

      def addPart() = {
        result += buffer.toString.trim
        buffer = new java.lang.StringBuilder
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

      val KeyValue = """^([a-zA-Z_0-9]+)="?(.*?)"?$""".r

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
  def handleFilePart[A](handler: FileInfo => Iteratee[ByteString, A]): PartHandler[FilePart[A]] = {
    case FileInfoMatcher(partName, fileName, contentType) =>
      val safeFileName = fileName.split('\\').takeRight(1).mkString
      handler(FileInfo(partName, safeFileName, contentType)).
        map(a => FilePart(partName, safeFileName, contentType, a))
  }

  private[play] object PartInfoMatcher {
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
      Traversable.takeUpTo[ByteString](maxLength)
        .transform(Iteratee.consume[ByteString]().map(bytes => DataPart(partName, bytes.utf8String)))
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

  sealed trait MatchInfo[A] {
    def content: A
    def isMatch = this match {
      case Matched(_) => true
      case Unmatched(_) => false
    }
  }
  case class Matched[A](val content: A) extends MatchInfo[A]
  case class Unmatched[A](val content: A) extends MatchInfo[A]

  def search[A](needle: ByteString): Enumeratee[ByteString, MatchInfo[ByteString]] = new Enumeratee[ByteString, MatchInfo[ByteString]] {
    val needleSize = needle.size
    val fullJump = needleSize
    val jumpBadCharecter: (Byte => Int) = {
      val map = Map(needle.dropRight(1).reverse.zipWithIndex.reverse: _*) //remove the last
      byte => map.get(byte).map(_ + 1).getOrElse(fullJump)
    }

    def applyOn[A](inner: Iteratee[MatchInfo[ByteString], A]): Iteratee[ByteString, Iteratee[MatchInfo[ByteString], A]] = {

      Iteratee.flatten(inner.fold1((a, e) => Future.successful(Done(Done(a, e), Input.Empty: Input[ByteString])),
        k => Future.successful(Cont(step(ByteString(), Cont(k)))),
        (err, r) => throw new Exception()))

    }
    def scan(previousMatches: List[MatchInfo[ByteString]], piece: ByteString, startScan: Int): (List[MatchInfo[ByteString]], ByteString) = {
      if (piece.length < needleSize) {
        (previousMatches, piece)
      } else {
        val fullMatch = Range(needleSize - 1, -1, -1).forall(scan => needle(scan) == piece(scan + startScan))
        if (fullMatch) {
          val (prefix, suffix) = piece.splitAt(startScan)
          val (matched, left) = suffix.splitAt(needleSize)
          val newResults = previousMatches ++ List(Unmatched(prefix), Matched(matched)) filter (!_.content.isEmpty)

          if (left.length < needleSize) (newResults, left) else scan(newResults, left, 0)

        } else {
          val jump = jumpBadCharecter(piece(startScan + needleSize - 1))
          val isFullJump = jump == fullJump
          val newScan = startScan + jump
          if (newScan + needleSize > piece.length) {
            val (prefix, suffix) = (piece.splitAt(startScan))
            (previousMatches ++ List(Unmatched(prefix)), suffix)
          } else scan(previousMatches, piece, newScan)
        }
      }
    }

    def step[A](rest: ByteString, inner: Iteratee[MatchInfo[ByteString], A])(in: Input[ByteString]): Iteratee[ByteString, Iteratee[MatchInfo[ByteString], A]] = {

      in match {
        case Input.Empty => Cont(step(rest, inner)) //here should rather pass Input.Empty along

        case Input.EOF => Done(inner, Input.El(rest))

        case Input.El(chunk) =>
          val all = rest ++ chunk
          def inputOrEmpty(a: ByteString) = if (a.isEmpty) Input.Empty else Input.El(a)

          Iteratee.flatten(inner.fold1((a, e) => Future.successful(Done(Done(a, e), inputOrEmpty(rest))),
            k => {
              val (result, suffix) = scan(Nil, all, 0)
              val fed = result.filter(!_.content.isEmpty).foldLeft(Future.successful(ByteString() -> Cont(k))) { (p, m) =>
                p.flatMap(i => i._2.fold1((a, e) => Future.successful((i._1 ++ m.content, Done(a, e))),
                  k => Future.successful((i._1, k(Input.El(m)))),
                  (err, e) => throw new Exception())
                )
              }
              fed.flatMap {
                case (ss, i) => i.fold1((a, e) => Future.successful(Done(Done(a, e), inputOrEmpty(ss ++ suffix))),
                  k => Future.successful(Cont[ByteString, Iteratee[MatchInfo[ByteString], A]]((in: Input[ByteString]) => in match {
                    case Input.EOF => Done(k(Input.El(Unmatched(suffix))), Input.EOF) //suffix maybe empty
                    case other => step(ss ++ suffix, Cont(k))(other)
                  })),
                  (err, e) => throw new Exception())
              }
            },
            (err, e) => throw new Exception())
          )
      }
    }
  }

}
