/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.codec.binary.Hex

/**
 * Utilities for Codecs operations.
 */
object Codecs {

  /**
   * Computes the SHA-1 digest for a byte array.
   *
   * @param bytes the data to hash
   * @return the SHA-1 digest, encoded as a hex string
   */
  def sha1(bytes: Array[Byte]): String = DigestUtils.sha1Hex(bytes)

  /**
   * Computes the MD5 digest for a byte array.
   *
   * @param bytes the data to hash
   * @return the MD5 digest, encoded as a hex string
   */
  def md5(bytes: Array[Byte]): String = DigestUtils.md5Hex(bytes)
  /**
   * Compute the SHA-1 digest for a `String`.
   *
   * @param text the text to hash
   * @return the SHA-1 digest, encoded as a hex string
   */
  def sha1(text: String): String = DigestUtils.sha1Hex(text)

  /**
   * Converts a byte array into an array of characters that denotes a hexadecimal representation.
   */
  def toHex(array: Array[Byte]): Array[Char] = Hex.encodeHex(array)

  /**
   * Converts a byte array into a `String` that denotes a hexadecimal representation.
   */
  def toHexString(array: Array[Byte]): String = Hex.encodeHexString(array)

  /**
   * Transform an hexadecimal String to a byte array.
   */
  def hexStringToByte(hexString: String): Array[Byte] = Hex.decodeHex(hexString.toCharArray)

}
