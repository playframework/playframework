package play.api

import java.io._

import scala.util.parsing.input._
import scala.util.parsing.combinator._
import scala.util.matching._

/**
 * This object provides a set of operations to create Configuration values.
 *
 * For example, to load a Configuration from a file:
 * {{{
 * val config = Configuration.fromFile(app.getFile("conf/application.conf"))
 * }}}
 */
object Configuration {

  /**
   * Load new configuration from a file.
   *
   * For example:
   * {{{
   * val config = Configuration.fromFile(app.getFile("conf/application.conf"))
   * }}}
   *
   * @param file Configuration file to read
   * @return A Configuration instance
   */
  def fromFile(file: File) = {
    Configuration(new ConfigurationParser(file).parse.map(c => c.key -> c).toMap)
  }

  class ConfigurationParser(configurationFile: File) extends RegexParsers {

    case class Comment(msg: String)

    override def skipWhitespace = false
    override val whiteSpace = """[ \t]+""".r

    override def phrase[T](p: Parser[T]) = new Parser[T] {
      lastNoSuccess = null
      def apply(in: Input) = p(in) match {
        case s @ Success(out, in1) =>
          if (in1.atEnd)
            s
          else if (lastNoSuccess == null || lastNoSuccess.next.pos < in1.pos)
            Failure("end of input expected", in1)
          else
            lastNoSuccess
        case _ => lastNoSuccess
      }
    }

    def namedError[A](p: Parser[A], msg: String) = Parser[A] { i =>
      p(i) match {
        case Failure(_, in) => Failure(msg, in)
        case o => o
      }
    }

    def end = """\s*""".r
    def newLine = namedError("\n", "End of line expected")
    def blankLine = ignoreWhiteSpace <~ newLine ^^ { case _ => Comment("") }
    def ignoreWhiteSpace = opt(whiteSpace)

    def comment = """#.*""".r ^^ { case s => Comment(s) }

    def configKey = namedError("""[a-zA-Z0-9_.]+""".r, "Configuration key expected")
    def configValue = namedError(""".+""".r, "Configuration value expected")
    def config = ignoreWhiteSpace ~ configKey ~ (ignoreWhiteSpace ~ "=" ~ ignoreWhiteSpace) ~ configValue ^^ {
      case (_ ~ k ~ _ ~ v) => Config(k, v.trim, configurationFile)
    }

    def sentence = (comment | positioned(config)) <~ newLine

    def parser = phrase((sentence | blankLine *) <~ end) ^^ {
      case configs => configs.collect {
        case c @ Config(_, _, _) => c
      }
    }

    def parse = {
      parser(new CharSequenceReader(scalax.file.Path(configurationFile).slurpString)) match {
        case Success(configs, _) => configs
        case NoSuccess(message, in) => {
          throw new PlayException("Configuration error", message) with ExceptionSource {
            def line = Some(in.pos.line)
            def position = Some(in.pos.column - 1)
            def input = Some(scalax.file.Path(configurationFile))
            def sourceName = Some(configurationFile.getAbsolutePath)
          }
        }
      }
    }

  }

}

/**
 * A configuration item.
 *
 * @param key The configuration key
 * @param value The configuration value as plain String
 * @param file The file from which this configuration was read
 */
case class Config(key: String, value: String, file: File) extends Positional

/**
 * A full configuration set.
 *
 * The best way to obtain this configuration is to read it from a file using:
 * {{{
 * val config = Configuration.fromFile(app.getFile("conf/application.conf"))
 * }}}
 *
 * @param data The configuration data.
 * @param root The root key of this configuration if it represents a sub configuration.
 */
