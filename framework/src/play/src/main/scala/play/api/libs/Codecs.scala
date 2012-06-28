package play.api.libs

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
  def sha1(bytes: Array[Byte]): String = {
    import java.security.MessageDigest
    val digest = MessageDigest.getInstance("SHA-1")
    digest.reset()
    digest.update(bytes)
    digest.digest().map(0xFF & _).map { "%02x".format(_) }.foldLeft("") { _ + _ }
  }
  
  /**
   * Computes the MD5 digest for a byte array.
   *
   * @param bytes the data to hash
   * @return the MD5 digest, encoded as a hex string
   */
  def md5(bytes: Array[Byte]): String = {
    import java.security.MessageDigest
    val digest = MessageDigest.getInstance("MD5")
    digest.reset()
    digest.update(bytes)
    digest.digest().map(0xFF & _).map { "%02x".format(_) }.foldLeft("") { _ + _ }
  }

  /**
   * Compute the SHA-1 digest for a `String`.
   *
   * @param text the text to hash
   * @return the SHA-1 digest, encoded as a hex string
   */
  def sha1(text: String): String = sha1(text.getBytes)

  // --

  private val hexChars = Array('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

  /**
   * Converts a byte array into an array of characters that denotes a hexadecimal representation.
   */
  def toHex(array: Array[Byte]): Array[Char] = {
    val result = new Array[Char](array.length * 2)
    for (i <- 0 until array.length) {
      val b = array(i) & 0xff
      result(2 * i) = hexChars(b >> 4)
      result(2 * i + 1) = hexChars(b & 0xf)
    }
    result
  }

  /**
   * Converts a byte array into a `String` that denotes a hexadecimal representation.
   */
  def toHexString(array: Array[Byte]): String = {
    new String(toHex(array))
  }

  /**
   * Converts a character that denotes hesadecimal digit into a byte
   */
  private def fromHex(value : Char) : Byte = {
    if (value >= '0' && value <= '9')      (value - '0').toByte
    else if (value >= 'a' && value <= 'f') (10 + value - 'a').toByte
    else if (value >= 'A' && value <= 'F') (10 + value - 'A').toByte
    else 
      throw new IllegalArgumentException("Invalid hexadecimal digit: " + value)
  }

  /**
   * Converts an array of characters that denotes a hexadecimal representation into a byte array.
   */
  def fromHex(array: Array[Char]): Array[Byte] = {
    val result = new Array[Byte](array.length / 2)
     for (i <- 0 until result.length) {
     result(i) = ((fromHex(array(2 * i)) << 4) + fromHex(array(2 * i + 1))).toByte
    }
    result
  }

  /**
   * Converts a String that denotes a hexadecimal representation into a byte array`.
   */
   def fromHexString(value: String) : Array[Byte] = fromHex(value.toCharArray)

}
