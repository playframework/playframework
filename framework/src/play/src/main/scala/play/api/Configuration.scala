/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api

import java.io._
import java.util.Properties
import java.util.concurrent.TimeUnit

import com.typesafe.config._
import com.typesafe.config.impl.ConfigImpl
import play.utils.PlayIO

import scala.collection.JavaConverters._
import scala.concurrent.duration.{ Duration, FiniteDuration, _ }
import scala.util.control.NonFatal

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

  private[play] def load(
    classLoader: ClassLoader,
    properties: Properties,
    directSettings: Map[String, AnyRef],
    allowMissingApplicationConf: Boolean): Configuration = {

    try {
      // Get configuration from the system properties.
      // Iterating through the system properties is prone to ConcurrentModificationExceptions (especially in our tests)
      // Typesafe config maintains a cache for this purpose.  So, if the passed in properties *are* the system
      // properties, use the Typesafe config cache, otherwise it should be safe to parse it ourselves.
      val systemPropertyConfig = if (properties eq System.getProperties) {
        ConfigImpl.systemPropertiesAsConfig()
      } else {
        ConfigFactory.parseProperties(properties)
      }

      // Inject our direct settings into the config.
      val directConfig: Config = ConfigFactory.parseMap(directSettings.asJava)

      // Resolve application.conf ourselves because:
      // - we may want to load configuration when application.conf is missing.
      // - We also want to delay binding and resolving reference.conf, which
      //   is usually part of the default application.conf loading behavior.
      // - We want to read config.file and config.resource settings from our
      //   own properties and directConfig rather than system properties.
      val applicationConfig: Config = {
        def setting(key: String): Option[AnyRef] =
          directSettings.get(key).orElse(Option(properties.getProperty(key)))

        {
          setting("config.resource").map(resource => ConfigFactory.parseResources(classLoader, resource.toString))
        } orElse {
          setting("config.file").map(fileName => ConfigFactory.parseFileAnySyntax(new File(fileName.toString)))
        } getOrElse {
          val parseOptions = ConfigParseOptions.defaults
            .setClassLoader(classLoader)
            .setAllowMissing(allowMissingApplicationConf)
          ConfigFactory.defaultApplication(parseOptions)
        }
      }

      // Resolve another .conf file so that we can override values in Akka's
      // reference.conf, but still make it possible for users to override
      // Play's values in their application.conf.
      val playOverridesConfig: Config = ConfigFactory.parseResources(classLoader, "play/reference-overrides.conf")

      // Resolve reference.conf ourselves because ConfigFactory.defaultReference resolves
      // values, and we won't have a value for `play.server.dir` until all our config is combined.
      val referenceConfig: Config = ConfigFactory.parseResources(classLoader, "reference.conf")

      // Combine all the config together into one big config
      val combinedConfig: Config = Seq(
        systemPropertyConfig,
        directConfig,
        applicationConfig,
        playOverridesConfig,
        referenceConfig
      ).reduceLeft(_ withFallback _)

      // Resolve settings. Among other things, the `play.server.dir` setting defined in directConfig will
      // be substituted into the default settings in referenceConfig.
      val resolvedConfig = combinedConfig.resolve

      Configuration(resolvedConfig)
    } catch {
      case e: ConfigException => throw configError(e.getMessage, Option(e.origin), Some(e))
    }
  }

  /**
   * Load a new Configuration from the Environment.
   */
  def load(environment: Environment, devSettings: Map[String, AnyRef]): Configuration = {
    load(environment.classLoader, System.getProperties, devSettings, allowMissingApplicationConf = environment.mode == Mode.Test)
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

  private[api] def configError(
    message: String, origin: Option[ConfigOrigin] = None, e: Option[Throwable] = None): PlayException = {
    /*
      The stable values here help us from putting a reference to a ConfigOrigin inside the anonymous ExceptionSource.
      This is necessary to keep the Exception serializable, because ConfigOrigin is not serializable.
     */
    val originLine = origin.map(_.lineNumber: java.lang.Integer).orNull
    val originSourceName = origin.map(_.filename).orNull
    val originUrlOpt = origin.flatMap(o => Option(o.url))
    new PlayException.ExceptionSource("Configuration error", message, e.orNull) {
      def line = originLine
      def position = null
      def input = originUrlOpt.map(PlayIO.readUrlAsString).orNull
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

  private[play] def reportDeprecation(path: String, deprecated: String): Unit = {
    val origin = underlying.getValue(deprecated).origin
    Logger.warn(s"${origin.description}: $deprecated is deprecated, use $path instead")
  }

  /**
   * Merge two configurations. The second configuration overrides the first configuration.
   * This is the opposite direction of `Config`'s `withFallback` method.
   */
  def ++(other: Configuration): Configuration = {
    Configuration(other.underlying.withFallback(underlying))
  }

  /**
   * Reads a value from the underlying implementation.
   * If the value is not set this will return None, otherwise returns Some.
   *
   * Does not check neither for incorrect type nor null value, but catches and wraps the error.
   */
  private def readValue[T](path: String, v: => T): Option[T] = {
    try {
      if (underlying.hasPathOrNull(path)) Some(v) else None
    } catch {
      case NonFatal(e) => throw reportError(path, e.getMessage, Some(e))
    }

  }

  /**
   * Check if the given path exists.
   */
  def has(path: String): Boolean = underlying.hasPath(path)

  /**
   * Get the config at the given path.
   */
  def get[A](path: String)(implicit loader: ConfigLoader[A]): A = {
    loader.load(underlying, path)
  }

  /**
   * Get the config at the given path and validate against a set of valid values.
   */
  def getAndValidate[A](path: String, values: Set[A])(implicit loader: ConfigLoader[A]): A = {
    val value = get(path)
    if (!values(value)) {
      throw reportError(path, s"Incorrect value, one of (${values.mkString(", ")}) was expected.")
    }
    value
  }

  /**
   * Get a value that may either not exist or be null. Note that this is not generally considered idiomatic Config
   * usage. Instead you should define all config keys in a reference.conf file.
   */
  def getOptional[A](path: String)(implicit loader: ConfigLoader[A]): Option[A] = {
    readValue(path, get[A](path))
  }

  /**
   * Get a prototyped sequence of objects.
   *
   * Each object in the sequence will fallback to the object loaded from prototype.\$path.
   */
  def getPrototypedSeq(path: String, prototypePath: String = "prototype.$path"): Seq[Configuration] = {
    val prototype = underlying.getConfig(prototypePath.replace("$path", path))
    get[Seq[Config]](path).map { config =>
      Configuration(config.withFallback(prototype))
    }
  }

  /**
   * Get a prototyped map of objects.
   *
   * Each value in the map will fallback to the object loaded from prototype.\$path.
   */
  def getPrototypedMap(path: String, prototypePath: String = "prototype.$path"): Map[String, Configuration] = {
    val prototype = if (prototypePath.isEmpty) {
      underlying
    } else {
      underlying.getConfig(prototypePath.replace("$path", path))
    }
    get[Map[String, Config]](path).map {
      case (key, config) => key -> Configuration(config.withFallback(prototype))
    }
  }

  /**
   * Get a deprecated configuration item.
   *
   * If the deprecated configuration item is defined, it will be returned, and a warning will be logged.
   *
   * Otherwise, the configuration from path will be looked up.
   */
  def getDeprecated[A: ConfigLoader](path: String, deprecatedPaths: String*): A = {
    deprecatedPaths.collectFirst {
      case deprecated if underlying.hasPath(deprecated) =>
        reportDeprecation(path, deprecated)
        get[A](deprecated)
    }.getOrElse {
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
  def getDeprecatedWithFallback(path: String, deprecated: String, parent: String = ""): Configuration = {
    val config = get[Config](path)
    val merged = if (underlying.hasPath(deprecated)) {
      reportDeprecation(path, deprecated)
      get[Config](deprecated).withFallback(config)
    } else config
    Configuration(merged)
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
  @deprecated("Use get[String] or getAndValidate[String] with reference config entry", "2.6.0")
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
  @deprecated("Use get[Int] with reference config entry", "2.6.0")
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
   * Authorized values are `yes`/`no` or `true`/`false`.
   *
   * @param path the configuration key, relative to the configuration root key
   * @return a configuration value
   */
  @deprecated("Use get[Boolean] with reference config entry", "2.6.0")
  def getBoolean(path: String): Option[Boolean] = getOptional[Boolean](path)

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
  @deprecated("Use getMillis with reference config entry", "2.6.0")
  def getMilliseconds(path: String): Option[Long] = getOptional[Duration](path).map(_.toMillis)

  /**
   * Retrieves a configuration value as `Milliseconds`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val timeout = configuration.getMillis("engine.timeout")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * engine.timeout = 1 second
   * }}}
   */
  def getMillis(path: String): Long = get[Duration](path).toMillis

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
  @deprecated("Use getNanos with reference config entry", "2.6.0")
  def getNanoseconds(path: String): Option[Long] = getOptional[Duration](path).map(_.toNanos)

  /**
   * Retrieves a configuration value as `Milliseconds`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val timeout = configuration.getNanos("engine.timeout")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * engine.timeout = 1 second
   * }}}
   */
  def getNanos(path: String): Long = get[Duration](path).toNanos

  /**
   * Retrieves a configuration value as `Bytes`.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * val maxSize = configuration.getBytes("engine.maxSize")
   * }}}
   *
   * The configuration must be provided as:
   *
   * {{{
   * engine.maxSize = 512k
   * }}}
   */
  @deprecated("Use underlying.getBytes with reference config entry", "2.6.0")
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
  @deprecated("Use get[Configuration] with reference config entry", "2.6.0")
  def getConfig(path: String): Option[Configuration] = getOptional[Configuration](path)

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
  @deprecated("Use get[Double] with reference config entry", "2.6.0")
  def getDouble(path: String): Option[Double] = getOptional[Double](path)

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
  @deprecated("Use get[Long] with reference config entry", "2.6.0")
  def getLong(path: String): Option[Long] = getOptional[Long](path)

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
  @deprecated("Use get[Number] with reference config entry", "2.6.0")
  def getNumber(path: String): Option[Number] = getOptional[Number](path)

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
   * Authorized values are `yes`/`no` or `true`/`false`.
   */
  @deprecated("Use underlying.getBooleanList with reference config entry", "2.6.0")
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
   * Authorized values are `yes`/`no` or `true`/`false`.
   */
  @deprecated("Use get[Seq[Boolean]] with reference config entry", "2.6.0")
  def getBooleanSeq(path: String): Option[Seq[java.lang.Boolean]] = getOptional[Seq[Boolean]](path).map(_.map(new java.lang.Boolean(_)))

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
  @deprecated("Use underlying.getBytesList with reference config entry", "2.6.0")
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
  @deprecated("Use underlying.getBytesList with reference config entry", "2.6.0")
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
  @deprecated("Use underlying.getConfigList with reference config entry", "2.6.0")
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
  @deprecated("Use underlying.getConfigList with reference config entry", "2.6.0")
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
  @deprecated("Use underlying.getDoubleList with reference config entry", "2.6.0")
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
  @deprecated("Use get[Seq[Double]] with reference config entry", "2.6.0")
  def getDoubleSeq(path: String): Option[Seq[java.lang.Double]] = getOptional[Seq[Double]](path).map(_.map(new java.lang.Double(_)))

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
  @deprecated("Use underlying.getIntList with reference config entry", "2.6.0")
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
  @deprecated("Use get[Seq[Int]] with reference config entry", "2.6.0")
  def getIntSeq(path: String): Option[Seq[java.lang.Integer]] = getOptional[Seq[Int]](path).map(_.map(new java.lang.Integer(_)))

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
  @deprecated("Use get[ConfigList] with reference config entry", "2.6.0")
  def getList(path: String): Option[ConfigList] = getOptional[ConfigList](path)

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
  @deprecated("Use underlying.getLongList with reference config entry", "2.6.0")
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
  @deprecated("Use get[Seq[Long]] with reference config entry", "2.6.0")
  def getLongSeq(path: String): Option[Seq[java.lang.Long]] =
    getOptional[Seq[Long]](path).map(_.map(new java.lang.Long(_)))

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
  @deprecated("Use underlying.getMillisecondsList with reference config entry", "2.6.0")
  def getMillisecondsList(path: String): Option[java.util.List[java.lang.Long]] =
    readValue(path, underlying.getDurationList(path, TimeUnit.MILLISECONDS))

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
  @deprecated("Use get[Seq[Duration]].map(_.toMillis) with reference config entry", "2.6.0")
  def getMillisecondsSeq(path: String): Option[Seq[java.lang.Long]] =
    getOptional[Seq[Duration]](path).map(_.map(duration => new java.lang.Long(duration.toMillis)))

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
  @deprecated("Use underlying.getNanosecondsList with reference config entry", "2.6.0")
  def getNanosecondsList(path: String): Option[java.util.List[java.lang.Long]] =
    readValue(path, underlying.getDurationList(path, TimeUnit.NANOSECONDS))

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
  @deprecated("Use get[Seq[Duration]].map(_.toMillis) with reference config entry", "2.6.0")
  def getNanosecondsSeq(path: String): Option[Seq[java.lang.Long]] =
    getOptional[Seq[Duration]](path).map(_.map(duration => new java.lang.Long(duration.toNanos)))

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
  @deprecated("Use underlying.getNumberList with reference config entry", "2.6.0")
  def getNumberList(path: String): Option[java.util.List[java.lang.Number]] =
    readValue(path, underlying.getNumberList(path))

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
  @deprecated("Use get[Seq[Number]] with reference config entry", "2.6.0")
  def getNumberSeq(path: String): Option[Seq[java.lang.Number]] =
    getOptional[Seq[Number]](path)

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
  @deprecated("Use underlying.getObjectList with reference config entry", "2.6.0")
  def getObjectList(path: String): Option[java.util.List[_ <: ConfigObject]] =
    readValue[java.util.List[_ <: ConfigObject]](path, underlying.getObjectList(path))

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
  @deprecated("Use underlying.getStringList with reference config entry", "2.6.0")
  def getStringList(path: String): Option[java.util.List[java.lang.String]] =
    readValue(path, underlying.getStringList(path))

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
  @deprecated("Use get[Seq[String]] with reference config entry", "2.6.0")
  def getStringSeq(path: String): Option[Seq[java.lang.String]] =
    getOptional[Seq[String]](path)

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
  @deprecated("Use get[ConfigObject] with reference config entry", "2.6.0")
  def getObject(path: String): Option[ConfigObject] =
    getOptional[ConfigObject](path)

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
   *
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
    val origin = Option(if (underlying.hasPath(path)) underlying.getValue(path).origin else underlying.root.origin)
    Configuration.configError(message, origin, e)
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
    Configuration.configError(message, Option(underlying.root.origin), e)
  }
}

/**
 * A config loader
 */
trait ConfigLoader[A] { self =>
  def load(config: Config, path: String = ""): A
  def map[B](f: A => B): ConfigLoader[B] = new ConfigLoader[B] {
    def load(config: Config, path: String): B = {
      f(self.load(config, path))
    }
  }
}

object ConfigLoader {

  def apply[A](f: Config => String => A): ConfigLoader[A] = new ConfigLoader[A] {
    def load(config: Config, path: String): A = f(config)(path)
  }

  import scala.collection.JavaConverters._

  implicit val stringLoader: ConfigLoader[String] = ConfigLoader(_.getString)
  implicit val seqStringLoader: ConfigLoader[Seq[String]] = ConfigLoader(_.getStringList).map(_.asScala)

  implicit val intLoader: ConfigLoader[Int] = ConfigLoader(_.getInt)
  implicit val seqIntLoader: ConfigLoader[Seq[Int]] = ConfigLoader(_.getIntList).map(_.asScala.map(_.toInt))

  implicit val booleanLoader: ConfigLoader[Boolean] = ConfigLoader(_.getBoolean)
  implicit val seqBooleanLoader: ConfigLoader[Seq[Boolean]] =
    ConfigLoader(_.getBooleanList).map(_.asScala.map(_.booleanValue))

  implicit val durationLoader: ConfigLoader[Duration] = ConfigLoader { config => path =>
    if (!config.getIsNull(path)) config.getDuration(path).toNanos.nanos else Duration.Inf
  }

  // Note: this does not support null values but it added for convenience
  implicit val seqDurationLoader: ConfigLoader[Seq[Duration]] =
    ConfigLoader(_.getDurationList).map(_.asScala.map(_.toNanos.nanos))

  implicit val finiteDurationLoader: ConfigLoader[FiniteDuration] =
    ConfigLoader(_.getDuration).map(_.toNanos.nanos)
  implicit val seqFiniteDurationLoader: ConfigLoader[Seq[FiniteDuration]] =
    ConfigLoader(_.getDurationList).map(_.asScala.map(_.toNanos.nanos))

  implicit val doubleLoader: ConfigLoader[Double] = ConfigLoader(_.getDouble)
  implicit val seqDoubleLoader: ConfigLoader[Seq[Double]] =
    ConfigLoader(_.getDoubleList).map(_.asScala.map(_.doubleValue))

  implicit val numberLoader: ConfigLoader[Number] = ConfigLoader(_.getNumber)
  implicit val seqNumberLoader: ConfigLoader[Seq[Number]] = ConfigLoader(_.getNumberList).map(_.asScala)

  implicit val longLoader: ConfigLoader[Long] = ConfigLoader(_.getLong)
  implicit val seqLongLoader: ConfigLoader[Seq[Long]] =
    ConfigLoader(_.getDoubleList).map(_.asScala.map(_.longValue))

  implicit val bytesLoader: ConfigLoader[ConfigMemorySize] = ConfigLoader(_.getMemorySize)
  implicit val seqBytesLoader: ConfigLoader[Seq[ConfigMemorySize]] = ConfigLoader(_.getMemorySizeList).map(_.asScala)

  implicit val configLoader: ConfigLoader[Config] = ConfigLoader(_.getConfig)
  implicit val configListLoader: ConfigLoader[ConfigList] = ConfigLoader(_.getList)
  implicit val configObjectLoader: ConfigLoader[ConfigObject] = ConfigLoader(_.getObject)
  implicit val seqConfigLoader: ConfigLoader[Seq[Config]] = ConfigLoader(_.getConfigList).map(_.asScala)

  implicit val configurationLoader: ConfigLoader[Configuration] = configLoader.map(Configuration(_))
  implicit val seqConfigurationLoader: ConfigLoader[Seq[Configuration]] = seqConfigLoader.map(_.map(Configuration(_)))

  private[play] implicit val playConfigLoader: ConfigLoader[PlayConfig] = configLoader.map(PlayConfig(_))
  private[play] implicit val seqPlayConfigLoader: ConfigLoader[Seq[PlayConfig]] = seqConfigLoader.map(_.map(PlayConfig(_)))

  /**
   * Loads a value, interpreting a null value as None and any other value as Some(value).
   */
  implicit def optionLoader[A](implicit valueLoader: ConfigLoader[A]): ConfigLoader[Option[A]] = new ConfigLoader[Option[A]] {
    def load(config: Config, path: String): Option[A] = {
      if (config.getIsNull(path)) None else {
        val value = valueLoader.load(config, path)
        Some(value)
      }
    }
  }

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

// TODO: remove when play projects (play-slick et al.) stop depending on PlayConfig
@deprecated("Use play.api.Configuration", "2.6.0")
private[play] class PlayConfig(val underlying: Config) {
  def get[A](path: String)(implicit loader: ConfigLoader[A]): A = {
    loader.load(underlying, path)
  }

  def getPrototypedSeq(path: String, prototypePath: String = "prototype.$path"): Seq[PlayConfig] = {
    Configuration(underlying).getPrototypedSeq(path, prototypePath).map(c => PlayConfig(c.underlying))
  }

  def getPrototypedMap(path: String, prototypePath: String = "prototype.$path"): Map[String, PlayConfig] = {
    Configuration(underlying).getPrototypedMap(path, prototypePath).mapValues(c => PlayConfig(c.underlying))
  }

  def getDeprecated[A: ConfigLoader](path: String, deprecatedPaths: String*): A = {
    Configuration(underlying).getDeprecated(path, deprecatedPaths: _*)
  }

  def getDeprecatedWithFallback(path: String, deprecated: String, parent: String = ""): PlayConfig = {
    PlayConfig(Configuration(underlying).getDeprecatedWithFallback(path, deprecated, parent).underlying)
  }

  def reportError(path: String, message: String, e: Option[Throwable] = None): PlayException = {
    Configuration(underlying).reportError(path, message, e)
  }

  def subKeys: Set[String] = Configuration(underlying).subKeys

  private[play] def reportDeprecation(path: String, deprecated: String): Unit = {
    Configuration(underlying).reportDeprecation(path, deprecated)
  }
}

@deprecated("Use play.api.Configuration", "2.6.0")
private[play] object PlayConfig {
  def apply(underlying: Config) = new PlayConfig(underlying)
  def apply(configuration: Configuration) = new PlayConfig(configuration.underlying)
}
