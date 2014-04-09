<!--- Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com> -->
# Configuring Cipher Suites

A [cipher suite](https://en.wikipedia.org/wiki/Cipher_suite) is really four different ciphers in one, describing the key exchange, bulk encryption, message authentication and random number function. In this particular case, we’re focusing on the bulk encryption cipher.

## Configuring Enabled Ciphers

The list of cipher suites has changed considerably between 1.6 and 1.7.

In 1.7 and 1.8, the default [out of the box](http://sim.ivi.co/2011/07/jsse-oracle-provider-preference-of-tls.html) cipher suite list is used.

In 1.6, the out of the box list is [out of order](http://op-co.de/blog/posts/android_ssl_downgrade/), with some weaker cipher suites configured in front of stronger ones.  As such, the default list of enabled cipher suites is as follows:

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

This can be useful to enable perfect forward security, for example, as only DHE and ECDHE cipher suites enable PFE.

## Recommendation: increase the DHE key size

Diffie Hellman has been in the news recently because it offers perfect forward secrecy.  However, in 1.6 and 1.7, the server handshake of DHE is set to 1024 at most.  This is currently considered weak.

If you have JDK 1.8, setting the system property `-Djdk.tls.ephemeralDHKeySize=2048` is recommended to ensure stronger keysize in the handshake.  Please see [Customizing Size of Ephemeral Diffie-Hellman Keys](http://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#customizing_dh_keys).

## Disabling Weak Ciphers Globally

The `jdk.tls.disabledAlgorithms` can be used to prevent weak ciphers, and can also be used to prevent small key sizes from being used in a handshake.  This is a [useful feature](http://sim.ivi.co/2013/11/harness-ssl-and-jsse-key-size-control.html) that is only available in JDK 1.7 and later.

  // http://marc.info/?l=openjdk-security-dev&m=138932097003284&w=2
  //
  //

The official documentation for disabled algorithms is [here](http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#DisabledAlgorithms).

The parameter names to use for the disabled algorithms are not obvious, but are listed in the [Providers documentation](http://docs.oracle.com/javase/7/docs/technotes/guides/security/SunProviders.html).

For X.509 certificates, the public key algorithms used in signatures can be RSA, DSA or EC (listed as "ECDSA"):

```
jdk.certpath.disabledAlgorithms="RSA keySize < 2048, DSA keySize < 2048, EC keySize < 224"
```

Based off https://www.keylength.com and [mailing list discussion](http://openjdk.5641.n7.nabble.com/Code-Review-Request-7109274-Consider-disabling-support-for-X-509-certificates-with-RSA-keys-less-thas-td107890.html).

The digest algorithms used in signatures can be NONE, MD2, MD4, MD5, SHA1, SHA256, SHA512, SHA384

```
jdk.certpath.disabledAlgorithms="MD2, MD4, MD5, RSA keySize < 2048, DSA keySize < 2048, EC keySize < 224"
```

> NOTE: there are some cases where servers will send a root CA certificate which is self-signed as MD5.  According to RFC 2246, section 7.4.2, "the self-signed certificate which specifies the root certificate authority may optionally be omitted from the chain, under the assumption that the remote end must already possess it in order to validate it in any case", so it is technically possible to have a valid certificate chain fail because it contains a harmless MD5 root certificate.

For TLS, the code will match the first part of the cipher suite after the protocol, i.e. TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384 has ECDHE as the relevant cipher, giving "DHE, ECDH, ECDHE, RSA":

```
jdk.tls.disabledAlgorithms="DHE keySize < 2048, ECDH keySize < 2048, ECDHE keySize < 2048, RSA keySize < 2048"
```

This property is not set in WS SSL by default, as it is global to the entire JVM, but it is recommended.  Note that if you set `DHE keySize < 2048`, you will
also want to set `jdk.tls.ephemeralDHKeySize=2048` (and be running JDK 1.8).

## Using Weak Ciphers

There are some ciphers which are known to have flaws, and are [disabled](http://sim.ivi.co/2011/08/jsse-oracle-provider-default-disabled.html) in 1.7.  WS will throw an exception if a weak cipher is found in the `ws.ssl.enabledCiphers` list.  If you specifically want a weak cipher, set this flag:

```
ws.ssl.loose.acceptWeakCipher=true
```