package play.api

import java.io._

import scala.util.parsing.input._
import scala.util.parsing.combinator._
import scala.util.matching._
import com.typesafe.config
import com.typesafe.config.{ ConfigFactory, ConfigParseOptions, ConfigSyntax }

/**
 * This object provides a set of operations to create `Configuration` values.
 *
 * For example, to load a `Configuration` from a file:
 * {{{
 * val config = Configuration.fromFile(app.getFile("conf/application.conf"))
 * }}}
 */
object Configuration {

  /**
   * Loads a new configuration from a file.
   *
   * For example:
   * {{{
   * val config = Configuration.fromFile(app.getFile("conf/application.conf"))
   * }}}
   *
   * @param the file configuration file to read
   * @return a `Configuration` instance
   */
  def fromFile(file: File) = {
    import collection.JavaConverters._
    val options = ConfigParseOptions.defaults().setAllowMissing(false).setSyntax(ConfigSyntax.PROPERTIES)
    val currentConfig = ConfigFactory.load(ConfigFactory.parseFile(file, options))
    val javaEntries = currentConfig.entrySet()
    val data = javaEntries.asScala.toSeq.map { e => (e.getKey, Config(e.getKey, e.getValue.unwrapped.toString, file)) }.toMap
    Configuration(data)
  }

  /**
   * reads json/conf style configration from given relative resource
   * @param relative resource to load
   * @return com.typsafe.config.Config more information: https://github.com/havocp/config
   */
  def loadAsJava(resource: String) = ConfigFactory.load(resource)

  def empty = Configuration(Map.empty)

  /**
   * provides a way to allow method calls to underlying Config
   */
  implicit def delegateToConfig(c: RichConfig) = c.underlying

  /**
   * reading json/onf/properties style configration (application.conf,application.json,application.properties) from root resource
   * @param resource to load
   * @return RichConfig which is a wrapper around com.typesafe.config.Config https://github.com/havocp/config
   */
  def load(r: String) = new RichConfig(ConfigFactory.load(r))

  /**
   * A configuration item.
   *
   * @param key The configuration key
   * @param value The configuration value as plain String
   * @param file The file from which this configuration was read
   */
  case class Config(key: String, value: String, file: File) extends Positional

}

/**
 * scalafy com.typsafe.config.Config
 */

class RichConfig(val underlying: config.Config) {
  def get[T](key: String)(implicit m: Manifest[T]): Option[T] = Option(underlying.getAnyRef(key).asInstanceOf[T])
}

/**
 * A full configuration set.
 *
 * The best way to obtain this configuration is to read it from a file using:
 * {{{
 * val config = Configuration.fromFile(app.getFile("conf/application.conf"))
 * }}}
 *
 * @param data the configuration data
 * @param root the root key of this configuration if it represents a sub-configuration
 */
