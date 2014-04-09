<!--- Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com> -->
# Configuring WS SSL

[[Play WS|ScalaWS]] comes with an SSL (also known as TLS) client implementation that manages JSSE, the Java Secure Socket implementation provided by Java.

WS SSL allows for full configuration of an SSL context within a configuration file.

The defaults in WS are defined to be reasonable for a new implementation.

JDK 1.8 contains an implementation of JSSE which is [significantly more advanced](http://docs.oracle.com/javase/8/docs/technotes/guides/security/enhancements-8.html) than previous versions, and should be used if security is a priority.

## Table of Contents

- [[Configuring Trust Stores and Key Stores|KeyStores]]
- [[Configuring Protocols|Protocols]]
- [[Configuring Cipher Suites|CipherSuites]]
- [[Debugging SSL Connections|DebuggingSSL]]
- [[Configuring Certificate Validation|CertificateValidation]]
- [[Configuring Certificate Revocation|CertificateRevocation]]
- [[Configuring Hostname Verification|HostnameVerification]]

## Using the default SSLContext

If you don't want to use the SSLContext that WS provides for you, and want to use `SSLContext.getDefault`, please set:

```
ws.ssl.default = true
```
