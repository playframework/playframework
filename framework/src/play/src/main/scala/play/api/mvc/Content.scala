package play.api.mvc

/**
 * Generic type representing content to be sent over an HTTP response.
 */
trait Content {

  /**
   * The content String.
   */
  def body: String

  /**
   * The default Content type to use for this content.
   */
  def contentType: String

}