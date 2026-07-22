/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Arrays
import java.util.Base64

import scala.collection.mutable.ArrayBuffer
import scala.util.control.NonFatal

import play.api.http.HeaderNames
import play.api.mvc.request.ClientCertificateInfo
import play.api.mvc.request.ClientCertificateSource
import play.api.mvc.Headers

/**
 * Strict, bounded parsing for the RFC 9440 Client-Cert request fields. Malformed fields are ignored
 * independently as required by RFC 8941; configured resource-limit violations remain errors.
 */
private[common] object Rfc9440ClientCertificateParser {

  def parse(headers: Headers, limits: ClientCertificateHeaderLimits): Either[String, Option[ClientCertificateInfo]] = {
    val leafValues  = headers.getAll(HeaderNames.CLIENT_CERT)
    val chainValues = headers.getAll(HeaderNames.CLIENT_CERT_CHAIN)

    checkRawSize(leafValues, chainValues, limits.maxHeaderBytes) match {
      case Left(error) => Left(error.message)
      case Right(_)    =>
        val decodedBytes = new DecodedByteBudget(limits.maxDecodedBytes)
        val leaf         = parseLeaf(leafValues, limits, decodedBytes) match {
          case Left(LimitExceeded(message)) => return Left(message)
          case Left(_: InvalidField)        => None
          case Right(value)                 => value
        }
        val chain = parseChain(chainValues, limits, decodedBytes) match {
          case Left(LimitExceeded(message)) => return Left(message)
          case Left(_: InvalidField)        => Vector.empty
          case Right(value)                 => value
        }

        leaf match {
          case None       => Right(None)
          case Some(cert) =>
            rejectRepeatedLeaf(cert, chain) match {
              case Left(_)      => Right(Some(certificateInfo(cert, Vector.empty)))
              case Right(value) => Right(Some(certificateInfo(cert, value)))
            }
        }
    }
  }

  private sealed trait ParseFailure {
    def message: String
  }

  private final case class InvalidField(message: String)  extends ParseFailure
  private final case class LimitExceeded(message: String) extends ParseFailure

  private type ParseResult[A] = Either[ParseFailure, A]

  private def invalid[A](message: String): ParseResult[A] = Left(InvalidField(message))
  private def limit[A](message: String): ParseResult[A]   = Left(LimitExceeded(message))

  private def certificateInfo(leaf: X509Certificate, chain: Vector[X509Certificate]): ClientCertificateInfo =
    ClientCertificateInfo(leaf, chain, ClientCertificateSource.Rfc9440)

  private def parseLeaf(
      values: Seq[String],
      limits: ClientCertificateHeaderLimits,
      decodedBytes: DecodedByteBudget
  ): ParseResult[Option[X509Certificate]] = {
    if (values.isEmpty) {
      Right(None)
    } else if (values.sizeCompare(1) > 0) {
      invalid("Client-Cert must occur exactly once")
    } else {
      for {
        bytes       <- parseSingleByteSequence(values.head, limits.maxCertificateBytes, decodedBytes)
        certificate <- parseCertificate(bytes)
      } yield Some(certificate)
    }
  }

  private def rejectRepeatedLeaf(
      leaf: X509Certificate,
      chain: Vector[X509Certificate]
  ): ParseResult[Vector[X509Certificate]] = {
    val certificates = chain.iterator
    while (certificates.hasNext) {
      if (Arrays.equals(leaf.getEncoded, certificates.next().getEncoded)) {
        return invalid("Client-Cert-Chain must not repeat the Client-Cert leaf certificate")
      }
    }
    Right(chain)
  }

  private def checkRawSize(
      leafValues: Seq[String],
      chainValues: Seq[String],
      maximum: Long
  ): ParseResult[Unit] = {
    var total = 0L

    def addBytes(size: Long): Boolean = {
      if (size > maximum - total) false
      else {
        total += size
        true
      }
    }

    val leafIterator = leafValues.iterator
    while (leafIterator.hasNext) {
      if (!addBytes(leafIterator.next().getBytes(StandardCharsets.UTF_8).length.toLong)) {
        return limit("Forwarded client certificate fields exceed maxHeaderBytes")
      }
    }

    var firstChainValue = true
    val chainIterator   = chainValues.iterator
    while (chainIterator.hasNext) {
      // Repeated Structured Fields List lines are joined with a comma before parsing.
      if (!firstChainValue && !addBytes(1L)) {
        return limit("Forwarded client certificate fields exceed maxHeaderBytes")
      }
      if (!addBytes(chainIterator.next().getBytes(StandardCharsets.UTF_8).length.toLong)) {
        return limit("Forwarded client certificate fields exceed maxHeaderBytes")
      }
      firstChainValue = false
    }
    Right(())
  }

  private def parseSingleByteSequence(
      value: String,
      maxCertificateBytes: Long,
      decodedBytes: DecodedByteBudget
  ): ParseResult[Array[Byte]] = {
    val parser = new ByteSequenceParser(value, maxCertificateBytes, decodedBytes)
    parser.skipLeadingSpaces()
    for {
      bytes <- parser.parseByteSequenceItem()
      _     <- parser.skipSpacesAndRequireEnd()
    } yield bytes
  }

  private def parseChain(
      values: Seq[String],
      limits: ClientCertificateHeaderLimits,
      decodedBytes: DecodedByteBudget
  ): ParseResult[Vector[X509Certificate]] = {
    if (values.isEmpty) return Right(Vector.empty)

    val certificates = Vector.newBuilder[X509Certificate]
    // Structured Fields combines repeated List field lines with a comma before parsing.
    val parser = new ByteSequenceParser(values.mkString(","), limits.maxCertificateBytes, decodedBytes)
    parser.skipLeadingSpaces()
    parser.parseList(limits.maxChainLength) match {
      case Left(error)  => return Left(error)
      case Right(items) =>
        val itemIterator = items.iterator
        while (itemIterator.hasNext) {
          parseCertificate(itemIterator.next()) match {
            case Left(error)        => return Left(error)
            case Right(certificate) => certificates += certificate
          }
        }
    }
    Right(certificates.result())
  }

  private def parseCertificate(bytes: Array[Byte]): ParseResult[X509Certificate] = {
    val input = new ByteArrayInputStream(bytes)
    try {
      CertificateFactory.getInstance("X.509").generateCertificate(input) match {
        case certificate: X509Certificate if input.available() == 0 && Arrays.equals(certificate.getEncoded, bytes) =>
          Right(certificate)
        case _: X509Certificate => invalid("Client certificate is not one exact DER-encoded X.509 certificate")
        case _                  => invalid("Client certificate is not an X.509 certificate")
      }
    } catch {
      case _: CertificateException => invalid("Client certificate contains invalid X.509 data")
      case NonFatal(_)             => invalid("Client certificate could not be decoded")
    }
  }

  private final class DecodedByteBudget(maximum: Long) {
    private var total = 0L

    def add(size: Int): ParseResult[Unit] = {
      total += size
      if (total <= maximum) Right(()) else limit("Forwarded client certificates exceed maxDecodedBytes")
    }
  }

  /** Minimal RFC 8941 Byte Sequence/List parser for the two RFC 9440 fields. */
  private final class ByteSequenceParser(
      input: String,
      maxCertificateBytes: Long,
      decodedBytes: DecodedByteBudget
  ) {
    private var index = 0

    def skipLeadingSpaces(): Unit = skipSpaces()

    def parseList(maximumItems: Int): ParseResult[Vector[Array[Byte]]] = {
      val result = ArrayBuffer.empty[Array[Byte]]
      if (atEnd) return Right(Vector.empty)

      var done = false
      while (!done) {
        if (result.size >= maximumItems) return limit("Client-Cert-Chain exceeds maxChainLength")
        parseByteSequenceItem() match {
          case Left(error) => return Left(error)
          case Right(item) => result += item
        }
        skipOptionalWhitespace()
        if (atEnd) {
          done = true
        } else if (current == ',') {
          index += 1
          skipOptionalWhitespace()
          if (atEnd) return invalid("Client-Cert-Chain has a trailing empty list member")
        } else {
          return invalid("Client-Cert-Chain members must be Structured Fields Byte Sequences")
        }
      }
      Right(result.toVector)
    }

    def parseByteSequenceItem(): ParseResult[Array[Byte]] =
      for {
        bytes <- parseCertificateByteSequence()
        _     <- parseParameters()
      } yield bytes

    private def parseCertificateByteSequence(): ParseResult[Array[Byte]] = {
      if (atEnd || current != ':') return invalid("Expected a Structured Fields Byte Sequence")
      val encoded = parseByteSequenceContents() match {
        case Left(error)  => return Left(error)
        case Right(value) => value
      }
      if (encoded.isEmpty) return invalid("Client certificate Byte Sequence must not be empty")
      if (!validBase64Syntax(encoded)) return invalid("Client certificate Byte Sequence contains invalid Base64")

      val bytes =
        try Base64.getDecoder.decode(encoded)
        catch {
          case _: IllegalArgumentException => return invalid("Client certificate Byte Sequence contains invalid Base64")
        }
      if (bytes.length > maxCertificateBytes) {
        limit("A client certificate exceeds maxCertificateBytes")
      } else {
        decodedBytes.add(bytes.length).map(_ => bytes)
      }
    }

    private def parseParameters(): ParseResult[Unit] = {
      while (!atEnd && current == ';') {
        index += 1
        skipSpaces()
        parseParameterKey() match {
          case Left(error) => return Left(error)
          case Right(())   => ()
        }
        if (!atEnd && current == '=') {
          index += 1
          parseBareItem() match {
            case Left(error) => return Left(error)
            case Right(())   => ()
          }
        }
      }
      Right(())
    }

    private def parseParameterKey(): ParseResult[Unit] = {
      if (atEnd || (!isLowerAlpha(current) && current != '*')) {
        return invalid("Structured Fields parameter keys must start with a lowercase letter or asterisk")
      }
      index += 1
      while (!atEnd && isParameterKeyCharacter(current)) index += 1
      Right(())
    }

    private def parseBareItem(): ParseResult[Unit] = {
      if (atEnd) return invalid("Structured Fields parameter is missing its value")
      current match {
        case '-'                   => parseNumber()
        case char if isDigit(char) => parseNumber()
        case '"'                   => parseString()
        case '*'                   => parseToken()
        case char if isAlpha(char) => parseToken()
        case ':'                   => parseParameterByteSequence()
        case '?'                   => parseBoolean()
        case _                     => invalid("Structured Fields parameter contains an invalid bare item")
      }
    }

    private def parseNumber(): ParseResult[Unit] = {
      if (current == '-') index += 1
      val integerStart = index
      while (!atEnd && isDigit(current)) index += 1
      val integerDigits = index - integerStart
      if (integerDigits == 0) return invalid("Structured Fields parameter contains an invalid number")

      if (!atEnd && current == '.') {
        if (integerDigits > 12) return invalid("Structured Fields parameter decimal has too many integer digits")
        index += 1
        val fractionalStart = index
        while (!atEnd && isDigit(current)) index += 1
        val fractionalDigits = index - fractionalStart
        if (fractionalDigits >= 1 && fractionalDigits <= 3) Right(())
        else invalid("Structured Fields parameter decimal must have one to three fractional digits")
      } else {
        if (integerDigits <= 15) Right(())
        else invalid("Structured Fields parameter integer has too many digits")
      }
    }

    private def parseString(): ParseResult[Unit] = {
      index += 1
      while (!atEnd) {
        val char = current
        index += 1
        if (char == '"') return Right(())
        if (char == '\\') {
          if (atEnd || (current != '"' && current != '\\')) {
            return invalid("Structured Fields parameter string contains an invalid escape")
          }
          index += 1
        } else if (char < 0x20 || char > 0x7e) {
          return invalid("Structured Fields parameter string must contain only visible ASCII or space")
        }
      }
      invalid("Structured Fields parameter string is unterminated")
    }

    private def parseToken(): ParseResult[Unit] = {
      index += 1
      while (!atEnd && isStructuredTokenCharacter(current)) index += 1
      Right(())
    }

    private def parseParameterByteSequence(): ParseResult[Unit] =
      parseByteSequenceContents().flatMap { encoded =>
        if (validBase64Syntax(encoded)) Right(())
        else invalid("Structured Fields parameter Byte Sequence contains invalid Base64")
      }

    private def parseByteSequenceContents(): ParseResult[String] = {
      index += 1
      val start = index
      while (!atEnd && current != ':') index += 1
      if (atEnd) return invalid("Structured Fields Byte Sequence is missing its closing colon")
      val encoded = input.substring(start, index)
      index += 1
      Right(encoded)
    }

    private def parseBoolean(): ParseResult[Unit] = {
      index += 1
      if (atEnd || (current != '0' && current != '1')) {
        invalid("Structured Fields parameter contains an invalid boolean")
      } else {
        index += 1
        Right(())
      }
    }

    def skipSpacesAndRequireEnd(): ParseResult[Unit] = {
      skipSpaces()
      if (atEnd) Right(()) else invalid("Client-Cert must contain exactly one Structured Fields Byte Sequence")
    }

    private def validBase64Syntax(value: String): Boolean = {
      var padding = false
      var equals  = 0
      var i       = 0
      while (i < value.length) {
        val char = value.charAt(i)
        if (char == '=') {
          padding = true
          equals += 1
          if (equals > 2) return false
        } else if (padding || !isBase64Character(char)) {
          return false
        }
        i += 1
      }
      val unpaddedLength = value.length - equals
      if (equals == 0) value.length % 4 != 1
      else value.length             % 4 == 0 && unpaddedLength % 4 == (if (equals == 1) 3 else 2)
    }

    private def isBase64Character(char: Char): Boolean =
      (char >= 'A' && char <= 'Z') ||
        (char >= 'a' && char <= 'z') ||
        (char >= '0' && char <= '9') ||
        char == '+' || char == '/'

    private def skipSpaces(): Unit =
      while (!atEnd && current == ' ') index += 1

    private def skipOptionalWhitespace(): Unit = {
      while (!atEnd && (current == ' ' || current == '\t')) index += 1
    }

    private def isLowerAlpha(char: Char): Boolean = char >= 'a' && char <= 'z'

    private def isAlpha(char: Char): Boolean =
      (char >= 'a' && char <= 'z') || (char >= 'A' && char <= 'Z')

    private def isDigit(char: Char): Boolean = char >= '0' && char <= '9'

    private def isParameterKeyCharacter(char: Char): Boolean =
      isLowerAlpha(char) || isDigit(char) || char == '_' || char == '-' || char == '.' || char == '*'

    private def isStructuredTokenCharacter(char: Char): Boolean =
      isAlpha(char) ||
        isDigit(char) ||
        "!#$%&'*+-.^_`|~:/".indexOf(char) >= 0

    private def atEnd: Boolean = index >= input.length
    private def current: Char  = input.charAt(index)
  }
}
