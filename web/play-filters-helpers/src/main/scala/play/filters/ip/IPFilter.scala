/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.ip

import java.net.InetAddress

import com.google.common.net.InetAddresses
import jakarta.inject.Inject
import jakarta.inject.Provider
import jakarta.inject.Singleton
import play.api.http.HttpErrorHandler
import play.api.http.HttpErrorInfo
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.SimpleModule
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.api.mvc.request.RemoteNode
import play.api.Configuration
import play.api.Logger
import play.core.j.JavaHttpErrorHandlerAdapter

/**
 * A filter to allow or deny selected remote identities.
 *
 * For documentation on configuring this filter, please see the Play documentation at
 * [[https://www.playframework.com/documentation/latest/IPFilter]]
 *
 * @param config An IP filter configuration object
 * @param httpErrorHandler handling failed token error.
 */
@Singleton
class IPFilter @Inject() (config: IPFilterConfig, httpErrorHandler: HttpErrorHandler) extends EssentialFilter {

  private val logger = Logger(getClass)

  // Java API
  def this(
      config: IPFilterConfig,
      errorHandler: play.http.HttpErrorHandler
  ) = {
    this(config, new JavaHttpErrorHandlerAdapter(errorHandler))
  }

  override def apply(next: EssentialAction): EssentialAction = EssentialAction { req =>
    if (this.config.ipAllowed(req)) {
      next(req)
    } else {
      logger.warn(s"Access denied to ${req.path} for remote identity ${req.remote.identity}.")
      Accumulator.done(
        httpErrorHandler.onClientError(
          req.addAttr(HttpErrorHandler.Attrs.HttpErrorInfo, HttpErrorInfo("ip-filter")),
          this.config.accessDeniedHttpStatusCode,
          s"Remote identity not allowed: ${req.remote.identity}"
        )
      )
    }
  }
}

case class IPFilterConfig(
    accessDeniedHttpStatusCode: Int = Status.FORBIDDEN,
    ipAllowed: RequestHeader => Boolean = _ => false
)

object IPFilterConfig {

  private sealed trait RemoteNodeMatcher {
    def matches(node: RemoteNode): Boolean
  }

  private case object UnknownMatcher extends RemoteNodeMatcher {
    override def matches(node: RemoteNode): Boolean = node match {
      case RemoteNode.Unknown(_) => true
      case _                     => false
    }
  }

  private final case class ObfuscatedMatcher(identifier: String) extends RemoteNodeMatcher {
    override def matches(node: RemoteNode): Boolean = node match {
      case RemoteNode.Obfuscated(remoteIdentifier, _) => identifier == remoteIdentifier
      case _                                          => false
    }
  }

  private final case class IpMatcher(network: Array[Byte], prefixLength: Int) extends RemoteNodeMatcher {
    override def matches(node: RemoteNode): Boolean = node match {
      case RemoteNode.Ip(address, _) => matches(address)
      case _                         => false
    }

    private def matches(address: InetAddress): Boolean = {
      val candidate = address.getAddress
      if (candidate.length != network.length) {
        false
      } else {
        val completeBytes = prefixLength / 8
        var index         = 0
        while (index < completeBytes && network(index) == candidate(index)) {
          index += 1
        }

        if (index != completeBytes) {
          false
        } else {
          val remainingBits = prefixLength % 8
          remainingBits == 0 || {
            val mask = 0xff << (8 - remainingBits)
            (network(completeBytes) & mask) == (candidate(completeBytes) & mask)
          }
        }
      }
    }
  }

  private val MatcherSyntax =
    "expected 'unknown', an RFC 7239 obfuscated identifier (for example '_edge'), " +
      "a numeric IPv4/IPv6 literal, or a numeric IP literal with a CIDR prefix"

  private def parseMatchers(config: Configuration, key: String): Seq[RemoteNodeMatcher] =
    config
      .getOptional[Seq[String]](key)
      .getOrElse(Seq.empty)
      .zipWithIndex
      .map { case (value, index) => parseMatcher(config, key, index, value) }

