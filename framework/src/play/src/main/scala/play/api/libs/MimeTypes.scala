package play.api.libs

import scala.collection.JavaConverters._

/**
 * MIME type utilities.
 */
object MimeTypes {

  /**
   * Retrieves the usual MIME type for a given extension.
   *
   * @param ext the file extension, e.g. `txt`
   * @return the MIME type, if defined
   */
  def forExtension(ext: String): Option[String] = types.get(ext)

  /**
   * Retrieves the usual MIME type for a given file name
   *
   * @param name the file name, e.g. `hello.txt`
   * @return the MIME type, if defined
   */
  def forFileName(name: String) = name.split('.').takeRight(1).headOption.flatMap(forExtension(_))

  def types: Map[String, String] = applicationTypes ++ play.libs.MimeTypes.getAll().asScala

  /**
   * Mimetypes defined in the current application, as declared in application.conf
   */
  def applicationTypes: Map[String, String] = play.api.Play.maybeApplication.flatMap { application =>
    application.configuration.getConfig("mimetype").map { config =>
      config.subKeys.map { key =>
        (key, config.getString(key))
      }.collect {
        case ((key, Some(value))) =>
          (key, value)
      }.toMap
    }
  }.getOrElse(Map.empty)

  /**
   * tells you if mimeType is text or not.
   * Useful to determine whether the charset suffix should be attached to Content-Type or not
   * @param mimeType mimeType to check
   * @return true if mimeType is text
   */
  def isText(mimeType: String): Boolean = {
    mimeType.trim match {
      case text if text.startsWith("text/") => true
      case text if additionalText.contains(text) => true
      case _ => false
    }
  }

  lazy val additionalText =
    """
        application/json
        application/javascript
    """.split('\n').map(_.trim).filter(_.size > 0).filter(_(0) != '#')

}
