<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Configuring WS SSL

[[Play WS|ScalaWS]] allows you to set up HTTPS completely from a configuration file, without the need to write code.  It does this by layering the Java Secure Socket Extension (JSSE) with a configuration layer and with reasonable defaults.

JDK 1.8 contains an implementation of JSSE which is [significantly more advanced](https://docs.oracle.com/javase/8/docs/technotes/guides/security/enhancements-8.html) than previous versions, and should be used if security is a priority.

> **NOTE**: It is highly recommended (if not required) to use WS SSL with the
unlimited strength java cryptography extension.  You can download the policy files from Oracle's website at [Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files 8 Download](https://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html).

## Table of Contents

The Play WS configuration is based on [Typesafe SSLConfig](https://lightbend.github.io/ssl-config).  

For convenience, a table of contents to SSLConfig is provided:

- [Quick Start to WS SSL](https://lightbend.github.io/ssl-config/WSQuickStart.html)
- [Generating X.509 Certificates](https://lightbend.github.io/ssl-config/CertificateGeneration.html)
- [Configuring Trust Stores and Key Stores](https://lightbend.github.io/ssl-config/KeyStores.html)
- [Configuring Protocols](https://lightbend.github.io/ssl-config/Protocols.html)
- [Configuring Cipher Suites](https://lightbend.github.io/ssl-config/CipherSuites.html)
- [Configuring Certificate Validation](https://lightbend.github.io/ssl-config/CertificateValidation.html)
- [Configuring Certificate Revocation](https://lightbend.github.io/ssl-config/CertificateRevocation.html)
- [Configuring Hostname Verification](https://lightbend.github.io/ssl-config/HostnameVerification.html)
- [Example Configurations](https://lightbend.github.io/ssl-config/ExampleSSLConfig.html)
- [Using the Default SSLContext](https://lightbend.github.io/ssl-config/DefaultContext.html)
- [Debugging SSL Connections](https://lightbend.github.io/ssl-config/DebuggingSSL.html)
- [Loose Options](https://lightbend.github.io/ssl-config/LooseSSL.html)
- [Testing SSL](https://lightbend.github.io/ssl-config/TestingSSL.html)

> **NOTE**: The links below are relative to `Typesafe SSLConfig`, which uses the `ssl-config` as a prefix for ssl properties.<br>
> Play uses the `play.ws.ssl` prefix, so that, for instance the `ssl-config.loose.acceptAnyCertificate` becomes `play.ws.ssl.loose.acceptAnyCertificate` for your play `WSClient` configuration.

## Further Reading

JSSE is a complex product.  For convenience, the JSSE materials are provided here:

JDK 1.8:

* [JSSE Reference Guide](https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html)
* [JSSE Crypto Spec](https://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html#SSLTLS)
* [SunJSSE Providers](https://docs.oracle.com/javase/8/docs/technotes/guides/security/SunProviders.html#SunJSSEProvider)
* [PKI Programmer's Guide](https://docs.oracle.com/javase/8/docs/technotes/guides/security/certpath/CertPathProgGuide.html)
* [keytool](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/keytool.html)