  private def parseMatcher(
      config: Configuration,
      key: String,
      index: Int,
      value: String
  ): RemoteNodeMatcher = {
    def invalid(reason: String): Nothing =
      throw config.reportError(
        key,
        s"Invalid play.filters.ip.$key entry at index $index ('$value'): $reason; $MatcherSyntax."
      )

    if (value == null || value.isEmpty) {
      invalid("the value must not be empty")
    }
    if (value.exists(_.isWhitespace)) {
      invalid("whitespace is not allowed")
    }
    if (!value.forall(char => char >= '!' && char <= '~')) {
      invalid("only printable ASCII characters are allowed")
    }

    if (value.equalsIgnoreCase("unknown")) {
      UnknownMatcher
    } else if (value.charAt(0) == '_') {
      try {
        RemoteNode.Obfuscated(value, None)
        ObfuscatedMatcher(value)
      } catch {
        case exception: IllegalArgumentException => invalid(exception.getMessage)
      }
    } else {
      if (value.indexOf('%') >= 0) {
        invalid("IPv6 zone identifiers are not allowed")
      }

      val slashIndex = value.indexOf('/')
      if (slashIndex != value.lastIndexOf('/')) {
        invalid("a matcher may contain at most one '/'")
      }

      val (literal, configuredPrefix) =
        if (slashIndex < 0) {
          (value, None)
        } else {
          val prefix = value.substring(slashIndex + 1)
          if (prefix.isEmpty || !prefix.forall(char => char >= '0' && char <= '9')) {
            invalid("the CIDR prefix must be a non-negative decimal integer")
          }
          (value.substring(0, slashIndex), Some(prefix))
        }

      val address =
        try InetAddresses.forString(literal)
        catch {
          case _: IllegalArgumentException => invalid(s"'$literal' is not a numeric IP literal")
        }

      val addressBits  = address.getAddress.length * 8
      val prefixLength = configuredPrefix match {
        case None         => addressBits
        case Some(prefix) =>
          val parsed =
            try prefix.toInt
            catch {
              case _: NumberFormatException => invalid(s"CIDR prefix '$prefix' is too large")
            }
          if (parsed > addressBits) {
            invalid(s"CIDR prefix $parsed is outside the valid range 0-$addressBits for '$literal'")
          }
          parsed
      }

      IpMatcher(address.getAddress, prefixLength)
    }
  }

  /**
   * Parses out the IPFilterConfig from play.api.Configuration (usually this means application.conf).
   */
  def fromConfiguration(conf: Configuration): IPFilterConfig = {
    val ipConfig                   = conf.get[Configuration]("play.filters.ip")
    val accessDeniedHttpStatusCode = ipConfig.getOptional[Int]("accessDeniedHttpStatusCode").getOrElse(Status.FORBIDDEN)
    val whiteList                  = parseMatchers(ipConfig, "whiteList")
    val blackList                  = parseMatchers(ipConfig, "blackList")

    @inline def allowRemote(req: RequestHeader): Boolean = {
      if (whiteList.isEmpty) {
        if (blackList.isEmpty) {
          true // By default, all remote identities are allowed.
        } else {
          // A blacklist denies only matching remote identities.
          blackList.forall(!_.matches(req.remote.node))
        }
      } else {
        // A non-empty whitelist takes precedence and permits only matching remote identities.
        whiteList.exists(_.matches(req.remote.node))
      }
    }

    val whitelistModifiers = ipConfig.get[Seq[String]]("routeModifiers.whiteList")
    val blacklistModifiers = ipConfig.get[Seq[String]]("routeModifiers.blackList")

    @inline def checkRouteModifiers(rh: RequestHeader): Boolean = {
      import play.api.routing.Router.RequestImplicits._
      if (whitelistModifiers.isEmpty) {
        blacklistModifiers.isEmpty || blacklistModifiers.exists(rh.hasRouteModifier)
      } else {
        !whitelistModifiers.exists(rh.hasRouteModifier)
      }
    }

    val ipAllowed: RequestHeader => Boolean = { rh => !checkRouteModifiers(rh) || allowRemote(rh) }

    IPFilterConfig(
      accessDeniedHttpStatusCode,
      ipAllowed,
    )
  }

}

@Singleton
class IPFilterConfigProvider @Inject() (conf: Configuration) extends Provider[IPFilterConfig] {
  lazy val get: IPFilterConfig = IPFilterConfig.fromConfiguration(conf)
}

class IPFilterModule
    extends SimpleModule(
      bind[IPFilterConfig].toProvider[IPFilterConfigProvider],
      bind[IPFilter].toSelf
    )

/**
 * The IP filter components.
 */
trait IPFilterComponents {
  def configuration: Configuration

  def httpErrorHandler: HttpErrorHandler

  lazy val ipFilterConfig: IPFilterConfig = IPFilterConfig.fromConfiguration(configuration)
  lazy val ipFilter: IPFilter             = new IPFilter(ipFilterConfig, httpErrorHandler)
}
