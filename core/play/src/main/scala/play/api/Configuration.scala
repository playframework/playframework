/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

import java.io.File
import java.net.URI
import java.net.URL
import java.util.Properties
import java.time.Period
import java.time.temporal.TemporalAmount

import com.typesafe.config._
import play.twirl.api.utils.StringEscapeUtils
import play.utils.PlayIO

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration
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
  def load(
      classLoader: ClassLoader,
      properties: Properties,
      directSettings: Map[String, AnyRef],
      allowMissingApplicationConf: Boolean
  ): Configuration = {
    try {
      // Iterating through the system properties is prone to ConcurrentModificationExceptions
      // (such as in unit tests), which is why Typesafe config maintains a cache for it.
      // So, if the passed in properties *are* the system properties, don't parse it ourselves.
      val userDefinedProperties = if (properties eq System.getProperties) {
        ConfigFactory.empty()
      } else {
        ConfigFactory.parseProperties(properties)
      }

      // Inject our direct settings into the config.
      val directConfig: Config = ConfigFactory.parseMap(directSettings.asJava)

      // Resolve application.conf
      val applicationConfig: Config = {
        def setting(key: String): Option[String] =
          directSettings.get(key).orElse(Option(properties.getProperty(key))).map(_.toString)

        // The additional config.resource/config.file logic exists because
        // ConfigFactory.defaultApplication will blow up if those are defined but the file is missing
        // despite "setAllowMissing" (see DefaultConfigLoadingStrategy).
        // In DevMode this is relevant for config.resource as reloader.currentApplicationClassLoader
        // is null at the start of 'run', so the application classpath isn't available, which means
        // the resource will be missing.  For consistency (and historic behaviour) do config.file too.
        {
          setting("config.resource").map(resource => ConfigFactory.parseResources(classLoader, resource))
        }.orElse {
            setting("config.file").map(fileName => ConfigFactory.parseFileAnySyntax(new File(fileName)))
          }
          .getOrElse {
            val parseOptions = ConfigParseOptions.defaults
              .setClassLoader(classLoader)
              .setAllowMissing(allowMissingApplicationConf)
            ConfigFactory.defaultApplication(parseOptions)
          }
      }

      // Resolve another .conf file so that we can override values in Akka's
      // reference.conf, but still make it possible for users to override
      // Play's values in their application.conf.
      val playOverridesConfig: Config =
        ConfigFactory.parseResources(classLoader, "play/reference-overrides.conf")

      // Combine all the config together into one big config
      val combinedConfig: Config = Seq(
        userDefinedProperties,
        directConfig,
        applicationConfig,
        playOverridesConfig,
      ).reduceLeft(_.withFallback(_))

      // Resolve settings. Among other things, the `play.server.dir` setting defined in directConfig will
      // be substituted into the default settings in referenceConfig.
      val resolvedConfig = ConfigFactory.load(classLoader, combinedConfig)

      Configuration(resolvedConfig)
    } catch {
      case e: ConfigException => throw configError(e.getMessage, Option(e.origin), Some(e))
    }
  }

  /**
   * Load a new Configuration from the Environment.
   */
  def load(environment: Environment, devSettings: Map[String, AnyRef]): Configuration = {
    val allowMissingApplicationConf = environment.mode == Mode.Test
    load(environment.classLoader, System.getProperties, devSettings, allowMissingApplicationConf)
  }

  /**
   * Load a new Configuration from the Environment.
   */
  def load(environment: Environment): Configuration = load(environment, Map.empty)

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
      case map: Map[_, _]        => map.mapValues(toJava).toMap.asJava
      case iterable: Iterable[_] => iterable.map(toJava).asJava
      case v                     => v
    }

    Configuration(ConfigFactory.parseMap(toJava(data).asInstanceOf[java.util.Map[String, AnyRef]]))
  }

  /**
   * Create a new Configuration from the given key-value pairs.
   */
  def apply(data: (String, Any)*): Configuration = from(data.toMap)

  private[api] def configError(
      message: String,
      origin: Option[ConfigOrigin] = None,
      e: Option[Throwable] = None
  ): PlayException = {
    /*
      The stable values here help us from putting a reference to a ConfigOrigin inside the anonymous ExceptionSource.
      This is necessary to keep the Exception serializable, because ConfigOrigin is not serializable.
     */
    val originLine       = origin.map(_.lineNumber: java.lang.Integer).orNull
    val originSourceName = origin.map(_.filename).orNull
    val originUrlOpt     = origin.flatMap(o => Option(o.url))
    new PlayException.ExceptionSource("Configuration error", message, e.orNull) {
      def line              = originLine
      def position          = null
      def input             = originUrlOpt.map(PlayIO.readUrlAsString).orNull
      def sourceName        = originSourceName
      override def toString = "Configuration error: " + getMessage
    }
  }

  private[Configuration] val logger = Logger(getClass)
}

/**
 * A full configuration set.
 *
 * The underlying implementation is provided by https://github.com/typesafehub/config.
 *
 * @param underlying the underlying Config implementation
 */
