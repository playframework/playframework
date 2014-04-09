<!--- Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com> -->
# Configuring Certificate Validation

In an SSL connection, the identity of the remote server is verified using an X.509 certificate which has been signed by a certificate authority.

The JSSE implementation of X.509 certificates is defined in the [PKI Programmer's Guide](http://docs.oracle.com/javase/7/docs/technotes/guides/security/certpath/CertPathProgGuide.html).

Some X.509 certificates that are used by servers are old, and are using signatures that can be forged by an attacker.  Because of this, it may not be possible to verify the identity of the server if that signature algorithm is being used.  Fortunately, this is rare -- over 95% of trusted leaf certificates and 95% of trusted signing certificates use [NIST recommended key sizes](http://csrc.nist.gov/publications/nistpubs/800-131A/sp800-131A.pdf).

WS automatically disables weak signature algorithms and weak keys for you, according to the [current standards](http://sim.ivi.co/2012/04/nist-security-strength-time-frames.html).

> NOTE: This feature is very similar to [jdk.certpath.disabledAlgorithms](http://sim.ivi.co/2013/11/harness-ssl-and-jsse-key-size-control.html), but is specific to the WS client and can be set dynamically, whereas jdk.certpath.disabledAlgorithms is global across the JVM, must be set via a security property, and is only available in JDK 1.7 and later.

You can override this to your tastes, but it is recommended to be at least as strict as the defaults.  The appropriate signature names can be looked up in the [Providers Documentation](http://docs.oracle.com/javase/7/docs/technotes/guides/security/SunProviders.html).

## Disabling Signature Algorithms

The default list of disabled signature algorithms is defined below:

```
ws.ssl.disabledSignatureAlgorithms = "MD2, MD4, MD5"
```

## Disabling Weak Keys

WS defines the default list of weak keys as follows:

```
ws.ssl.disabledKeyAlgorithms = "DHE keySize < 2048, ECDH keySize < 2048, ECDHE keySize < 2048, RSA keySize < 2048, DSA keySize < 2048, EC keySize < 224"
```
