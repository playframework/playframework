/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api

import java.io._
import java.util.concurrent.TimeUnit

import com.typesafe.config._

import scala.collection.JavaConverters._
import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.io.Source
import scala.util.Try
import scala.util.control.NonFatal
import play.utils.{ PlayIO, Threads }

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

  private[this] lazy val dontAllowMissingConfigOptions = ConfigParseOptions.defaults().setAllowMissing(false)

  private[this] lazy val dontAllowMissingConfig = ConfigFactory.load(dontAllowMissingConfigOptions)

  /**
   * loads `Configuration` from config.resource or config.file. If not found default to 'conf/application.conf' in Dev mode
   * @return configuration to be used
   */
  private[play] def loadDev(appPath: File, devSettings: Map[String, String]): Config = {
    try {
      lazy val file: Option[String] = {
        devSettings.get("config.file").orElse(Option(System.getProperty("config.file")))
      }
      val config: Config = file.map(f => ConfigFactory.parseFileAnySyntax(new File(f)))
        .getOrElse(ConfigFactory.parseResources(
          Option(System.getProperty("config.resource")).getOrElse("application.conf")))

      ConfigFactory.parseMap(devSettings.asJava).withFallback(ConfigFactory.load(config))
    } catch {
      case e: ConfigException => throw configError(e.origin, e.getMessage, Some(e))
    }
  }

  /**
   * Loads a new `Configuration` either from the classpath or from
   * `conf/application.conf` depending on the application's Mode.
   *
   * The provided mode is used if the application is not ready
   * yet, just like when calling this method from `play.api.Application`.
   *
   * Defaults to Mode.Dev
   *
   * @param mode Application mode.
   * @return a `Configuration` instance
   */
  def load(appPath: File, mode: Mode.Mode = Mode.Dev, devSettings: Map[String, String] = Map.empty): Configuration = {
    try {
      val currentMode = Play.maybeApplication.map(_.mode).getOrElse(mode)
      if (currentMode == Mode.Prod) Configuration(dontAllowMissingConfig) else Configuration(loadDev(appPath, devSettings))
    } catch {
      case e: ConfigException => throw configError(e.origin, e.getMessage, Some(e))
    }
  }

  /**
   * Load a new Configuration from the Environment.
   */
  def load(environment: Environment, devSettings: Map[String, String]): Configuration = {
    Threads.withContextClassLoader(environment.classLoader) {
      load(environment.rootPath, environment.mode, devSettings)
    }
  }

  /**
   * Load a new Configuration from the Environment.
   */
  def load(environment: Environment): Configuration = load(environment, Map.empty[String, String])

  /**
   * Returns an empty Configuration object.
   */
  def empty = Configuration(ConfigFactory.empty())

  /**
   * Returns the reference configuration object.
   */
  def reference = Configuration(ConfigFactory.defaultReference())

  /**
   * Create a new Configuration from the data passed as a Map.
   */
  def from(data: Map[String, Any]): Configuration = {

    def toJava(data: Any): Any = data match {
      case map: Map[_, _] => map.mapValues(toJava).asJava
      case iterable: Iterable[_] => iterable.map(toJava).asJava
      case v => v
    }

    Configuration(ConfigFactory.parseMap(toJava(data).asInstanceOf[java.util.Map[String, AnyRef]]))
  }

  /**
   * Create a new Configuration from the given key-value pairs.
   */
  def apply(data: (String, Any)*): Configuration = from(data.toMap)

  private[api] def configError(origin: ConfigOrigin, message: String, e: Option[Throwable] = None): PlayException = {
    /*
      The stable values here help us from putting a reference to a ConfigOrigin inside the anonymous ExceptionSource.
      This is necessary to keep the Exception serialisable, because ConfigOrigin is not serialisable.
     */
    val originLine = Option(origin.lineNumber: java.lang.Integer).orNull
    val originUrl = Option(origin.url)
    val originSourceName = Option(origin.filename).orNull
    new PlayException.ExceptionSource("Configuration error", message, e.orNull) {
      def line = originLine
      def position = null
      def input = originUrl.map(PlayIO.readUrlAsString).orNull
      def sourceName = originSourceName
      override def toString = "Configuration error: " + getMessage
    }
  }

  private[Configuration] def asScalaList[A](l: java.util.List[A]): Seq[A] = asScalaBufferConverter(l).asScala.toList
}

