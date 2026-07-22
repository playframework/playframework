/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.request

import java.security.cert.X509Certificate

import scala.jdk.javaapi.OptionConverters

/**
 * One accepted `X-Forwarded-Client-Cert` assertion.
 *
 * Repeated values retain their header order. [[certificate]] is the asserted leaf certificate;
 * [[chain]] contains the remaining presented chain in leaf-to-root order and never repeats the leaf.
 */
final case class XForwardedClientCert(
    by: Vector[String],
    hash: Option[String],
    certificate: Option[X509Certificate],
    chain: Vector[X509Certificate],
    subject: Option[String],
    uris: Vector[String],
    dnsNames: Vector[String]
) {
  require(by != null && by.forall(_ != null), "XFCC by values must not be null or contain null")
  require(hash != null && hash.forall(_ != null), "An XFCC hash option must not be null or contain null")
  require(
    certificate != null && certificate.forall(_ != null),
    "An XFCC certificate option must not be null or contain null"
  )
  require(chain != null && chain.forall(_ != null), "An XFCC certificate chain must not be null or contain null")
  require(subject != null && subject.forall(_ != null), "An XFCC subject option must not be null or contain null")
  require(uris != null && uris.forall(_ != null), "XFCC URI values must not be null or contain null")
  require(dnsNames != null && dnsNames.forall(_ != null), "XFCC DNS values must not be null or contain null")
  require(certificate.isDefined || chain.isEmpty, "An XFCC certificate chain requires a leaf certificate")
  require(
    certificate.forall(leaf => !chain.contains(leaf)),
    "An XFCC certificate chain must not repeat the leaf certificate"
  )

  /** The asserted leaf-and-chain sequence, or empty when no certificate was asserted. */
  def certificates: Vector[X509Certificate] = certificate.toVector ++ chain

  /** Convert this assertion to the Java API. */
  def asJava: play.mvc.Http.XForwardedClientCert =
    new play.mvc.Http.XForwardedClientCert(
      java.util.List.copyOf(play.libs.Scala.asJava(by)),
      OptionConverters.toJava(hash),
      OptionConverters.toJava(certificate),
      java.util.List.copyOf(play.libs.Scala.asJava(chain)),
      OptionConverters.toJava(subject),
      java.util.List.copyOf(play.libs.Scala.asJava(uris)),
      java.util.List.copyOf(play.libs.Scala.asJava(dnsNames))
    )
}

object XForwardedClientCert {

  /** Java-friendly factory that defensively normalizes all repeated values to vectors. */
  def create(
      by: Seq[String],
      hash: Option[String],
      certificate: Option[X509Certificate],
      chain: Seq[X509Certificate],
      subject: Option[String],
      uris: Seq[String],
      dnsNames: Seq[String]
  ): XForwardedClientCert = {
    require(by != null, "XFCC by values must not be null")
    require(chain != null, "An XFCC certificate chain must not be null")
    require(uris != null, "XFCC URI values must not be null")
    require(dnsNames != null, "XFCC DNS values must not be null")
    XForwardedClientCert(by.toVector, hash, certificate, chain.toVector, subject, uris.toVector, dnsNames.toVector)
  }
}