case class Configuration(data: Map[String, Configuration.Config], root: String = "") {

  /**
   * Merge 2 configurations.
   */
  def ++(configuration: Configuration): Configuration = {
    Configuration(
      data = this.absolute.data ++ configuration.absolute.data,
      root = "")
  }

  /**
   * Make this configuration as asbolute (empty root key)
   */
  def absolute: Configuration = {
    Configuration(
      data = this.data.map {
        case (key, config) => (config.key, config)
      },
      root = "")
  }

  /**
   * Retrieves a configuration item by key
   *
   * @param key configuration key, relative to the configuration root key
   * @return a configuration item
   */
  def get(key: String): Option[Configuration.Config] = data.get(key)

  /**
   * Retrieves a configuration value as a `String`.
   *
   * This method supports an optional set of valid values:
   * {{{
   * val mode = configuration.getString("engine.mode", Some(Set("dev","prod")))
   * }}}
   *
   * A configuration error will be thrown if the configuration value does not match any of the required values.
   *
   * @param key the configuration key, relative to configuration root key
   * @param validValues valid values for this configuration
   * @return a configuration value
   */
  def getString(key: String, validValues: Option[Set[String]] = None): Option[String] = data.get(key).map { c =>
    validValues match {
      case Some(values) if values.contains(c.value) => c.value
      case Some(values) if values.isEmpty => c.value
      case Some(values) => throw error("Incorrect value, one of " + (values.reduceLeft(_ + ", " + _)) + " was expected.", c)
      case None => c.value
    }
  }

  /**
   * Retrieves a configuration value as an `Int`.
   *
   * For example:
   * {{{
   * val poolSize = configuration.getInt("engine.pool.size")
   * }}}
   *
   * A configuration error will be thrown if the configuration value is not a valid `Int`.
   *
   * @param key the configuration key, relative to the configuration root key
   * @return a configuration value
   */
  def getInt(key: String): Option[Int] = data.get(key).map { c =>
    try {
      Integer.parseInt(c.value)
    } catch {
      case e => throw error("Integer value required", c)
    }
  }

  /**
   * Retrieves a configuration value as a `Boolean`.
   *
   * For example:
   * {{{
   * val isEnabled = configuration.getString("engine.isEnabled")
   * }}}
   *
   * A configuration error will be thrown if the configuration value is not a valid `Boolean`.
   * Authorized vales are `yes/no or true/false.
   *
   * @param key the configuration key, relative to the configuration root key
   * @return a configuration value
   */
  def getBoolean(key: String): Option[Boolean] = data.get(key).map { c =>
    c.value match {
      case "true" => true
      case "yes" => true
      case "enabled" => true
      case "false" => false
      case "no" => false
      case "disabled" => false
      case o => throw error("Boolean value required", c)
    }
  }

  /**
   * Retrieves a sub-configuration, i.e. a configuration instance containing all keys starting with a given prefix.
   *
   * For example:
   * {{{
   * val engineConfig = configuration.getSub("engine")
   * }}}
   *
   * The root key of this new configuration will be ‘engine’, and you can access any sub-keys relatively.
   *
   * @param key the root prefix for this sub-configuration
   * @return a new configuration
   */
  def getSub(key: String): Option[Configuration] = Option(data.filterKeys(_.startsWith(key + ".")).map {
    case (k, c) => k.drop(key.size + 1) -> c
  }.toMap).filterNot(_.isEmpty).map(Configuration(_, absolute(key) + "."))

  /**
   * Retrieves a sub-configuration, i.e. a configuration instance containing all key starting with a prefix.
   *
   * For example:
   * {{{
   * val engineConfig = configuration.sub("engine")
   * }}}
   *
   * The root key of this new configuration will be ‘engine’, and you can access any sub-keys relatively.
   *
   * This method throw an error if the sub-configuration cannot be found.
   *
   * @param key The root prefix for this sub configuration.
   * @return A new configuration or throw an error.
   */
  def sub(key: String): Configuration = getSub(key).getOrElse {
    throw globalError("No configuration found '" + key + "'")
  }

  /**
   * Returns available keys.
   *
   * @return the set of keys available in this configuration
   */
  def keys: Set[String] = data.keySet

  /**
   * Returns sub-keys.
   *
   * @return the set of direct sub-keys available in this configuration
   */
  def subKeys: Set[String] = keys.map(_.split('.').head)

  /**
   * Creates a configuration error for a specific configuration key.
   *
   * For example:
   * {{{
   * throw configuration.reportError("engine.connectionUrl", "Cannot connect!")
   * }}}
   *
   * @param key the configuration key, related to this error
   * @param message the error message
   * @param e the related exception
   * @return a configuration exception
   */
  def reportError(key: String, message: String, e: Option[Throwable] = None) = {
    data.get(key).map { config =>
      error(message, config, e)
    }.getOrElse {
      new PlayException("Configuration error", absolute(key) + ": " + message, e)
    }
  }

  /**
   * Translates a relative key to an absolute key.
   *
   * @param key the configuration key, relative to configuration root key
   * @return the complete key
   */
  def absolute(key: String) = root + key

  /**
   * Creates a configuration error for this configuration.
   *
   * For example:
   * {{{
   * throw configuration.globalError("Missing configuration key: [yop.url]")
   * }}}
   *
   * @param message the error message
   * @param e the related exception
   * @return a configuration exception
   */
  def globalError(message: String, e: Option[Throwable] = None) = {
    data.headOption.map { c =>
      new PlayException("Configuration error", message, e) with PlayException.ExceptionSource {
        def line = Some(c._2.pos.line)
        def position = None
        def input = Some(scalax.file.Path(c._2.file))
        def sourceName = Some(c._2.file.getAbsolutePath)
      }
    }.getOrElse {
      new PlayException("Configuration error", message, e)
    }
  }

  private def error(message: String, config: Configuration.Config, e: Option[Throwable] = None) = {
    new PlayException("Configuration error", message, e) with PlayException.ExceptionSource {
      def line = Some(config.pos.line)
      def position = Some(config.pos.column + config.key.size)
      def input = Some(scalax.file.Path(config.file))
      def sourceName = Some(config.file.getAbsolutePath)
    }
  }

}
