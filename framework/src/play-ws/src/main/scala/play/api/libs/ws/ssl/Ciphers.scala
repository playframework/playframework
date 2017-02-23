/*
 *
 *  * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 *
 */
package play.api.libs.ws.ssl

import javax.net.ssl.SSLContext

/**
 * This class contains sets of recommended and deprecated TLS cipher suites.
 *
 * The JSSE list of cipher suites is different from the RFC defined list, with some cipher suites prefixed with "SSL_"
 * instead of "TLS_".  A full list is available from the <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/security/SunProviders.html#SupportedCipherSuites">SunJSSE provider list</a>
 *
 * Please see https://www.playframework.com/documentation/latest/CipherSuites for more details.
 */
object Ciphers {

  // We want to prioritize ECC and perfect forward security.
  // Unfortunately, ECC is only available under the "SunEC" provider, which is part of Oracle JDK.  If you're
  // using OpenJDK, you're out of luck.
  // http://armoredbarista.blogspot.com/2013/10/how-to-use-ecc-with-openjdk.html

  def recommendedCiphers: Seq[String] = foldVersion(
    run16 = java16RecommendedCiphers,
    runHigher = java17RecommendedCiphers)

  lazy val java17RecommendedCiphers: Seq[String] = {
    SSLContext.getDefault.getDefaultSSLParameters.getCipherSuites
  }.filterNot(deprecatedCiphers.contains(_))

  val java16RecommendedCiphers: Seq[String] = Seq(
    "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
    "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
    "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
    "TLS_RSA_WITH_AES_256_CBC_SHA",
    "TLS_RSA_WITH_AES_128_CBC_SHA",
    "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
    "TLS_EMPTY_RENEGOTIATION_INFO_SCSV" // per RFC 5746
  )

  // Suite B profile for TLS (requires 1.2): http://tools.ietf.org/html/rfc6460
  // http://adambard.com/blog/the-new-ssl-basics/

  // Even 1.7 doesn't support TLS_ECDHE_ECDSA_WITH_AES_256.
  // TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256 is the best you get,
  // and it's also at the top of the default 1.7 cipher list.
  val suiteBCiphers: Seq[String] = """
                                     |TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384
                                     |TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384
                                     |TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256
                                     |TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256
                                   """.stripMargin.split("\n")

  val suiteBTransitionalCiphers: Seq[String] = """TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384
                                                 |TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384
                                                 |TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256
                                                 |TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256
                                                 |TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA
                                                 |TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA
                                               """.stripMargin.split("\n")

  // From http://op-co.de/blog/posts/android_ssl_downgrade/
  // Caveat: https://news.ycombinator.com/item?id=6548545
  val recommendedSmithCiphers = Seq(
    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
    "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
    "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
    "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
    "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
    "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
    "TLS_RSA_WITH_AES_128_CBC_SHA",
    "TLS_RSA_WITH_AES_256_CBC_SHA",
    "SSL_RSA_WITH_3DES_EDE_CBC_SHA"
  )

  val exportCiphers = """SSL_RSA_EXPORT_WITH_RC4_40_MD5
                        |SSL_RSA_EXPORT_WITH_DES40_CBC_SHA
                        |SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA
                        |SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA
                        |TLS_KRB5_EXPORT_WITH_RC4_40_SHA
                        |TLS_KRB5_EXPORT_WITH_RC4_40_MD5
                        |TLS_KRB5_EXPORT_WITH_DES_CBC_40_SHA
                        |TLS_KRB5_EXPORT_WITH_DES_CBC_40_MD5
                      """.stripMargin.split("\n").toSet

  // Per RFC2246 section 11.5 (A.5)
  val anonCiphers = """TLS_DH_anon_WITH_RC4_128_MD5
                      |TLS_DH_anon_WITH_AES_128_CBC_SHA
                      |TLS_DH_anon_EXPORT_WITH_RC4_40_MD5
                      |TLS_DH_anon_WITH_RC4_128_MD5
                      |TLS_DH_anon_EXPORT_WITH_DES40_CBC_SHA
                      |TLS_DH_anon_WITH_DES_CBC_SHA
                      |TLS_DH_anon_WITH_3DES_EDE_CBC_SHA
                      |TLS_DH_anon_WITH_AES_128_CBC_SHA
                      |TLS_DH_anon_WITH_AES_256_CBC_SHA
                      |TLS_ECDH_anon_WITH_RC4_128_SHA
                      |TLS_ECDH_anon_WITH_AES_128_CBC_SHA
                      |TLS_ECDH_anon_WITH_AES_256_CBC_SHA
                      |TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA
                      |TLS_ECDH_anon_WITH_NULL_SHA
                      |SSL_DH_anon_WITH_RC4_128_MD5
                      |SSL_DH_anon_WITH_3DES_EDE_CBC_SHA
                      |SSL_DH_anon_WITH_DES_CBC_SHA
                      |SSL_DH_anon_EXPORT_WITH_RC4_40_MD5
                      |SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA
                    """.stripMargin.split("\n").toSet

