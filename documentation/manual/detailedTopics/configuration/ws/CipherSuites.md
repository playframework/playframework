<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Configuring Cipher Suites

A [cipher suite](https://en.wikipedia.org/wiki/Cipher_suite) is really four different ciphers in one, describing the key exchange, bulk encryption, message authentication and random number function.  There is [no official naming convention](http://utcc.utoronto.ca/~cks/space/blog/tech/SSLCipherNames) of cipher suites, but most cipher suites are described in order -- for example, "TLS_DHE_RSA_WITH_AES_256_CBC_SHA" uses DHE for key exchange, RSA for server certificate authentication, 256-bit key AES in CBC mode for the stream cipher, and SHA for the message authentication.

## Configuring Enabled Ciphers

The list of cipher suites has changed considerably between 1.6, 1.7 and 1.8.

In 1.7 and 1.8, the default [out of the box](http://sim.ivi.co/2011/07/jsse-oracle-provider-preference-of-tls.html) cipher suite list is used.

In 1.6, the out of the box list is [out of order](http://op-co.de/blog/posts/android_ssl_downgrade/), with some weaker cipher suites configured in front of stronger ones, and contains a number of ciphers that are now considered weak.  As such, the default list of enabled cipher suites is as follows:

```
  "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
  "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
  "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
  "TLS_RSA_WITH_AES_256_CBC_SHA",
  "TLS_RSA_WITH_AES_128_CBC_SHA",
  "SSL_RSA_WITH_RC4_128_SHA",
  "SSL_RSA_WITH_RC4_128_MD5",
  "TLS_EMPTY_RENEGOTIATION_INFO_SCSV" // per RFC 5746
```

The list of cipher suites can be configured manually using the `ws.ssl.enabledCiphers` setting:

```
play.ws.ssl.enabledCiphers = [
  "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256"
]
```

This can be useful to enable perfect forward security, for example, as only DHE and ECDHE cipher suites enable PFE.

## Recommendation: increase the DHE key size

Diffie Hellman has been in the news recently because it offers perfect forward secrecy.  However, in 1.6 and 1.7, the server handshake of DHE is set to 1024 at most, which is considered weak and can be compromised by attackers.

If you have JDK 1.8, setting the system property `-Djdk.tls.ephemeralDHKeySize=2048` is recommended to ensure stronger keysize in the handshake.  Please see [Customizing Size of Ephemeral Diffie-Hellman Keys](http://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#customizing_dh_keys).

## Recommendation: Use Ciphers with Perfect Forward Secrecy

As per the [Recommendations for Secure Use of TLS and DTLS](https://datatracker.ietf.org/doc/draft-ietf-uta-tls-bcp/), the following cipher suites are recommended:

```
play.ws.ssl.enabledCiphers = [
  "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
  "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
  "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
  "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
]
```

Some of these ciphers are only available in JDK 1.8.

## Disabling Weak Ciphers and Weak Key Sizes Globally

The `jdk.tls.disabledAlgorithms` can be used to prevent weak ciphers, and can also be used to prevent [small key sizes](http://sim.ivi.co/2011/07/java-se-7-release-security-enhancements.html) from being used in a handshake.  This is a [useful feature](http://sim.ivi.co/2013/11/harness-ssl-and-jsse-key-size-control.html) that is only available in Oracle JDK 1.7 and later.

The official documentation for disabled algorithms is in the [JSSE Reference Guide](http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#DisabledAlgorithms).

For TLS, the code will match the first part of the cipher suite after the protocol, i.e. TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384 has ECDHE as the relevant cipher.  The parameter names to use for the disabled algorithms are not obvious, but are listed in the [Providers documentation](http://docs.oracle.com/javase/7/docs/technotes/guides/security/SunProviders.html) and can be seen in the [source code](http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/7u40-b43/sun/security/ssl/SSLAlgorithmConstraints.java#265).

To enable `jdk.tls.disabledAlgorithms` or `jdk.certpath.disabledAlgorithms` (which looks at signature algorithms and weak keys in X.509 certificates) you must create a properties file:

```
# disabledAlgorithms.properties
jdk.tls.disabledAlgorithms=EC keySize < 160, RSA keySize < 2048, DSA keySize < 2048
jdk.certpath.disabledAlgorithms=MD2, MD4, MD5, EC keySize < 160, RSA keySize < 2048, DSA keySize < 2048
```

And then start up the JVM with [java.security.properties](http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7133344):

```
java -Djava.security.properties=disabledAlgorithms.properties
```

## Debugging

To debug ciphers and weak keys, turn on the following debug settings:

```
play.ws.ssl.debug = {
 ssl = true
 handshake = true
 verbose = true
 data = true
}
```

> **Next**: [[Configuring Certificate Validation|CertificateValidation]]
