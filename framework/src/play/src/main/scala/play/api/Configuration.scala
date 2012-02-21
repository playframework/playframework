package play.api

import java.io._

import com.typesafe.config.{ Config, ConfigFactory, ConfigParseOptions, ConfigSyntax, ConfigOrigin, ConfigException }

import scala.collection.JavaConverters._

/**
 * This object provides a set of operations to create `Configuration` values.
 *
 * For example, to load a `Configuration` in a running application:
 * {{{
 * val config = Configuration.load()
 * val foo = config.getString("foo").getOrElse("boo")
 * }}}
 *
 * The underlying implementation is provided by https://github.com/typesafehub/config.
 */
object Configuration {

  /**
   * loads `Configuration` from 'conf/application.conf' in Dev mode
   * @return  configuration to be used
   */
  private[play] def loadDev(appPath: File) = {
    try {
      val file = Option(System.getProperty("config.file")).map(f => new File(f)).getOrElse(new File(appPath, "conf/application.conf"))
      ConfigFactory.load(ConfigFactory.parseFileAnySyntax(file))
    } catch {
      case e: ConfigException => throw configError(e.origin, e.getMessage, Some(e))
    }
  }

  /**
   * Loads a new `Configuration` either from the classpath or from
   * `conf/application.conf` depending on the application's Mode.
   *
   * The provieded mode is used if the application is not ready
   * yet, just like when calling this method from `play.api.Application`.
   *
   * Defaults to Mode.Dev
   *
   * @param mode Application mode.
   * @return a `Configuration` instance
   */
  def load(appPath: File, mode: Mode.Mode = Mode.Dev) = {
    try {
      val currentMode = Play.maybeApplication.map(_.mode).getOrElse(mode)
      if (currentMode == Mode.Prod) Configuration(ConfigFactory.load()) else Configuration(loadDev(appPath))
    } catch {
      case e: ConfigException => throw configError(e.origin, e.getMessage, Some(e))
      case e => throw e
    }
  }

  /**
   * Returns an empty Configuration object.
   */
  def empty = Configuration(ConfigFactory.empty())

  /**
   * Create a ConfigFactory object from the data passed as a Map.
   */
  def from(data: Map[String, String]) = {
    Configuration(ConfigFactory.parseMap(data.asJava))
  }

  private def configError(origin: ConfigOrigin, message: String, e: Option[Throwable] = None): PlayException = {
    import scalax.io.JavaConverters._
    new PlayException("Configuration error", message, e) with PlayException.ExceptionSource {
      def line = Option(origin.lineNumber)
      def position = None
      def input = Option(origin.url).map(_.asInput)
      def sourceName = Option(origin.filename)
      override def toString = "Configuration error: " + getMessage
    }
  }

}

/**
 * A full configuration set.
 *
 * The underlying implementation is provided by https://github.com/typesafehub/config.
 *
 * @param underlying the underlying Config implementation
 */
case class Configuration(underlying: Config) {

  /**
   * Merge 2 configurations.
   */
  def ++(other: Configuration): Configuration = {
    Configuration(other.underlying.withFallback(underlying))
  }

  /**
   * Read a value from the underlying implementation,
   * catching Errors and wrapping it in an Option value.
   */
  private def readValue[T](path: String, v: => T): Option[T] = {
    try {
      Option(v)
    } catch {
      case e: ConfigException.Missing => None
      case e => throw reportError(path, e.getMessage, Some(e))
    }
  }

  /**
   * Retrieves a configuration value as a `String`.
   *
   * This method supports an optional set of valid values:
   * {{{
   * val config = Configuration.load()
   * val mode = config.getString("engine.mode", Some(Set("dev","prod")))
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
   * val configuration = Configuration.load()
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
   * val configuration = Configuration.load()
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

  /**
   * Retrieves a configuration value as `Milliseconds`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val timeout = configuration.getString("engine.timeout")
   * }}}
   *
   * The configuratioon must be provided as:
   *
   * {{{
   * engine.timeout = 1 second
   * }}}
   */
  def getMilliseconds(path: String): Option[Long] = readValue(path, underlying.getMilliseconds(path))

  /**
   * Retrieves a configuration value as `Bytes`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val maxSize = configuration.getString("engine.maxSize")
   * }}}
   *
   * The configuratioon must be provided as:
   *
   * {{{
   * engine.maxSize = 512k
   * }}}
   */
  def getBytes(path: String): Option[Long] = readValue(path, underlying.getBytes(path))

  /**
   * Retrieves a sub-configuration, i.e. a configuration instance containing all keys starting with a given prefix.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
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
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val keys = configuration.keys
   * }}}
   *
   * @return the set of keys available in this configuration
   */
  def keys: Set[String] = underlying.entrySet.asScala.map(_.getKey).toSet

  /**
   * Returns sub-keys.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val subKeys = configuration.subKeys
   * }}}
   * @return the set of direct sub-keys available in this configuration
   */
  def subKeys: Set[String] = keys.map(_.split('.').head)

  /**
   * Creates a configuration error for a specific configuration key.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * throw configuration.reportError("engine.connectionUrl", "Cannot connect!")
   * }}}
   *
   * @param key the configuration key, related to this error
   * @param message the error message
   * @param e the related exception
   * @return a configuration exception
   */
  def reportError(path: String, message: String, e: Option[Throwable] = None): PlayException = {
    Configuration.configError(if (underlying.hasPath(path)) underlying.getValue(path).origin else underlying.root.origin, message, e)
  }

  /**
   * Creates a configuration error for this configuration.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
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
