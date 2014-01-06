/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import javax.net.ssl.SSLContext

object Ciphers {

  // We want to prioritize ECC and perfect forward security.
  // Unfortunately, ECC is only available under the "SunEC" provider, which is part of Oracle JDK.  If you're
  // using OpenJDK, you're out of luck.
  // http://armoredbarista.blogspot.com/2013/10/how-to-use-ecc-with-openjdk.html

  def recommendedCiphers: Seq[String] = foldVersion(
    run16 = java16RecommendedCiphers,
    runHigher = java17RecommendedCiphers)

  val java17RecommendedCiphers: Seq[String] = SSLContext.getDefault.getDefaultSSLParameters.getCipherSuites

  val java16RecommendedCiphers: Seq[String] = Seq(
    "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
    "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
    "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
    "TLS_RSA_WITH_AES_256_CBC_SHA",
    "TLS_RSA_WITH_AES_128_CBC_SHA",
    "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
    "SSL_RSA_WITH_RC4_128_SHA",
    "SSL_RSA_WITH_RC4_128_MD5"
  )

  // Suite B profile for TLS (requires 1.2): http://tools.ietf.org/html/rfc6460
  // http://adambard.com/blog/the-new-ssl-basics/

  // Even 1.7 doesn't support TLS_ECDHE_ECDSA_WITH_AES_256. TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256 is the best you get,
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

