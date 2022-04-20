<!--- Copyright (C) Lightbend Inc. <https://www.lightbend.com> -->

# Configuring HTTPS

Play can be configured to serve HTTPS.  To enable this, simply tell Play which port to listen to using the `https.port` system property.  For example:

```bash
./bin/your-app -Dhttps.port=9443
```

## Providing configuration

HTTPS configuration can either be supplied using system properties or in `application.conf`. For more details see the [[configuration|Configuration]] and [[production configuration|ProductionConfiguration]] pages.

## SSL Certificates

### SSL Certificates from a keystore

By default, Play will generate itself a self-signed certificate, however typically this will not be suitable for serving a website.  Play uses Java key stores to configure SSL certificates and keys.

Signing authorities often provide instructions on how to create a Java keystore (typically with reference to Tomcat configuration).  The official Oracle documentation on how to generate keystores using the JDK keytool utility can be found [here](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/keytool.html).  There is also an example in the [Generating X.509 Certificates](https://lightbend.github.io/ssl-config/CertificateGeneration.html) section.

Having created your keystore, the following configuration properties can be used to configure Play to use it:

* **play.server.https.keyStore.path** - The path to the keystore containing the private key and certificate, if not provided generates a keystore for you
* **play.server.https.keyStore.type** - The key store type, defaults to `JKS`
* **play.server.https.keyStore.password** - The password, defaults to a blank password
* **play.server.https.keyStore.algorithm** - The key store algorithm, defaults to the platforms default algorithm

### SSL Certificates from a custom SSL Engine

Another alternative to configure the SSL certificates is to provide a custom [SSLEngine](https://docs.oracle.com/javase/8/docs/api/javax/net/ssl/SSLEngine.html).  This is also useful in cases where a customized `SSLEngine` is required, such as in the case of client authentication. In Java, an implementation must be provided for [`play.server.SSLEngineProvider`](api/java/play/server/SSLEngineProvider.html) and in Scala, an implementation must be provided for [`play.server.api.SSLEngineProvider`](api/scala/play/server/api/SSLEngineProvider.html). For example:

Scala
: @[scalaexample](code/scalaguide/CustomSSLEngineProvider.scala)

Java
: @[javaexample](code/javaguide/CustomSSLEngineProvider.java)

Having created an implementation for `play.server.SSLEngineProvider` or `play.server.api.SSLEngineProvider`, the following system property configures Play to use it:

* **`play.server.https.engineProvider`** - The path to the class implementing `play.server.SSLEngineProvider` or `play.server.api.SSLEngineProvider`:

Example:

```bash
./bin/your-app -Dhttps.port=9443 -Dplay.server.https.engineProvider=mypackage.CustomSSLEngineProvider
```

## Turning HTTP off

To disable binding on the HTTP port, set the `http.port` system property to be `disabled`, eg:

```bash
./bin/your-app -Dhttp.port=disabled -Dhttps.port=9443 -Dplay.server.https.keyStore.path=/path/to/keystore -Dplay.server.https.keyStore.password=changeme
```

## Production usage of HTTPS

If Play is serving HTTPS in production, it should be running JDK 1.8.  JDK 1.8 provides a number of new features that make JSSE feasible as a [TLS termination layer](https://blog.ivanristic.com/2014/03/ssl-tls-improvements-in-java-8.html).  If not using JDK 1.8, using a [[reverse proxy|HTTPServer]] in front of Play will give better control and security of HTTPS.

If you intend to use Play for TLS termination layer, please note the following settings:

* **[`SSLParameters.setUseCipherSuiteorder()`](https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#cipher_suite_preference)** - Reorders cipher suite order to the server's preference.
* **-Djdk.tls.ephemeralDHKeySize=2048** - Increases the key size in a DH key exchange.
* **-Djdk.tls.rejectClientInitiatedRenegotiation=true** - Rejects client renegotiation.
