/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.detailed.filters.csp

import java.nio.charset.{Charset, StandardCharsets}
import java.security.MessageDigest
import java.util.Base64

// #csp-hash-generator
class CSPHashGenerator(digestAlgorithm: String) {

  private val digestInstance: MessageDigest = {
    digestAlgorithm match {
      case "sha256" =>
        MessageDigest.getInstance("SHA-256")
      case "sha384" =>
        MessageDigest.getInstance("SHA-384")
      case "sha512" =>
        MessageDigest.getInstance("SHA-512")
    }
  }

  def generateUTF8(str: String): String = {
    generate(str, StandardCharsets.UTF_8)
  }

  def generate(str: String, charset: Charset): String = {
    val bytes = str.getBytes(charset)
    encode(digestInstance.digest(bytes))
  }

  protected def encode(digestBytes: Array[Byte]): String = {
    val rawHash = Base64.getMimeEncoder.encodeToString(digestBytes)
    s"'$digestAlgorithm-$rawHash'"
  }
}
// #csp-hash-generator