/**
 * A full configuration set.
 *
 * The underlying implementation is provided by https://github.com/typesafehub/config.
 *
 * @param underlying the underlying Config implementation
 */
case class Configuration(underlying: Config) {
  import Configuration.asScalaList

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
      case NonFatal(e) => throw reportError(path, e.getMessage, Some(e))
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
   * @param path the configuration key, relative to configuration root key
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
   * @param path the configuration key, relative to the configuration root key
   * @return a configuration value
   */
  def getInt(path: String): Option[Int] = readValue(path, underlying.getInt(path))

  /**
   * Retrieves a configuration value as a `Boolean`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val isEnabled = configuration.getBoolean("engine.isEnabled")
   * }}}
   *
   * A configuration error will be thrown if the configuration value is not a valid `Boolean`.
   * Authorized vales are `yes/no or true/false.
   *
   * @param path the configuration key, relative to the configuration root key
   * @return a configuration value
   */
  def getBoolean(path: String): Option[Boolean] = readValue(path, underlying.getBoolean(path))

  /**
   * Retrieves a configuration value as `Milliseconds`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val timeout = configuration.getMilliseconds("engine.timeout")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * engine.timeout = 1 second
   * }}}
   */
  def getMilliseconds(path: String): Option[Long] = readValue(path, underlying.getDuration(path, TimeUnit.MILLISECONDS))

  /**
   * Retrieves a configuration value as `Nanoseconds`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val timeout = configuration.getNanoseconds("engine.timeout")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * engine.timeout = 1 second
   * }}}
   */
  def getNanoseconds(path: String): Option[Long] = readValue(path, underlying.getDuration(path, TimeUnit.NANOSECONDS))

  /**
   * Retrieves a configuration value as `Bytes`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val maxSize = configuration.getString("engine.maxSize")
   * }}}
   *
   * The configuration must be provided as:
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
   * val engineConfig = configuration.getConfig("engine")
   * }}}
   *
   * The root key of this new configuration will be ‘engine’, and you can access any sub-keys relatively.
   *
   * @param path the root prefix for this sub-configuration
   * @return a new configuration
   */
  def getConfig(path: String): Option[Configuration] = readValue(path, underlying.getConfig(path)).map(Configuration(_))

  /**
   * Retrieves a configuration value as a `Double`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val population = configuration.getDouble("world.population")
   * }}}
   *
   * A configuration error will be thrown if the configuration value is not a valid `Double`.
   *
   * @param path the configuration key, relative to the configuration root key
   * @return a configuration value
   */
  def getDouble(path: String): Option[Double] = readValue(path, underlying.getDouble(path))

  /**
   * Retrieves a configuration value as a `Long`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val duration = configuration.getLong("timeout.duration")
   * }}}
   *
   * A configuration error will be thrown if the configuration value is not a valid `Long`.
   *
   * @param path the configuration key, relative to the configuration root key
   * @return a configuration value
   */
  def getLong(path: String): Option[Long] = readValue(path, underlying.getLong(path))

  /**
   * Retrieves a configuration value as a `Number`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val counter = configuration.getNumber("foo.counter")
   * }}}
   *
   * A configuration error will be thrown if the configuration value is not a valid `Number`.
   *
   * @param path the configuration key, relative to the configuration root key
   * @return a configuration value
   */
  def getNumber(path: String): Option[Number] = readValue(path, underlying.getNumber(path))

  /**
   * Retrieves a configuration value as a List of `Boolean`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val switches = configuration.getBooleanList("board.switches")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * board.switches = [true, true, false]
   * }}}
   *
   * A configuration error will be thrown if the configuration value is not a valid `Boolean`.
   * Authorized vales are `yes/no or true/false.
   */
  def getBooleanList(path: String): Option[java.util.List[java.lang.Boolean]] = readValue(path, underlying.getBooleanList(path))

  /**
   * Retrieves a configuration value as a Seq of `Boolean`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val switches = configuration.getBooleanSeq("board.switches")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * board.switches = [true, true, false]
   * }}}
   *
   * A configuration error will be thrown if the configuration value is not a valid `Boolean`.
   * Authorized vales are `yes/no or true/false.
   */
  def getBooleanSeq(path: String): Option[Seq[java.lang.Boolean]] = getBooleanList(path).map(asScalaList)

  /**
   * Retrieves a configuration value as a List of `Bytes`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val maxSizes = configuration.getBytesList("engine.maxSizes")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * engine.maxSizes = [512k, 256k, 256k]
   * }}}
   */
  def getBytesList(path: String): Option[java.util.List[java.lang.Long]] = readValue(path, underlying.getBytesList(path))

  /**
   * Retrieves a configuration value as a Seq of `Bytes`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val maxSizes = configuration.getBytesSeq("engine.maxSizes")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * engine.maxSizes = [512k, 256k, 256k]
   * }}}
   */
  def getBytesSeq(path: String): Option[Seq[java.lang.Long]] = getBytesList(path).map(asScalaList)

  /**
   * Retrieves a List of sub-configurations, i.e. a configuration instance for each key that matches the path.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val engineConfigs = configuration.getConfigList("engine")
   * }}}
   *
   * The root key of this new configuration will be "engine", and you can access any sub-keys relatively.
   */
  def getConfigList(path: String): Option[java.util.List[Configuration]] = readValue[java.util.List[_ <: Config]](path, underlying.getConfigList(path)).map { configs => configs.asScala.map(Configuration(_)).asJava }

  /**
   * Retrieves a Seq of sub-configurations, i.e. a configuration instance for each key that matches the path.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val engineConfigs = configuration.getConfigSeq("engine")
   * }}}
   *
   * The root key of this new configuration will be "engine", and you can access any sub-keys relatively.
   */
  def getConfigSeq(path: String): Option[Seq[Configuration]] = getConfigList(path).map(asScalaList)

  /**
   * Retrieves a configuration value as a List of `Double`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val maxSizes = configuration.getDoubleList("engine.maxSizes")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * engine.maxSizes = [5.0, 3.34, 2.6]
   * }}}
   */
  def getDoubleList(path: String): Option[java.util.List[java.lang.Double]] = readValue(path, underlying.getDoubleList(path))

  /**
   * Retrieves a configuration value as a Seq of `Double`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val maxSizes = configuration.getDoubleSeq("engine.maxSizes")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * engine.maxSizes = [5.0, 3.34, 2.6]
   * }}}
   */
  def getDoubleSeq(path: String): Option[Seq[java.lang.Double]] = getDoubleList(path).map(asScalaList)

  /**
   * Retrieves a configuration value as a List of `Integer`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val maxSizes = configuration.getIntList("engine.maxSizes")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * engine.maxSizes = [100, 500, 2]
   * }}}
   */
  def getIntList(path: String): Option[java.util.List[java.lang.Integer]] = readValue(path, underlying.getIntList(path))

  /**
   * Retrieves a configuration value as a Seq of `Integer`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val maxSizes = configuration.getIntSeq("engine.maxSizes")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * engine.maxSizes = [100, 500, 2]
   * }}}
   */
  def getIntSeq(path: String): Option[Seq[java.lang.Integer]] = getIntList(path).map(asScalaList)

  /**
   * Gets a list value (with any element type) as a ConfigList, which implements java.util.List<ConfigValue>.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val maxSizes = configuration.getList("engine.maxSizes")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * engine.maxSizes = ["foo", "bar"]
   * }}}
   */
  def getList(path: String): Option[ConfigList] = readValue(path, underlying.getList(path))

  /**
   * Retrieves a configuration value as a List of `Long`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val maxSizes = configuration.getLongList("engine.maxSizes")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * engine.maxSizes = [10000000000000, 500, 2000]
   * }}}
   */
  def getLongList(path: String): Option[java.util.List[java.lang.Long]] = readValue(path, underlying.getLongList(path))

  /**
   * Retrieves a configuration value as a Seq of `Long`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val maxSizes = configuration.getLongSeq("engine.maxSizes")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * engine.maxSizes = [10000000000000, 500, 2000]
   * }}}
   */
  def getLongSeq(path: String): Option[Seq[java.lang.Long]] = getLongList(path).map(asScalaList)

  /**
   * Retrieves a configuration value as List of `Milliseconds`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val timeouts = configuration.getMillisecondsList("engine.timeouts")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * engine.timeouts = [1 second, 1 second]
   * }}}
   */
  def getMillisecondsList(path: String): Option[java.util.List[java.lang.Long]] = readValue(path, underlying.getDurationList(path, TimeUnit.MILLISECONDS))

  /**
   * Retrieves a configuration value as Seq of `Milliseconds`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val timeouts = configuration.getMillisecondsSeq("engine.timeouts")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * engine.timeouts = [1 second, 1 second]
   * }}}
   */
  def getMillisecondsSeq(path: String): Option[Seq[java.lang.Long]] = getMillisecondsList(path).map(asScalaList)

  /**
   * Retrieves a configuration value as List of `Nanoseconds`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val timeouts = configuration.getNanosecondsList("engine.timeouts")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * engine.timeouts = [1 second, 1 second]
   * }}}
   */
  def getNanosecondsList(path: String): Option[java.util.List[java.lang.Long]] = readValue(path, underlying.getDurationList(path, TimeUnit.NANOSECONDS))

  /**
   * Retrieves a configuration value as Seq of `Nanoseconds`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val timeouts = configuration.getNanosecondsSeq("engine.timeouts")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * engine.timeouts = [1 second, 1 second]
   * }}}
   */
  def getNanosecondsSeq(path: String): Option[Seq[java.lang.Long]] = getNanosecondsList(path).map(asScalaList)

  /**
   * Retrieves a configuration value as a List of `Number`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val maxSizes = configuration.getNumberList("engine.maxSizes")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * engine.maxSizes = [50, 500, 5000]
   * }}}
   */
  def getNumberList(path: String): Option[java.util.List[java.lang.Number]] = readValue(path, underlying.getNumberList(path))

  /**
   * Retrieves a configuration value as a Seq of `Number`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val maxSizes = configuration.getNumberSeq("engine.maxSizes")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * engine.maxSizes = [50, 500, 5000]
   * }}}
   */
  def getNumberSeq(path: String): Option[Seq[java.lang.Number]] = getNumberList(path).map(asScalaList)

  /**
   * Retrieves a configuration value as a List of `ConfigObject`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val engineProperties = configuration.getObjectList("engine.properties")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * engine.properties = [{id: 5, power: 3}, {id: 6, power: 20}]
   * }}}
   */
  def getObjectList(path: String): Option[java.util.List[_ <: ConfigObject]] = readValue[java.util.List[_ <: ConfigObject]](path, underlying.getObjectList(path))

  /**
   * Retrieves a configuration value as a List of `String`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val names = configuration.getStringList("names")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * names = ["Jim", "Bob", "Steve"]
   * }}}
   */
  def getStringList(path: String): Option[java.util.List[java.lang.String]] = readValue(path, underlying.getStringList(path))

  /**
   * Retrieves a configuration value as a Seq of `String`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val names = configuration.getStringSeq("names")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * names = ["Jim", "Bob", "Steve"]
   * }}}
   */
  def getStringSeq(path: String): Option[Seq[java.lang.String]] = getStringList(path).map(asScalaList)

  /**
   * Retrieves a ConfigObject for this path, which implements Map<String,ConfigValue>
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val engineProperties = configuration.getObject("engine.properties")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * engine.properties = {id: 1, power: 5}
   * }}}
   */
  def getObject(path: String): Option[ConfigObject] = readValue(path, underlying.getObject(path))

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
  def subKeys: Set[String] = underlying.root().keySet().asScala.toSet

  /**
   * Returns every path as a set of key to value pairs, by recursively iterating through the
   * config objects.
   */
  def entrySet: Set[(String, ConfigValue)] = underlying.entrySet().asScala.map(e => e.getKey -> e.getValue).toSet

  /**
   * Creates a configuration error for a specific configuration key.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * throw configuration.reportError("engine.connectionUrl", "Cannot connect!")
   * }}}
   *
   * @param path the configuration key, related to this error
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

  /**
   * Loads a String configuration item, looking at the deprecated key first, and outputting a warning if it's defined,
   * otherwise loading the new key.
   */
  private[play] def getDeprecatedString(key: String, deprecatedKey: String): String = {
    getString(deprecatedKey).fold(underlying.getString(key)) { value =>
      Logger.warn(s"$deprecatedKey is deprecated, use $key instead")
      value
    }
  }

  /**
   * Loads a String configuration item, looking at the deprecated key first, and outputting a warning if it's defined,
   * otherwise loading the new key.
   */
  private[play] def getDeprecatedStringOpt(key: String, deprecatedKey: String): Option[String] = {
    getString(deprecatedKey).map { value =>
      Logger.warn(s"$deprecatedKey is deprecated, use $key instead")
      value
    }.orElse(getString(key)).filter(_.nonEmpty)
  }

  /**
   * Loads a Boolean configuration item, looking at the deprecated key first, and outputting a warning if it's defined,
   * otherwise loading the new key.
   */
  private[play] def getDeprecatedBoolean(key: String, deprecatedKey: String): Boolean = {
    getBoolean(deprecatedKey).fold(underlying.getBoolean(key)) { value =>
      Logger.warn(s"$deprecatedKey is deprecated, use $key instead")
      value
    }
  }

  /**
   * Loads a Duration configuration item, looking at the deprecated key first, and outputting a warning if it's defined,
   * otherwise loading the new key.
   */
  private[play] def getDeprecatedDuration(key: String, deprecatedKey: String): FiniteDuration = {
    new FiniteDuration(getNanoseconds(deprecatedKey).fold(underlying.getDuration(key, TimeUnit.NANOSECONDS)) { value =>
      Logger.warn(s"$deprecatedKey is deprecated, use $key instead")
      value
    }, TimeUnit.NANOSECONDS)
  }

  /**
   * Loads an optional Duration configuration item, looking at the deprecated key first, and outputting a warning if
   * it's defined, otherwise loading the new key.
   */
  private[play] def getDeprecatedDurationOpt(key: String, deprecatedKey: String): Option[FiniteDuration] = {
    getNanoseconds(deprecatedKey).map { value =>
      Logger.warn(s"$deprecatedKey is deprecated, use $key instead")
      value
    }.orElse(getNanoseconds(key)).map { value =>
      new FiniteDuration(value, TimeUnit.NANOSECONDS)
    }
  }

}

