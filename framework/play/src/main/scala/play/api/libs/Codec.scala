package play.api.libs

/**
 * Utilities for Codec operations.
 */
object Codec {

  /**
   * Compute the SHA-1 of a byte array.
   *
   * @param bytes The data to compute.
   * @return SHA-1 encoded as Hex String.
   */
  def sha1(bytes: Array[Byte]): String = {
    import java.security.MessageDigest
    val digest = MessageDigest.getInstance("SHA-1")
    digest.reset()
    digest.update(bytes)
    digest.digest().map(0xFF & _).map { "%02x".format(_) }.foldLeft("") { _ + _ }
  }

  /**
   * Compute the SHA-1 of a String.
   *
   * @param text The text to compute.
   * @return SHA-1 encoded as Hex String.
   */
  def sha1(text: String): String = sha1(text.getBytes)

  private val hexChars = Array('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

  /**
   * Converts a byte array into an array of char denoting a hexadecimal representation
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
   * Converts a byte array into a String denoting a hexadecimal representation
   */
  def toHexString(array: Array[Byte]): String = {
    new String(toHex(array))
  }
}