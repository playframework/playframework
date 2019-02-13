/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import com.google.common.io.BaseEncoding

/**
 * Utilities for Codecs operations.
 */
object Codecs {

  private def hexEncoder = BaseEncoding.base16.lowerCase
  private def sha1MessageDigest = MessageDigest.getInstance("SHA-1")
  private def md5MessageDigest = MessageDigest.getInstance("MD5")

  /**
   * Computes the SHA-1 digest for a byte array.
   *
   * @param bytes the data to hash
   * @return the SHA-1 digest, encoded as a hex string
   */
  def sha1(bytes: Array[Byte]): String = toHexString(sha1MessageDigest.digest(bytes))

  /**
   * Computes the MD5 digest for a byte array.
   *
   * @param bytes the data to hash
   * @return the MD5 digest, encoded as a hex string
   */
  def md5(bytes: Array[Byte]): String = toHexString(md5MessageDigest.digest(bytes))
  /**
   * Computes the MD5 digest for a String.
   *
   * @param text the data to hash
   * @return the MD5 digest, encoded as a hex string
   */
  def md5(text: String): String = toHexString(md5MessageDigest.digest(text.getBytes(StandardCharsets.UTF_8)))
  /**
   * Compute the SHA-1 digest for a `String`.
   *
   * @param text the text to hash
   * @return the SHA-1 digest, encoded as a hex string
   */
  def sha1(text: String): String = toHexString(sha1MessageDigest.digest(text.getBytes(StandardCharsets.UTF_8)))

  /**
   * Converts a byte array into an array of characters that denotes a hexadecimal representation.
   */
  def toHex(array: Array[Byte]): Array[Char] = toHexString(array).toCharArray

  /**
   * Converts a byte array into a `String` that denotes a hexadecimal representation.
   */
  def toHexString(array: Array[Byte]): String = hexEncoder.encode(array)

  /**
   * Transform an hexadecimal String to a byte array.
   */
  def hexStringToByte(hexString: String): Array[Byte] = hexEncoder.decode(hexString.toLowerCase)

}