/**
 * A Play configuration wrapper.
 *
 * Eventually, maybe this will replace Configuration.
 *
 * The story behind this:
 *
 * In the early days, Play's Configuration object conveniently wrapped Typesafe config, returning an options, and
 * converting Seq types.
 *
 * The problem with returning Options is that that's not idiomatic Typesafe config usage - configuration should not
 * be optional, defaults should be specified in reference.conf.  Another problem is that if you want to add new
 * functionality, you have to do so for every permutation of getType method.
 *
 * So, this new implementation does not return options, and uses type classes to handle the permutation issue.
 *
 * It also provides a number of additional features, including:
 *
 * - Prototyped config objects
 * - Optional values signified by null in reference.conf
 * - Deprecated config that outputs a warning if defined
 * - Deprecated objects, merging values with the new value
 *
 * @param underlying The underlying Typesafe config object
 */
private[play] class PlayConfig(val underlying: Config) {

  /**
   * Get the config at the given path.
   */
  def get[A](path: String)(implicit loader: ConfigLoader[A]): A = {
    loader.load(underlying, path)
  }

  /**
   * Get an optional configuration item.
   *
   * If the value of the item is null, this will return None, otherwise returns Some.
   *
   * @throws com.typesafe.config.ConfigException.Missing If the value is undefined (as opposed to null) this will still
   *         throw an exception.
   */
  def getOptional[A: ConfigLoader](path: String): Option[A] = {
    try {
      // If the value is null, typesafe config will throw an exception. This is the only way that typesafe config allows
      // discovering null values
      Some(get[A](path))
    } catch {
      case e: ConfigException.Null => None
    }
  }

  /**
   * Get a prototyped sequence of objects.
   *
   * Each object in the sequence will fallback to the object loaded from prototype.$path.
   */
  def getPrototypedSeq(path: String, prototypePath: String = "prototype.$path"): Seq[PlayConfig] = {
    val prototype = underlying.getConfig(prototypePath.replace("$path", path))
    get[Seq[Config]](path).map { config =>
      new PlayConfig(config.withFallback(prototype))
    }
  }

  /**
   * Get a prototyped map of objects.
   *
   * Each value in the map will fallback to the object loaded from prototype.$path.
   */
  def getPrototypedMap(path: String, prototypePath: String = "prototype.$path"): Map[String, PlayConfig] = {
    val prototype = underlying.getConfig(prototypePath.replace("$path", path))
    get[Map[String, Config]](path).map {
      case (key, config) => key -> new PlayConfig(config.withFallback(prototype))
    }.toMap
  }

  /**
   * Get an optional deprecated configuration item.
   *
   * If the deprecated configuration item is defined, it will be returned, and a warning will be logged.
   *
   * Otherwise, the configuration from path will be looked up.
   *
   * If the value of the item is null, this will return None, otherwise returns Some.
   */
  def getOptionalDeprecated[A: ConfigLoader](path: String, deprecated: String): Option[A] = {
    if (underlying.hasPath(deprecated)) {
      reportDeprecation(path, deprecated)
      getOptional[A](deprecated)
    } else {
      getOptional[A](path)
    }
  }

  /**
   * Get a deprecated configuration item.
   *
   * If the deprecated configuration item is defined, it will be returned, and a warning will be logged.
   *
   * Otherwise, the configuration from path will be looked up.
   */
  def getDeprecated[A: ConfigLoader](path: String, deprecated: String): A = {
    if (underlying.hasPath(deprecated)) {
      reportDeprecation(path, deprecated)
      get[A](deprecated)
    } else {
      get[A](path)
    }
  }

  /**
   * Get a deprecated configuration.
   *
   * If the deprecated configuration is defined, it will be returned, falling back to the new configuration, and a
   * warning will be logged.
   *
   * Otherwise, the configuration from path will be looked up and used as is.
   */
  def getDeprecatedWithFallback(path: String, deprecated: String, parent: String = ""): PlayConfig = {
    val config = get[Config](path)
    val merged = if (underlying.hasPath(deprecated)) {
      reportDeprecation(path, deprecated)
      get[Config](deprecated).withFallback(config)
    } else config
    new PlayConfig(merged)
  }

  /**
   * Creates a configuration error for a specific configuration key.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * throw configuration.reportError("engine.connectionUrl", "Cannot connect!")
   * }}}
   *
   * @param path the configuration key, related to this error
   * @param message the error message
   * @param e the related exception
   * @return a configuration exception
   */
  def reportError(path: String, message: String, e: Option[Throwable] = None): PlayException = {
    Configuration.configError(if (underlying.hasPath(path)) underlying.getValue(path).origin else underlying.root.origin, message, e)
  }

  private def reportDeprecation(path: String, deprecated: String): Unit = {
    val origin = underlying.getValue(deprecated).origin
    Logger.warn(s"${origin.description}: $deprecated is deprecated, use $path instead:")
    Try {
      if (origin.url != null && origin.lineNumber() > 0) {
        val is = origin.url.openStream()
        try {
          Source.fromInputStream(is).getLines()
            .drop(origin.lineNumber() - 1)
            .toStream.headOption
            .map { line =>
              Logger.warn(line)
            }
        } finally {
          is.close()
        }
      }

    }

  }
}

