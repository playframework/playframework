<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Configuring WS SSL

[[Play WS|ScalaWS]] allows you to set up HTTPS completely from a configuration file, without the need to write code.  It does this by layering the Java Secure Socket Extension (JSSE) with a configuration layer and with reasonable defaults.

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

JDK 11:

* [JSSE Reference Guide](https://docs.oracle.com/en/java/javase/11/security/java-secure-socket-extension-jsse-reference-guide.html)
* [JSSE Crypto Spec](https://docs.oracle.com/en/java/javase/11/security/java-cryptography-architecture-jca-reference-guide.html#GUID-C9C9DD6C-3A6B-4759-B41E-AAAC502C0229)
* [SunJSSE Providers](https://docs.oracle.com/en/java/javase/11/security/oracle-providers.html#GUID-7093246A-31A3-4304-AC5F-5FB6400405E2)
* [PKI Programmer's Guide](https://docs.oracle.com/en/java/javase/11/security/java-pki-programmers-guide.html)
* [keytool](https://docs.oracle.com/en/java/javase/11/tools/keytool.html)