  val nullCiphers = """SSL_RSA_WITH_NULL_MD5
                      |SSL_RSA_WITH_NULL_SHA
                      |TLS_ECDH_ECDSA_WITH_NULL_SHA
                      |TLS_ECDH_RSA_WITH_NULL_SHA
                      |TLS_ECDHE_ECDSA_WITH_NULL_SHA
                      |TLS_ECDHE_RSA_WITH_NULL_SHA
                    """.stripMargin.split("\n").toSet

  val desCiphers = """SSL_RSA_WITH_DES_CBC_SHA
                     |SSL_DHE_RSA_WITH_DES_CBC_SHA
                     |SSL_DHE_DSS_WITH_DES_CBC_SHA
                     |TLS_KRB5_WITH_DES_CBC_SHA
                   """.stripMargin.split("\n").toSet

  val md5Ciphers = """SSL_RSA_WITH_RC4_128_MD5
                     |SSL_RSA_WITH_NULL_MD5
                     |SSL_RSA_EXPORT_WITH_RC4_40_MD5
                     |SSL_DH_anon_EXPORT_WITH_RC4_40_MD5
                     |SSL_DH_anon_WITH_RC4_128_MD5
                     |TLS_KRB5_WITH_DES_CBC_MD5
                     |TLS_KRB5_WITH_3DES_EDE_CBC_MD5
                     |TLS_KRB5_WITH_RC4_128_MD5
                     |TLS_KRB5_WITH_IDEA_CBC_MD5
                     |TLS_KRB5_EXPORT_WITH_DES_CBC_40_MD5
                     |TLS_KRB5_EXPORT_WITH_RC2_CBC_40_MD5
                     |TLS_KRB5_EXPORT_WITH_RC4_40_MD5
                   """.stripMargin.split("\n").toSet

  val rc4Ciphers = """SSL_DH_anon_EXPORT_WITH_RC4_40_MD5
                     |SSL_DH_anon_WITH_RC4_128_MD5
                     |SSL_RSA_EXPORT_WITH_RC4_40_MD5
                     |SSL_RSA_WITH_RC4_128_MD5
                     |SSL_RSA_WITH_RC4_128_SHA
                     |TLS_DHE_PSK_WITH_RC4_128_SHA
                     |TLS_ECDHE_ECDSA_WITH_RC4_128_SHA
                     |TLS_ECDHE_PSK_WITH_RC4_128_SHA
                     |TLS_ECDHE_RSA_WITH_RC4_128_SHA
                     |TLS_ECDH_anon_WITH_RC4_128_SHA
                     |TLS_ECDH_ECDSA_WITH_RC4_128_SHA
                     |TLS_ECDH_RSA_WITH_RC4_128_SHA
                     |TLS_KRB5_EXPORT_WITH_RC4_40_MD5
                     |TLS_KRB5_EXPORT_WITH_RC4_40_SHA
                     |TLS_KRB5_WITH_RC4_128_MD5
                     |TLS_KRB5_WITH_RC4_128_SHA
                     |TLS_PSK_WITH_RC4_128_SHA
                     |TLS_RSA_PSK_WITH_RC4_128_SHA
                   """.stripMargin.split("\n").toSet

  val sha1Ciphers = """SSL_RSA_WITH_RC4_128_SHA
                      |TLS_RSA_EXPORT_WITH_DES40_CBC_SHA
                      |TLS_ECDH_ECDSA_WITH_RC4_128_SHA
                      |TLS_ECDH_RSA_WITH_RC4_128_SHA
                      |TLS_ECDHE_ECDSA_WITH_RC4_128_SHA
                      |TLS_ECDHE_RSA_WITH_RC4_128_SHA
                      |TLS_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA
                      |TLS_DHE_DSS_WITH_DES_CBC_SHA
                      |TLS_DHE_DSS_WITH_AES_256_CBC_SHA
                      |TLS_DHE_DSS_WITH_3DES_EDE_CBC_SHA
                      |TLS_DHE_DSS_WITH_AES_128_CBC_SHA
                      |TLS_DHE_RSA_WITH_DES_CBC_SHA
                      |TLS_DHE_RSA_WITH_AES_128_CBC_SHA
                      |TLS_DHE_RSA_WITH_AES_256_CBC_SHA
                      |TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA
                      |TLS_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA
                      |TLS_DH_anon_WITH_AES_256_CBC_SHA
                    """.stripMargin.split("\n").toSet

  // See RFC 4346, RFC 5246, and RFC 5469
  // rc4 added to deprecated ciphers as of https://tools.ietf.org/html/rfc7465
  val deprecatedCiphers = desCiphers ++ nullCiphers ++ anonCiphers ++ exportCiphers ++ rc4Ciphers

}
