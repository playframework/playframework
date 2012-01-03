package play.api

import java.io._

import com.typesafe.config.{ Config, ConfigFactory, ConfigParseOptions, ConfigSyntax, ConfigOrigin, ConfigException }

import scala.collection.JavaConverters._

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
  def load() = {
    try {
      Configuration(Play.maybeApplication.filter(_.mode == Mode.Dev).map(_ => ConfigFactory.load("application")).getOrElse(ConfigFactory.load()))
    } catch {
      case e:ConfigException => throw configError(e.origin, e.getMessage, Some(e))
    }
  }

  def empty = Configuration(ConfigFactory.empty())
  
  def from(data: Map[String,String]) = {
    Configuration(ConfigFactory.parseMap(data.asJava))
  }

  private def configError(origin: ConfigOrigin, message: String, e: Option[Throwable] = None): PlayException = {
    import scalax.io.JavaConverters._
    new PlayException("Configuration error", message, e) with PlayException.ExceptionSource {
      def line = Option(origin.lineNumber)
      def position = None
      def input = Option(origin.url).map(_.asInput)
      def sourceName = Option(origin.filename)
    }
  }

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
case class Configuration(underlying: Config) {

  /**
   * Merge 2 configurations.
   */
  def ++(other: Configuration): Configuration = {
    Configuration(other.underlying.withFallback(underlying))
  }
  
  private def readValue[T](path: String, v: => T): Option[T] = {
    try {
      Option(v)
    } catch {
      case e:ConfigException.Missing => None
      case e => throw reportError(path, e.getMessage, Some(e))
    }
  }

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
  def getString(path: String, validValues: Option[Set[String]] = None): Option[String] = readValue(path, underlying.getString(path)).map { value =>
    validValues match {
      case Some(values) if values.contains(value) => value
      case Some(values) if values.isEmpty => value
      case Some(values) => throw reportError(path, "Incorrect value, one of " + (values.reduceLeft(_ + ", " + _)) + " was expected.")
      case None => value
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
  def getInt(path: String): Option[Int] = readValue(path, underlying.getInt(path))

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
  def getBoolean(path: String): Option[Boolean] = readValue(path, underlying.getBoolean(path))
  
  def getMilliseconds(path: String): Option[Long] = readValue(path, underlying.getMilliseconds(path))
  
  def getBytes(path: String): Option[Long] = readValue(path, underlying.getBytes(path))

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
  def getConfig(path: String): Option[Configuration] = readValue(path, underlying.getConfig(path)).map(Configuration(_))

  /**
   * Returns available keys.
   *
   * @return the set of keys available in this configuration
   */
  def keys: Set[String] = underlying.entrySet.asScala.map(_.getKey).toSet

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
  def reportError(path: String, message: String, e: Option[Throwable] = None): PlayException = {
    Configuration.configError(if(underlying.hasPath(path)) underlying.getValue(path).origin else underlying.root.origin, message, e)
  }

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
  def globalError(message: String, e: Option[Throwable] = None): PlayException = {
    Configuration.configError(underlying.root.origin, message, e)
  }

}
