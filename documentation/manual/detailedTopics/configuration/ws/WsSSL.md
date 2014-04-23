<!--- Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com> -->
# Configuring WS SSL

[[Play WS|ScalaWS]] allows you to set up HTTPS completely from a configuration file, without the need to write code.  It does this by layering the Java Secure Socket Extension (JSSE) with a configuration layer and with reasonable defaults.

JDK 1.8 contains an implementation of JSSE which is [significantly more advanced](http://docs.oracle.com/javase/8/docs/technotes/guides/security/enhancements-8.html) than previous versions, and should be used if security is a priority.

## Table of Contents

- [[Generating X.509 Certificates|CertificateGeneration]]
- [[Configuring Trust Stores and Key Stores|KeyStores]]
- [[Configuring Protocols|Protocols]]
- [[Configuring Cipher Suites|CipherSuites]]
- [[Debugging SSL Connections|DebuggingSSL]]
- [[Configuring Certificate Validation|CertificateValidation]]
- [[Configuring Certificate Revocation|CertificateRevocation]]
- [[Configuring Hostname Verification|HostnameVerification]]
- [[Example Configurations|ExampleSSLConfig]]
- [[Using the Default SSLContext|DefaultContext]]
- [[Loose Options|LooseSSL]]

## Further Reading

JSSE is a complex product.  For convenience, the JSSE materials are provided here:

JDK 1.8:

* [JSSE Reference Guide](http://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html)
* [JSSE Crypto Spec](http://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html#SSLTLS)
* [SunJSSE Providers](http://docs.oracle.com/javase/8/docs/technotes/guides/security/SunProviders.html#SunJSSEProvider)
* [PKI Programmer's Guide](http://docs.oracle.com/javase/8/docs/technotes/guides/security/certpath/CertPathProgGuide.html)

JDK 1.7:

* [JSSE Reference Guide](http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html)
* [JSSE Crypto Spec](http://docs.oracle.com/javase/7/docs/technotes/guides/security/crypto/CryptoSpec.html#SSLTLS)
* [SunJSSE Providers](http://docs.oracle.com/javase/7/docs/technotes/guides/security/SunProviders.html#SunJSSEProvider)
* [PKI Programmer's Guide](http://docs.oracle.com/javase/7/docs/technotes/guides/security/certpath/CertPathProgGuide.html)

JDK 1.6:

* [JSSE Reference Guide](http://docs.oracle.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html)
* [JSSE Crypto Spec](http://docs.oracle.com/javase/6/docs/technotes/guides/security/crypto/CryptoSpec.html#SSLTLS)
* [SunJSSE Providers](http://docs.oracle.com/javase/6/docs/technotes/guides/security/SunProviders.html#SunJSSEProvider)
* [PKI Programmer's Guide](http://docs.oracle.com/javase/6/docs/technotes/guides/security/certpath/CertPathProgGuide.html)

> **Next:** [[Generating X.509 Certificates|CertificateGeneration]]