private[play] object PlayConfig {
  def apply(underlying: Config) = new PlayConfig(underlying)
  def apply(configuration: Configuration) = new PlayConfig(configuration.underlying)
}

/**
 * A config loader
 */
private[play] trait ConfigLoader[A] { self =>
  def load(config: Config, path: String): A
  def map[B](f: A => B): ConfigLoader[B] = new ConfigLoader[B] {
    def load(config: Config, path: String): B = {
      f(self.load(config, path))
    }
  }
}

private[play] object ConfigLoader {

  def apply[A](f: Config => String => A): ConfigLoader[A] = new ConfigLoader[A] {
    def load(config: Config, path: String): A = f(config)(path)
  }

  import scala.collection.JavaConverters._

  private def toScala[A](as: java.util.List[A]): Seq[A] = as.asScala

  implicit val stringLoader = ConfigLoader(_.getString)
  implicit val seqStringLoader = ConfigLoader(_.getStringList).map(toScala)

  implicit val intLoader = ConfigLoader(_.getInt)
  implicit val seqIntLoader = ConfigLoader(_.getIntList).map(toScala(_).map(_.toInt))

  implicit val booleanLoader = ConfigLoader(_.getBoolean)
  implicit val seqBooleanLoader = ConfigLoader(_.getBooleanList).map(toScala(_).map(_.booleanValue()))

  implicit val durationLoader: ConfigLoader[Duration] = ConfigLoader(config => path =>
    try {
      FiniteDuration(config.getDuration(path, TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
    } catch {
      case e: ConfigException.Null => Duration.Inf
    }
  )

  implicit val finiteDurationLoader: ConfigLoader[FiniteDuration] = ConfigLoader(config => config.getDuration(_, TimeUnit.MILLISECONDS))
    .map(millis => FiniteDuration(millis, TimeUnit.MILLISECONDS))
  implicit val seqFiniteDurationLoader: ConfigLoader[Seq[FiniteDuration]] = ConfigLoader(config => config.getDurationList(_, TimeUnit.MILLISECONDS))
    .map(toScala(_).map(millis => FiniteDuration(millis, TimeUnit.MILLISECONDS)))

  implicit val doubleLoader = ConfigLoader(_.getDouble)
  implicit val seqDoubleLoader = ConfigLoader(_.getDoubleList).map(toScala)

  implicit val longLoader = ConfigLoader(_.getLong)
  implicit val seqLongLoader = ConfigLoader(_.getLongList).map(toScala)

  implicit val bytesLoader = ConfigLoader(_.getMemorySize)
  implicit val seqBytesLoader = ConfigLoader(_.getMemorySizeList).map(toScala)

  implicit val configLoader: ConfigLoader[Config] = ConfigLoader(_.getConfig)
  implicit val seqConfigLoader: ConfigLoader[Seq[Config]] = ConfigLoader(_.getConfigList).map(_.asScala)

  implicit val playConfigLoader = configLoader.map(new PlayConfig(_))
  implicit val seqPlayConfigLoader = seqConfigLoader.map(_.map(new PlayConfig(_)))

  implicit def mapLoader[A](implicit valueLoader: ConfigLoader[A]): ConfigLoader[Map[String, A]] = new ConfigLoader[Map[String, A]] {
    def load(config: Config, path: String): Map[String, A] = {
      val obj = config.getObject(path)
      val conf = obj.toConfig
      obj.keySet().asScala.map { key =>
        key -> valueLoader.load(conf, key)
      }.toMap
    }
  }
}