  // This is painful, but there isn't a well defined format for cipher suites.
  // One possibility is using https://github.com/timw/groktls
  // Or we could rework the AlgorithmConstraints JDK 1.7 code like jdk.tls.disabledAlgorithms

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
    "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
    "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
    "TLS_RSA_WITH_AES_128_CBC_SHA",
    "TLS_RSA_WITH_AES_256_CBC_SHA",
    "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
    "SSL_RSA_WITH_RC4_128_SHA",
    "SSL_RSA_WITH_RC4_128_MD5"
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
                     |TLS_RSA_WITH_NULL_MD5
                     |TLS_RSA_EXPORT_WITH_RC4_40_MD5
                     |TLS_RSA_WITH_RC4_128_MD5
                     |TLS_RSA_EXPORT_WITH_RC2_CBC_40_MD5
                     |TLS_DH_anon_EXPORT_WITH_RC4_40_MD5
                     |TLS_DH_anon_WITH_RC4_128_MD5
                     |TLS_KRB5_WITH_DES_CBC_MD5
                     |TLS_KRB5_WITH_3DES_EDE_CBC_MD5
                     |TLS_KRB5_WITH_RC4_128_MD5
                     |TLS_KRB5_WITH_IDEA_CBC_MD5
                     |TLS_KRB5_EXPORT_WITH_DES_CBC_40_MD5
                     |TLS_KRB5_EXPORT_WITH_RC2_CBC_40_MD5
                     |TLS_KRB5_EXPORT_WITH_RC4_40_MD5
                     |TLS_RSA_EXPORT_WITH_RC4_40_MD5
                     |TLS_RSA_EXPORT_WITH_RC2_CBC_40_MD5
                   """.stripMargin.split("\n").toSet

  val rc4Ciphers = """
                     |SSL_RSA_WITH_RC4_128_MD5
                     |SSL_RSA_WITH_RC4_128_SHA
                     |SSL_RSA_EXPORT_WITH_RC4_40_MD5
                     |SSL_DH_anon_WITH_RC4_128_MD5
                     |SSL_DH_anon_EXPORT_WITH_RC4_40_MD5
                     |TLS_KRB5_WITH_RC4_128_SHA
                     |TLS_KRB5_WITH_RC4_128_MD5
                     |TLS_KRB5_EXPORT_WITH_RC4_40_SHA
                     |TLS_KRB5_EXPORT_WITH_RC4_40_MD5
                     |TLS_ECDHE_ECDSA_WITH_RC4_128_SHA
                     |TLS_ECDHE_RSA_WITH_RC4_128_SHA
                     |SSL_RSA_WITH_RC4_128_SHA
                     |TLS_ECDH_ECDSA_WITH_RC4_128_SHA
                     |TLS_ECDH_RSA_WITH_RC4_128_SHA
                     |SSL_RSA_WITH_RC4_128_MD5
                     |TLS_ECDH_anon_WITH_RC4_128_SHA
                     |SSL_DH_anon_WITH_RC4_128_MD5
                     |SSL_RSA_EXPORT_WITH_RC4_40_MD5
                     |SSL_DH_anon_EXPORT_WITH_RC4_40_MD5
                     |TLS_KRB5_WITH_RC4_128_SHA
                     |TLS_KRB5_WITH_RC4_128_MD5
                     |TLS_KRB5_EXPORT_WITH_RC4_40_SHA
                     |TLS_KRB5_EXPORT_WITH_RC4_40_MD5
                   """.stripMargin.split("\n").toSet

  val sha1Ciphers = """TLS_RSA_EXPORT_WITH_DES40_CBC_SHA
                      |TLS_ECDH_ECDSA_WITH_RC4_128_SHA
                      |TLS_ECDH_RSA_WITH_RC4_128_SHA
                      |TLS_ECDHE_ECDSA_WITH_RC4_128_SHA
                      |TLS_ECDHE_RSA_WITH_RC4_128_SHA
                      |TLS_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA
                      |TLS_DHE_DSS_WITH_DES_CBC_SHA
                      |TLS_DHE_DSS_WITH_3DES_EDE_CBC_SHA
                      |TLS_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA
                      |TLS_DHE_RSA_WITH_DES_CBC_SHA
                      |TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA
                      |TLS_DHE_DSS_WITH_AES_128_CBC_SHA
                      |TLS_DHE_RSA_WITH_AES_128_CBC_SHA
                      |TLS_DHE_DSS_WITH_AES_256_CBC_SHA
                      |TLS_DHE_RSA_WITH_AES_256_CBC_SHA
                      |TLS_DH_anon_WITH_AES_256_CBC_SHA
                      |SSL_RSA_WITH_RC4_128_SHA
                    """.stripMargin.split("\n").toSet

  // The goal for deprecated ciphers is to stop known terrible ciphers rather than weak ones --
  // HMAC-MD5 and SHA1 ciphers are not included in this list because they are not known to be attacked,
  // and RC4 is not included because the attack is extremely expensive and RC4 is very widely deployed.
  // https://news.ycombinator.com/item?id=6548545
  // In any case, people who know what they want will use Suite B.
  val deprecatedCiphers = desCiphers ++ nullCiphers ++ anonCiphers ++ exportCiphers

  // The canonical list is here: https://www.iana.org/assignments/tls-parameters/tls-parameters.xhtml#tls-parameters-4
  // The JDK 1.6 list is here: http://docs.oracle.com/javase/6/docs/technotes/guides/security/SunProviders.html

  // 1.6.0 allCipherSuites:
  /*
    SSL_RSA_WITH_RC4_128_MD5
    SSL_RSA_WITH_RC4_128_SHA
    TLS_RSA_WITH_AES_128_CBC_SHA
    TLS_RSA_WITH_AES_256_CBC_SHA
    TLS_DHE_RSA_WITH_AES_128_CBC_SHA
    TLS_DHE_RSA_WITH_AES_256_CBC_SHA
    TLS_DHE_DSS_WITH_AES_128_CBC_SHA
    TLS_DHE_DSS_WITH_AES_256_CBC_SHA
    SSL_RSA_WITH_3DES_EDE_CBC_SHA
    SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA
    SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA
    SSL_RSA_WITH_DES_CBC_SHA
    SSL_DHE_RSA_WITH_DES_CBC_SHA
    SSL_DHE_DSS_WITH_DES_CBC_SHA
    SSL_RSA_EXPORT_WITH_RC4_40_MD5
    SSL_RSA_EXPORT_WITH_DES40_CBC_SHA
    SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA
    SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA
    TLS_EMPTY_RENEGOTIATION_INFO_SCSV
    SSL_RSA_WITH_NULL_MD5
    SSL_RSA_WITH_NULL_SHA
    SSL_DH_anon_WITH_RC4_128_MD5
    TLS_DH_anon_WITH_AES_128_CBC_SHA
    TLS_DH_anon_WITH_AES_256_CBC_SHA
    SSL_DH_anon_WITH_3DES_EDE_CBC_SHA
    SSL_DH_anon_WITH_DES_CBC_SHA
    SSL_DH_anon_EXPORT_WITH_RC4_40_MD5
    SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA
    TLS_KRB5_WITH_RC4_128_SHA
    TLS_KRB5_WITH_RC4_128_MD5
    TLS_KRB5_WITH_3DES_EDE_CBC_SHA
    TLS_KRB5_WITH_3DES_EDE_CBC_MD5
    TLS_KRB5_WITH_DES_CBC_SHA
    TLS_KRB5_WITH_DES_CBC_MD5
    TLS_KRB5_EXPORT_WITH_RC4_40_SHA
    TLS_KRB5_EXPORT_WITH_RC4_40_MD5
    TLS_KRB5_EXPORT_WITH_DES_CBC_40_SHA
    TLS_KRB5_EXPORT_WITH_DES_CBC_40_MD5
   */

  // 1.7.0_45 sslContext.getSupportedSSLParameters.getCipherSuites():
  /*
      TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256
      TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256
      TLS_RSA_WITH_AES_128_CBC_SHA256
      TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256
      TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256
      TLS_DHE_RSA_WITH_AES_128_CBC_SHA256
      TLS_DHE_DSS_WITH_AES_128_CBC_SHA256
      TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA
      TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA
      TLS_RSA_WITH_AES_128_CBC_SHA
      TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA
      TLS_ECDH_RSA_WITH_AES_128_CBC_SHA
      TLS_DHE_RSA_WITH_AES_128_CBC_SHA
      TLS_DHE_DSS_WITH_AES_128_CBC_SHA
      TLS_ECDHE_ECDSA_WITH_RC4_128_SHA
      TLS_ECDHE_RSA_WITH_RC4_128_SHA
      SSL_RSA_WITH_RC4_128_SHA
      TLS_ECDH_ECDSA_WITH_RC4_128_SHA
      TLS_ECDH_RSA_WITH_RC4_128_SHA
      TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA
      TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA
      SSL_RSA_WITH_3DES_EDE_CBC_SHA
      TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA
      TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA
      SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA
      SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA
      SSL_RSA_WITH_RC4_128_MD5
      TLS_EMPTY_RENEGOTIATION_INFO_SCSV
      TLS_DH_anon_WITH_AES_128_CBC_SHA256
      TLS_ECDH_anon_WITH_AES_128_CBC_SHA
      TLS_DH_anon_WITH_AES_128_CBC_SHA
      TLS_ECDH_anon_WITH_RC4_128_SHA
      SSL_DH_anon_WITH_RC4_128_MD5
      TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA
      SSL_DH_anon_WITH_3DES_EDE_CBC_SHA
      TLS_RSA_WITH_NULL_SHA256
      TLS_ECDHE_ECDSA_WITH_NULL_SHA
      TLS_ECDHE_RSA_WITH_NULL_SHA
      SSL_RSA_WITH_NULL_SHA
      TLS_ECDH_ECDSA_WITH_NULL_SHA
      TLS_ECDH_RSA_WITH_NULL_SHA
      TLS_ECDH_anon_WITH_NULL_SHA
      SSL_RSA_WITH_NULL_MD5
      SSL_RSA_WITH_DES_CBC_SHA
      SSL_DHE_RSA_WITH_DES_CBC_SHA
      SSL_DHE_DSS_WITH_DES_CBC_SHA
      SSL_DH_anon_WITH_DES_CBC_SHA
      SSL_RSA_EXPORT_WITH_RC4_40_MD5
      SSL_DH_anon_EXPORT_WITH_RC4_40_MD5
      SSL_RSA_EXPORT_WITH_DES40_CBC_SHA
      SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA
      SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA
      SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA
      TLS_KRB5_WITH_RC4_128_SHA
      TLS_KRB5_WITH_RC4_128_MD5
      TLS_KRB5_WITH_3DES_EDE_CBC_SHA
      TLS_KRB5_WITH_3DES_EDE_CBC_MD5
      TLS_KRB5_WITH_DES_CBC_SHA
      TLS_KRB5_WITH_DES_CBC_MD5
      TLS_KRB5_EXPORT_WITH_RC4_40_SHA
      TLS_KRB5_EXPORT_WITH_RC4_40_MD5
      TLS_KRB5_EXPORT_WITH_DES_CBC_40_SHA
      TLS_KRB5_EXPORT_WITH_DES_CBC_40_MD5
     */

  // 1.7.0_45 sslContext.getDefaultParameters().getCipherSuites()
  /*
  TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256
  TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256
  TLS_RSA_WITH_AES_128_CBC_SHA256
  TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256
  TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256
  TLS_DHE_RSA_WITH_AES_128_CBC_SHA256
  TLS_DHE_DSS_WITH_AES_128_CBC_SHA256
  TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA
  TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA
  TLS_RSA_WITH_AES_128_CBC_SHA
  TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA
  TLS_ECDH_RSA_WITH_AES_128_CBC_SHA
  TLS_DHE_RSA_WITH_AES_128_CBC_SHA
  TLS_DHE_DSS_WITH_AES_128_CBC_SHA
  TLS_ECDHE_ECDSA_WITH_RC4_128_SHA
  TLS_ECDHE_RSA_WITH_RC4_128_SHA
  SSL_RSA_WITH_RC4_128_SHA
  TLS_ECDH_ECDSA_WITH_RC4_128_SHA
  TLS_ECDH_RSA_WITH_RC4_128_SHA
  TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA
  TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA
  SSL_RSA_WITH_3DES_EDE_CBC_SHA
  TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA
  TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA
  SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA
  SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA
  SSL_RSA_WITH_RC4_128_MD5
  TLS_EMPTY_RENEGOTIATION_INFO_SCSV
   */

  // 1.6.0 enabledCiphers:
  /*
    SSL_RSA_WITH_RC4_128_MD5
    SSL_RSA_WITH_RC4_128_SHA
    TLS_RSA_WITH_AES_128_CBC_SHA
    TLS_RSA_WITH_AES_256_CBC_SHA
    TLS_DHE_RSA_WITH_AES_128_CBC_SHA
    TLS_DHE_RSA_WITH_AES_256_CBC_SHA
    TLS_DHE_DSS_WITH_AES_128_CBC_SHA
    TLS_DHE_DSS_WITH_AES_256_CBC_SHA
    SSL_RSA_WITH_3DES_EDE_CBC_SHA
    SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA
    SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA
    SSL_RSA_WITH_DES_CBC_SHA
    SSL_DHE_RSA_WITH_DES_CBC_SHA
    SSL_DHE_DSS_WITH_DES_CBC_SHA
    SSL_RSA_EXPORT_WITH_RC4_40_MD5
    SSL_RSA_EXPORT_WITH_DES40_CBC_SHA
    SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA
    SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA
    TLS_EMPTY_RENEGOTIATION_INFO_SCSV
   */

}