case class Configuration(data: Map[String, Config], root: String = "") {

  /**
   * Retrieve a configuration item by key
   *
   * @param key Configuration key (relative to configuration root key).
   * @return Maybe a configuration item.
   */
  def get(key: String): Option[Config] = data.get(key)

  /**
   * Retrieve a configuration value as String
   *
   * This method support an optional set of valid values:
   * {{{
   * val mode = configuration.getString("engine.mode", Some(Set("dev","prod")))
   * }}}
   *
   * A configuration error will be thrown if the configuration value does not match any of the required values.
   *
   * @param key Configuration key (relative to configuration root key).
   * @param validValues Valid values for this configuration.
   * @return Maybe a configuration value.
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
   * Retrieve a configuration value as Int
   *
   * Example:
   * {{{
   * val poolSize = configuration.getInt("engine.pool.size")
   * }}}
   *
   * A configuration error will be thrown if the configuration value is not a valid Int.
   *
   * @param key Configuration key (relative to configuration root key).
   * @return Maybe a configuration value.
   */
  def getInt(key: String): Option[Int] = data.get(key).map { c =>
    try {
      Integer.parseInt(c.value)
    } catch {
      case e => throw error("Integer value required", c)
    }
  }

  /**
   * Retrieve a configuration value as Boolean
   *
   * Example:
   * {{{
   * val isEnabled = configuration.getString("engine.isEnabled")
   * }}}
   *
   * A configuration error will be thrown if the configuration value is not a valid Boolean.
   * Authorized vales are yes/no or true/false.
   *
   * @param key Configuration key (relative to configuration root key).
   * @return Maybe a configuration value.
   */
  def getBoolean(key: String): Option[Boolean] = data.get(key).map { c =>
    c.value match {
      case "true" => true
      case "yes" => true
      case "false" => false
      case "no" => false
      case o => throw error("Boolean value required", c)
    }
  }

  /**
   * Retrieve a sub configuration, ie. a configuration instance containing all key starting with a prefix.
   *
   * For example:
   * {{{
   * val engineConfig = configuration.getSub("engine")
   * }}}
   *
   * The the root key of this new configuration will be 'engine', and you can access any sub keys relatively.
   *
   * @param key The root prefix for this sub configuration.
   * @return Maybe a new configuration
   */
  def getSub(key: String): Option[Configuration] = Option(data.filterKeys(_.startsWith(key + ".")).map {
    case (k, c) => k.drop(key.size + 1) -> c
  }.toMap).filterNot(_.isEmpty).map(Configuration(_, full(key) + "."))

  /**
   * Retrieve a sub configuration, ie. a configuration instance containing all key starting with a prefix.
   *
   * For example:
   * {{{
   * val engineConfig = configuration.sub("engine")
   * }}}
   *
   * The the root key of this new configuration will be 'engine', and you can access any sub keys relatively.
   *
   * This method throw an error if the sub configuration cannot be found.
   *
   * @param key The root prefix for this sub configuration.
   * @return A new configuration or throw an error.
   */
  def sub(key: String): Configuration = getSub(key).getOrElse {
    throw globalError("No configuration found '" + key + "'")
  }

  /**
   * @return The set of keys available in this configuration.
   */
  def keys: Set[String] = data.keySet

  /**
   * @return The set of direct sub keys available in this configuration.
   */
  def subKeys: Set[String] = keys.map(_.split('.').head)

  /**
   * Create a configuration error for a specific congiguration key.
   *
   * For example:
   * {{{
   * throw configuration.reportError("engine.connectionUrl", "Cannot connect!")
   * }}}
   *
   * @param key The configuration key, related to this error.
   * @param message The error message.
   * @param e Maybe the related exception.
   * @return A configuration exception.
   */
  def reportError(key: String, message: String, e: Option[Throwable] = None) = {
    data.get(key).map { config =>
      error(message, config, e)
    }.getOrElse {
      new PlayException("Configuration error", full(key) + ": " + message, e)
    }
  }

  /**
   * Translate any relative key to an absolute key.
   *
   * @param key Configuration key (relative to configuration root key).
   * @return The complete key
   */
  def full(key: String) = root + key

  /**
   * Create a configuration error for this configuration.
   *
   * For example:
   * {{{
   * throw configuration.globalError("Missing configuration key: [yop.url]")
   * }}}
   *
   * @param message The error message.
   * @param e Maybe the related exception.
   * @return A configuration exception.
   */
  def globalError(message: String, e: Option[Throwable] = None) = {
    data.headOption.map { c =>
      new PlayException("Configuration error", message, e) with ExceptionSource {
        def line = Some(c._2.pos.line)
        def position = None
        def input = Some(scalax.file.Path(c._2.file))
        def sourceName = Some(c._2.file.getAbsolutePath)
      }
    }.getOrElse {
      new PlayException("Configuration error", message, e)
    }
  }

  private def error(message: String, config: Config, e: Option[Throwable] = None) = {
    new PlayException("Configuration error", message, e) with ExceptionSource {
      def line = Some(config.pos.line)
      def position = Some(config.pos.column + config.key.size)
      def input = Some(scalax.file.Path(config.file))
      def sourceName = Some(config.file.getAbsolutePath)
    }
  }

}