/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.MessageDigest
import java.util.Arrays
import java.util.Base64
import java.util.Locale

import scala.collection.mutable.ArrayBuffer
import scala.util.control.NonFatal

import play.api.mvc.request.XForwardedClientCert

/** Strict, bounded parsing for Envoy's text `X-Forwarded-Client-Cert` format. */
private[common] object XForwardedClientCertHeaderParser {

  /**
   * Parser limits. `maxChainLength` counts chain certificates, excluding the leaf certificate that an XFCC `Chain`
   * value repeats.
   */
  final case class Limits(
      maxHeaderBytes: Long,
      maxDecodedBytes: Long,
      maxCertificateBytes: Long,
      maxChainLength: Int
  ) {
    require(maxHeaderBytes >= 0, "maxHeaderBytes must not be negative")
    require(maxDecodedBytes >= 0, "maxDecodedBytes must not be negative")
    require(maxCertificateBytes >= 0, "maxCertificateBytes must not be negative")
    require(maxChainLength >= 0, "maxChainLength must not be negative")
  }

  /**
   * Parse a sanitized-single XFCC field.
   *
   * The returned vector is sequence-shaped so the request model can retain ordered assertions in a future policy,
   * but this policy accepts at most one physical field containing exactly one information element.
   */
  def parse(headerValues: Seq[String], limits: Limits): Either[String, Vector[XForwardedClientCert]] = {
    checkRawSize(headerValues, limits.maxHeaderBytes).flatMap { _ =>
      headerValues match {
        case Seq()      => Right(Vector.empty)
        case Seq(value) => parseElement(value, limits).map(Vector(_))
        case _          => Left("Sanitized X-Forwarded-Client-Cert must occur exactly once")
      }
    }
  }

  private def checkRawSize(values: Seq[String], maximum: Long): Either[String, Unit] = {
    var total = 0L
    val it    = values.iterator
    while (it.hasNext) {
      val value = it.next()
      if (value == null) return Left("X-Forwarded-Client-Cert must not contain a null value")
      val size = value.getBytes(StandardCharsets.UTF_8).length.toLong
      if (size > maximum - total) return Left("X-Forwarded-Client-Cert exceeds maxHeaderBytes")
      total += size
    }
    Right(())
  }

  private def parseElement(value: String, limits: Limits): Either[String, XForwardedClientCert] = {
    val fields = new FieldParser(value).parse()
    fields.flatMap { parsed =>
      val decodedBytes = new DecodedByteBudget(limits.maxDecodedBytes)
      for {
        certificateFromCert   <- parseOptionalCert(parsed.certificate, limits, decodedBytes)
        certificatesFromChain <- parseOptionalChain(parsed.chain, limits, decodedBytes)
        normalized            <- normalizeCertificates(certificateFromCert, certificatesFromChain)
        (certificate, chain) = normalized
        _ <- verifyHash(parsed.hash, certificate)
      } yield XForwardedClientCert(
        parsed.by.toVector,
        parsed.hash,
        certificate,
        chain,
        parsed.subject,
        parsed.uris.toVector,
        parsed.dnsNames.toVector
      )
    }
  }

  private def parseOptionalCert(
      encoded: Option[String],
      limits: Limits,
      decodedBytes: DecodedByteBudget
  ): Either[String, Option[X509Certificate]] = encoded match {
    case None        => Right(None)
    case Some(value) =>
      parsePem(value, 1L, limits, decodedBytes, "Cert").flatMap {
        case Vector(certificate) => Right(Some(certificate))
        case _                   => Left("XFCC Cert must contain exactly one PEM certificate")
      }
  }

  private def parseOptionalChain(
      encoded: Option[String],
      limits: Limits,
      decodedBytes: DecodedByteBudget
  ): Either[String, Vector[X509Certificate]] = encoded match {
    case None        => Right(Vector.empty)
    case Some(value) =>
      // Envoy's Chain includes the leaf, whereas Play's normalized chain does not.
      val maximumCertificates = limits.maxChainLength.toLong + 1L
      parsePem(value, maximumCertificates, limits, decodedBytes, "Chain").flatMap {
        case Vector()     => Left("XFCC Chain must contain at least one PEM certificate")
        case certificates => Right(certificates)
      }
  }

  private def normalizeCertificates(
      certificateFromCert: Option[X509Certificate],
      certificatesFromChain: Vector[X509Certificate]
  ): Either[String, (Option[X509Certificate], Vector[X509Certificate])] =
    (certificateFromCert, certificatesFromChain) match {
      case (None, Vector())                                         => Right(None -> Vector.empty)
      case (Some(leaf), Vector())                                   => normalizedChain(leaf, Vector.empty)
      case (None, chain)                                            => normalizedChain(chain.head, chain.tail)
      case (Some(leaf), chain) if sameCertificate(leaf, chain.head) => normalizedChain(leaf, chain.tail)
      case (Some(_), _)                                             => Left("XFCC Cert and Chain must assert the same leaf certificate")
    }

  private def normalizedChain(
      leaf: X509Certificate,
      chain: Vector[X509Certificate]
  ): Either[String, (Option[X509Certificate], Vector[X509Certificate])] = {
    val certificates = chain.iterator
    while (certificates.hasNext) {
      if (sameCertificate(leaf, certificates.next())) {
        return Left("XFCC Chain must not repeat its leaf certificate")
      }
    }
    Right(Some(leaf) -> chain)
  }

  private def verifyHash(hash: Option[String], certificate: Option[X509Certificate]): Either[String, Unit] =
    (hash, certificate) match {
      case (Some(expected), Some(leaf)) =>
        val actual = hex(MessageDigest.getInstance("SHA-256").digest(leaf.getEncoded))
        Either.cond(actual.equalsIgnoreCase(expected), (), "XFCC Hash does not match the asserted leaf certificate")
      case _ => Right(())
    }

  private def sameCertificate(left: X509Certificate, right: X509Certificate): Boolean =
    Arrays.equals(left.getEncoded, right.getEncoded)

  private def hex(bytes: Array[Byte]): String = {
    val characters = "0123456789abcdef"
    val result     = new StringBuilder(bytes.length * 2)
    var index      = 0
    while (index < bytes.length) {
      val value = bytes(index) & 0xff
      result.append(characters.charAt(value >>> 4))
      result.append(characters.charAt(value & 0x0f))
      index += 1
    }
    result.result()
  }

  private def parsePem(
      encoded: String,
      maximumCertificates: Long,
      limits: Limits,
      decodedBytes: DecodedByteBudget,
      fieldName: String
  ): Either[String, Vector[X509Certificate]] =
    strictPercentDecode(encoded).flatMap { bytes =>
      parseDecodedPem(
        new String(bytes, StandardCharsets.US_ASCII),
        maximumCertificates,
        limits,
        decodedBytes,
        fieldName
      )
    }

  private def parseDecodedPem(
      pem: String,
      maximumCertificates: Long,
      limits: Limits,
      decodedBytes: DecodedByteBudget,
      fieldName: String
  ): Either[String, Vector[X509Certificate]] = {
    val certificates = Vector.newBuilder[X509Certificate]
    var count        = 0L
    var index        = skipPemWhitespace(pem, 0)
    if (index == pem.length) return Left(s"XFCC $fieldName must contain a PEM certificate")

    while (index < pem.length) {
      if (count >= maximumCertificates) {
        return Left(
          if (fieldName == "Cert") "XFCC Cert must contain exactly one PEM certificate"
          else "XFCC Chain exceeds maxChainLength"
        )
      }
      if (!pem.startsWith(BeginCertificate, index)) {
        return Left(s"XFCC $fieldName contains data outside a PEM certificate")
      }
      val contentStart = index + BeginCertificate.length
      val end          = pem.indexOf(EndCertificate, contentStart)
      if (end < 0) return Left(s"XFCC $fieldName has an unterminated PEM certificate")

      decodePemCertificate(pem.substring(contentStart, end), limits, decodedBytes) match {
        case Left(error)        => return Left(error)
        case Right(certificate) => certificates += certificate
      }
      count += 1
      index = skipPemWhitespace(pem, end + EndCertificate.length)
    }
    Right(certificates.result())
  }

  private def strictPercentDecode(value: String): Either[String, Array[Byte]] = {
    val result      = new Array[Byte](value.length)
    var inputIndex  = 0
    var outputIndex = 0
    while (inputIndex < value.length) {
      val char = value.charAt(inputIndex)
      if (char == '%') {
        if (inputIndex + 2 >= value.length) return Left("XFCC certificate contains an invalid percent escape")
        val high = hexDigit(value.charAt(inputIndex + 1))
        val low  = hexDigit(value.charAt(inputIndex + 2))
        if (high < 0 || low < 0) return Left("XFCC certificate contains an invalid percent escape")
        result(outputIndex) = ((high << 4) | low).toByte
        inputIndex += 3
      } else {
        // In particular, '+' stays '+': this is URI percent decoding, not form decoding.
        if (char > 0x7f) return Left("XFCC certificate PEM must contain only ASCII bytes")
        result(outputIndex) = char.toByte
        inputIndex += 1
      }
      outputIndex += 1
    }
    Right(Arrays.copyOf(result, outputIndex))
  }

  private def decodePemCertificate(
      content: String,
      limits: Limits,
      decodedBytes: DecodedByteBudget
  ): Either[String, X509Certificate] = {
    val base64 = new StringBuilder(content.length)
    var index  = 0
    while (index < content.length) {
      val char = content.charAt(index)
      if (isPemWhitespace(char)) ()
      else if (isBase64Character(char) || char == '=') base64.append(char)
      else return Left("XFCC certificate PEM contains invalid Base64 data")
      index += 1
    }

    val encoded = base64.result()
    if (encoded.isEmpty || !validCanonicalBase64Syntax(encoded)) {
      return Left("XFCC certificate PEM contains invalid Base64 data")
    }
    val bytes =
      try Base64.getDecoder.decode(encoded)
      catch {
        case _: IllegalArgumentException => return Left("XFCC certificate PEM contains invalid Base64 data")
      }

    if (!Base64.getEncoder.encodeToString(bytes).equals(encoded)) {
      Left("XFCC certificate PEM contains non-canonical Base64 data")
    } else if (bytes.length.toLong > limits.maxCertificateBytes) {
      Left("An XFCC certificate exceeds maxCertificateBytes")
    } else {
      decodedBytes.add(bytes.length).flatMap(_ => parseCertificate(bytes))
    }
  }

  private def validCanonicalBase64Syntax(value: String): Boolean = {
    if (value.length % 4 != 0) return false
    var padding = false
    var equals  = 0
    var index   = 0
    while (index < value.length) {
      val char = value.charAt(index)
      if (char == '=') {
        padding = true
        equals += 1
        if (equals > 2) return false
      } else if (padding || !isBase64Character(char)) {
        return false
      }
      index += 1
    }
    true
  }

  private def isBase64Character(char: Char): Boolean =
    (char >= 'A' && char <= 'Z') ||
      (char >= 'a' && char <= 'z') ||
      (char >= '0' && char <= '9') ||
      char == '+' || char == '/'

  private def parseCertificate(bytes: Array[Byte]): Either[String, X509Certificate] = {
    val input = new ByteArrayInputStream(bytes)
    try {
      CertificateFactory.getInstance("X.509").generateCertificate(input) match {
        case certificate: X509Certificate if input.available() == 0 && Arrays.equals(certificate.getEncoded, bytes) =>
          Right(certificate)
        case _: X509Certificate => Left("XFCC certificate is not one exact DER-encoded X.509 certificate")
        case _                  => Left("XFCC certificate is not an X.509 certificate")
      }
    } catch {
      case _: CertificateException => Left("XFCC certificate contains invalid X.509 data")
      case NonFatal(_)             => Left("XFCC certificate could not be decoded")
    }
  }

  private def skipPemWhitespace(value: String, start: Int): Int = {
    var index = start
    while (index < value.length && isPemWhitespace(value.charAt(index))) index += 1
    index
  }

  private def isPemWhitespace(char: Char): Boolean =
    char == ' ' || char == '\t' || char == '\r' || char == '\n'

  private def hexDigit(char: Char): Int =
    if (char >= '0' && char <= '9') char - '0'
    else if (char >= 'a' && char <= 'f') char - 'a' + 10
    else if (char >= 'A' && char <= 'F') char - 'A' + 10
    else -1

  private def isTokenCharacter(char: Char): Boolean =
    (char >= 'a' && char <= 'z') ||
      (char >= 'A' && char <= 'Z') ||
      (char >= '0' && char <= '9') ||
      "!#$%&'*+-.^_`|~".indexOf(char) >= 0

  private def isInvalidValueCharacter(char: Char): Boolean =
    char == '\r' || char == '\n' || char == 0x7f || (char < ' ' && char != '\t')

  private val BeginCertificate = "-----BEGIN CERTIFICATE-----"
  private val EndCertificate   = "-----END CERTIFICATE-----"

  private final class DecodedByteBudget(maximum: Long) {
    private var total = 0L

    def add(size: Int): Either[String, Unit] = {
      if (size.toLong > maximum - total) Left("XFCC certificates exceed maxDecodedBytes")
      else {
        total += size
        Right(())
      }
    }
  }

  private final case class ParsedFields(
      by: ArrayBuffer[String] = ArrayBuffer.empty,
      var hash: Option[String] = None,
      var certificate: Option[String] = None,
      var chain: Option[String] = None,
      var subject: Option[String] = None,
      uris: ArrayBuffer[String] = ArrayBuffer.empty,
      dnsNames: ArrayBuffer[String] = ArrayBuffer.empty,
      var recognized: Boolean = false
  )

  private final case class ParsedValue(value: String, quoted: Boolean)

  private final class FieldParser(input: String) {
    private val result = ParsedFields()
    private var index  = 0

    def parse(): Either[String, ParsedFields] = {
      skipOptionalWhitespace()
      if (atEnd) return Left("X-Forwarded-Client-Cert must contain an information element")

      var done = false
      while (!done) {
        val name = parseName() match {
          case Left(error)  => return Left(error)
          case Right(value) => value
        }
        skipOptionalWhitespace()
        if (atEnd || current != '=') return Left("Every XFCC field must have a value")
        index += 1
        skipOptionalWhitespace()

        val value = parseValue() match {
          case Left(error)  => return Left(error)
          case Right(value) => value
        }
        addField(name, value) match {
          case Left(error) => return Left(error)
          case Right(())   => ()
        }

        skipOptionalWhitespace()
        if (atEnd) done = true
        else if (current == ',') {
          return Left("Sanitized X-Forwarded-Client-Cert must contain exactly one information element")
        } else if (current == ';') {
          index += 1
          skipOptionalWhitespace()
          if (atEnd || current == ',' || current == ';') return Left("XFCC contains an empty field")
        } else {
          return Left("XFCC fields must be separated by semicolons")
        }
      }

      Either.cond(result.recognized, result, "XFCC must contain at least one recognized field")
    }

    private def parseName(): Either[String, String] = {
      val start = index
      while (!atEnd && isTokenCharacter(current)) index += 1
      Either.cond(index > start, input.substring(start, index), "XFCC contains an invalid field name")
    }

    private def parseValue(): Either[String, ParsedValue] =
      if (!atEnd && current == '"') parseQuotedValue()
      else parseUnquotedValue()

    private def parseQuotedValue(): Either[String, ParsedValue] = {
      index += 1
      val value = new StringBuilder
      while (!atEnd) {
        val char = current
        if (char == '"') {
          index += 1
          return Right(ParsedValue(value.result(), quoted = true))
        } else if (char == '\\') {
          val backslashesStart = index
          while (!atEnd && current == '\\') index += 1
          val backslashCount = index - backslashesStart
          if (!atEnd && current == '"' && backslashCount % 2 == 1) {
            // Envoy wraps the whole field without adding another escaping layer. An odd backslash run keeps an
            // RFC 2253-escaped quote inside the value, and the complete run must be preserved verbatim.
            value.append("\\" * backslashCount)
            value.append('"')
            index += 1
          } else {
            value.append("\\" * backslashCount)
          }
        } else if (isInvalidValueCharacter(char)) {
          return Left("XFCC values must not contain control characters")
        } else {
          value.append(char)
          index += 1
        }
      }
      Left("XFCC contains an unterminated quoted value")
    }

    private def parseUnquotedValue(): Either[String, ParsedValue] = {
      val start = index
      while (!atEnd && current != ';' && current != ',') {
        if (current == '"' || current == '\\')
          return Left("XFCC quotes and backslashes must be escaped in a quoted value")
        if (isInvalidValueCharacter(current)) return Left("XFCC values must not contain control characters")
        index += 1
      }
      var end = index
      while (end > start && (input.charAt(end - 1) == ' ' || input.charAt(end - 1) == '\t')) end -= 1
      Right(ParsedValue(input.substring(start, end), quoted = false))
    }

    private def addField(name: String, parsedValue: ParsedValue): Either[String, Unit] = {
      val value = parsedValue.value
      name.toLowerCase(Locale.ROOT) match {
        case "by"  => addRepeated(result.by, value, "By")
        case "uri" =>
          // Envoy deliberately emits URI= when URI forwarding is enabled but no URI SAN exists.
          result.recognized = true
          if (value.nonEmpty) result.uris += value
          Right(())
        case "dns"  => addRepeated(result.dnsNames, value, "DNS")
        case "hash" =>
          result.recognized = true
          if (result.hash.isDefined) Left("XFCC Hash must not occur more than once")
          else if (value.length != 64 || !value.forall(hexDigit(_) >= 0))
            Left("XFCC Hash must contain 64 hexadecimal digits")
          else {
            result.hash = Some(value)
            Right(())
          }
        case "cert"    => addSingleton(value, result.certificate, "Cert", value => result.certificate = Some(value))
        case "chain"   => addSingleton(value, result.chain, "Chain", value => result.chain = Some(value))
        case "subject" =>
          result.recognized = true
          if (result.subject.isDefined) Left("XFCC Subject must not occur more than once")
          else if (!parsedValue.quoted) Left("XFCC Subject must be double-quoted")
          else {
            // Envoy and Istio can intentionally emit Subject="".
            result.subject = Some(value)
            Right(())
          }
        case _ => Right(()) // Syntactically valid extension fields remain forward-compatible.
      }
    }

    private def addRepeated(values: ArrayBuffer[String], value: String, name: String): Either[String, Unit] = {
      result.recognized = true
      if (value.isEmpty) Left(s"XFCC $name must not be empty")
      else {
        values += value
        Right(())
      }
    }

    private def addSingleton(
        value: String,
        existing: Option[String],
        name: String,
        set: String => Unit
    ): Either[String, Unit] = {
      result.recognized = true
      if (existing.isDefined) Left(s"XFCC $name must not occur more than once")
      else if (value.isEmpty) Left(s"XFCC $name must not be empty")
      else {
        set(value)
        Right(())
      }
    }

    private def skipOptionalWhitespace(): Unit =
      while (!atEnd && (current == ' ' || current == '\t')) index += 1

    private def atEnd: Boolean = index >= input.length
    private def current: Char  = input.charAt(index)
  }
}