case class Configuration(underlying: Config) {
  import Configuration.logger

  private[play] def reportDeprecation(path: String, deprecated: String): Unit = {
    val origin = underlying.getValue(deprecated).origin
    logger.warn(s"${origin.description}: $deprecated is deprecated, use $path instead")
  }

  /**
   * Merge two configurations. The second configuration overrides the first configuration.
   * This is the opposite direction of `Config`'s `withFallback` method.
   */
  @deprecated("Use withFallback instead", since = "2.8.0")
  def ++(other: Configuration): Configuration = {
    Configuration(other.underlying.withFallback(underlying))
  }

  /**
   * Merge two configurations. The second configuration will act as the fallback for the first
   * configuration.
   */
  def withFallback(other: Configuration): Configuration = {
    Configuration(underlying.withFallback(other.underlying))
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
    try {
      if (underlying.hasPath(path)) Some(get[A](path)) else None
    } catch {
      case NonFatal(e) => throw reportError(path, e.getMessage, Some(e))
    }
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
    deprecatedPaths
      .collectFirst {
        case deprecated if underlying.hasPath(deprecated) =>
          reportDeprecation(path, deprecated)
          get[A](deprecated)
      }
      .getOrElse {
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
  def map[B](f: A => B): ConfigLoader[B] = (config, path) => f(self.load(config, path))
}

object ConfigLoader {
  def apply[A](f: Config => String => A): ConfigLoader[A] = f(_)(_)

  implicit val stringLoader: ConfigLoader[String]         = ConfigLoader(_.getString)
  implicit val seqStringLoader: ConfigLoader[Seq[String]] = ConfigLoader(_.getStringList).map(_.asScala.toSeq)

  implicit val intLoader: ConfigLoader[Int]         = ConfigLoader(_.getInt)
  implicit val seqIntLoader: ConfigLoader[Seq[Int]] = ConfigLoader(_.getIntList).map(_.asScala.map(_.toInt).toSeq)

  implicit val booleanLoader: ConfigLoader[Boolean] = ConfigLoader(_.getBoolean)
  implicit val seqBooleanLoader: ConfigLoader[Seq[Boolean]] =
    ConfigLoader(_.getBooleanList).map(_.asScala.map(_.booleanValue).toSeq)

  implicit val finiteDurationLoader: ConfigLoader[FiniteDuration] =
    ConfigLoader(_.getDuration).map(javaDurationToScala)

  implicit val seqFiniteDurationLoader: ConfigLoader[Seq[FiniteDuration]] =
    ConfigLoader(_.getDurationList).map(_.asScala.map(javaDurationToScala).toSeq)

  implicit val durationLoader: ConfigLoader[Duration] = ConfigLoader { config => path =>
    if (config.getIsNull(path)) Duration.Inf
    else if (config.getString(path) == "infinite") Duration.Inf
    else finiteDurationLoader.load(config, path)
  }

  // Note: this does not support null values but it added for convenience
  implicit val seqDurationLoader: ConfigLoader[Seq[Duration]] =
    seqFiniteDurationLoader.map(identity[Seq[Duration]])

  implicit val periodLoader: ConfigLoader[Period] = ConfigLoader(_.getPeriod)

  implicit val temporalLoader: ConfigLoader[TemporalAmount] = ConfigLoader(_.getTemporal)

  implicit val doubleLoader: ConfigLoader[Double] = ConfigLoader(_.getDouble)
  implicit val seqDoubleLoader: ConfigLoader[Seq[Double]] =
    ConfigLoader(_.getDoubleList).map(_.asScala.map(_.doubleValue).toSeq)

  implicit val numberLoader: ConfigLoader[Number]         = ConfigLoader(_.getNumber)
  implicit val seqNumberLoader: ConfigLoader[Seq[Number]] = ConfigLoader(_.getNumberList).map(_.asScala.toSeq)

  implicit val longLoader: ConfigLoader[Long] = ConfigLoader(_.getLong)
  implicit val seqLongLoader: ConfigLoader[Seq[Long]] =
    ConfigLoader(_.getLongList).map(_.asScala.map(_.longValue).toSeq)

  implicit val bytesLoader: ConfigLoader[ConfigMemorySize] = ConfigLoader(_.getMemorySize)
  implicit val seqBytesLoader: ConfigLoader[Seq[ConfigMemorySize]] =
    ConfigLoader(_.getMemorySizeList).map(_.asScala.toSeq)

  implicit val configLoader: ConfigLoader[Config]             = ConfigLoader(_.getConfig)
  implicit val configListLoader: ConfigLoader[ConfigList]     = ConfigLoader(_.getList)
  implicit val configObjectLoader: ConfigLoader[ConfigObject] = ConfigLoader(_.getObject)
  implicit val seqConfigLoader: ConfigLoader[Seq[Config]]     = ConfigLoader(_.getConfigList).map(_.asScala.toSeq)

  implicit val configurationLoader: ConfigLoader[Configuration]         = configLoader.map(Configuration(_))
  implicit val seqConfigurationLoader: ConfigLoader[Seq[Configuration]] = seqConfigLoader.map(_.map(Configuration(_)))

  implicit val urlLoader: ConfigLoader[URL] = ConfigLoader(_.getString).map(new URL(_))
  implicit val uriLoader: ConfigLoader[URI] = ConfigLoader(_.getString).map(new URI(_))

  private def javaDurationToScala(javaDuration: java.time.Duration): FiniteDuration =
    Duration.fromNanos(javaDuration.toNanos)

  /**
   * Loads a value, interpreting a null value as None and any other value as Some(value).
   */
  implicit def optionLoader[A](implicit valueLoader: ConfigLoader[A]): ConfigLoader[Option[A]] =
    (config, path) => if (config.getIsNull(path)) None else Some(valueLoader.load(config, path))

  implicit def mapLoader[A](implicit valueLoader: ConfigLoader[A]): ConfigLoader[Map[String, A]] =
    (config, path) => {
      val obj  = config.getObject(path)
      val conf = obj.toConfig

      obj
        .keySet()
        .iterator()
        .asScala
        .map { key =>
          // quote and escape the key in case it contains dots or special characters
          val path = "\"" + StringEscapeUtils.escapeEcmaScript(key) + "\""
          key -> valueLoader.load(conf, path)
        }
        .toMap
    }
}
