<!--- Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com> -->
# Configuring Cipher Suites

A [cipher suite](https://en.wikipedia.org/wiki/Cipher_suite) is really four different ciphers in one, describing the key exchange, bulk encryption, message authentication and random number function. In this particular case, we’re focusing on the bulk encryption cipher.

## Configuring Enabled Ciphers

The list of cipher suites has changed considerably between 1.6 and 1.7.

In 1.7 and 1.8, the default [out of the box](http://sim.ivi.co/2011/07/jsse-oracle-provider-preference-of-tls.html) cipher suite list is used.

In 1.6, the out of the box list is [out of order](http://op-co.de/blog/posts/android_ssl_downgrade/), with some weaker cipher suites configured in front of stronger ones.  As such, the default list of enabled ciphers is as follows:

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

The list of cipher suites can be configured manually using the `ws.ssl.enabledCiphers` flag:

```
ws.ssl.enabledCiphers = [
  "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384"
]
```

The above settings will give JDK 1.8 Suite B cryptography according to [RFC 6460](https://tools.ietf.org/html/rfc6460)

## Recommendation: increase the DHE key size

Diffie Hellman has been in the news recently because it offers perfect forward secrecy.  However, in 1.6 and 1.7, the server handshake of DHE is set to 1024 at most.  This is considered weak.

If you have JDK 1.8, setting the system property `-Djdk.tls.ephemeralDHKeySize=2048` is recommended to ensure stronger keysize in the handshake.  Please see [Customizing Size of Ephemeral Diffie-Hellman Keys](http://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#customizing_dh_keys).

## Disabling Weak Ciphers Globally

The `jdk.tls.disabledAlgorithms` can be used to prevent weak ciphers, and can also be used to prevent small key sizes from being used in a handshake.  This is a [useful feature](http://sim.ivi.co/2013/11/harness-ssl-and-jsse-key-size-control.html) that is only available in JDK 1.7 and later.

The parameter names to use for the disabled algorithms are not obvious, but are listed the [Providers documentation](http://docs.oracle.com/javase/7/docs/technotes/guides/security/SunProviders.html).


```
-Djdk.tls.disabledAlgorithms="DHE keySize < 2048, ECDH keySize < 2048, ECDHE keySize < 2048, RSA keySize < 2048, DSA keySize < 2048, EC keySize < 224"
```

This property is not set in WS SSL by default, as it is global to the entire JVM, but it is recommended.

## Using Weak Ciphers

There are some cipher suites which are known to have flaws, and are [disabled](http://sim.ivi.co/2011/08/jsse-oracle-provider-default-disabled.html) in 1.7.  They will be filtered out of the `ws.ssl.enabledCiphers` list.  If you wish to disable this filter, set this flag:

```
ws.ssl.loose.acceptWeakCipher=true
```