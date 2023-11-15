<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Play 2.9 Migration Guide

* This guide is for migrating from Play 2.8 to Play 2.9. See the [[Play 2.8 Migration Guide|Migration28]] to upgrade from Play 2.7.
* If you have completed this migration guide and want to proceed with migrating your application to [Play 3.0](https://www.playframework.com/documentation/latest/Highlights30) (built on Pekko and Pekko HTTP), please refer to the [Play 3.0 Migration Guide](https://www.playframework.com/documentation/latest/Migration30) for further instructions.

## How to migrate

Before starting `sbt`, make sure to make the following upgrades.

### Play upgrade

Update the Play version number in `project/plugins.sbt`:

```scala
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.9.x")
```

Where the "x" in `2.9.x` is the minor version of Play you want to use, for instance `2.9.0`.
Check the release notes for Play's minor version [releases](https://github.com/playframework/playframework/releases).

### sbt upgrade

Play 2.9 only supports sbt 1.9 or newer. To update, change your `project/build.properties` so that it reads:

```properties
sbt.version=1.9.6
```

At the time of this writing `1.9.6` is the latest version in the sbt 1.x family, you may be able to use newer versions too. Check the release notes for sbt's [releases](https://github.com/sbt/sbt/releases) for details.

### Minimum required Java and sbt version

Play 2.9 dropped Java 8 support and requires at least Java 11. Other supported Java versions are 17 and 21. When upgrading to Play 2.9, we strongly recommend considering an upgrade to at least Java 17 LTS. It’s worth noting that in the forthcoming Play 2.x release, which will be version 2.10, we are likely to discontinue support for Java 11.

> Please be aware that binding an HTTPS port with a self-signed certificate in Java 17 and Java 21 may lead to issues. For more details on this matter, refer to [["Generation of Self-Signed Certificates Fails in Java 17 and Java 21"|Migration29#Generation-of-Self-Signed-Certificates-Fails-in-Java-17-and-Java-21]].

### Jetty ALPN Agent removed

Because Java 11 is the minimum required Java versions for Play 2.9 and supports ALPN out of the box, the [Jetty ALPN Agent](https://github.com/jetty-project/jetty-alpn-agent), is not strictly required anymore.
In case you passed a `-javaagent:jetty-alpn-agent-*.jar` flag to your Play application(s) (maybe via `SBT_OPTS` environment variable) you have to remove that flags now.

### Upgrading Akka and Akka HTTP

If you wish to upgrade beyond Akka 2.6 and Akka HTTP 10.2, you can do that with the assistance of our [[Play Scala|ScalaAkka#Updating-Akka-version]] or [[Play Java|JavaAkka#Updating-Akka-version]] update guides. We also strongly encourage you to review:

- [[How Play Deals with Akka’s License Change|General#How-Play-Deals-with-Akkas-License-Change]]

## API Changes

Play 2.9 contains multiple API changes. As usual, we follow our policy of deprecating existing APIs before removing them. This section details these changes.

### Scala 2.12 support discontinued

Play 2.9 supports Scala 2.13 and Scala 3.3, but not 2.12 anymore. Scala 3 requires some more migration steps which you can find in our [[Scala 3 migration guide|Scala3Migration]].

### Setting `scalaVersion` in your project

**Both Scala and Java users** must configure sbt to use a supported Scala version. Even if you have no Scala code in your project, Play itself uses Scala and must be configured to use the right Scala libraries.

To set the Scala version in sbt, simply set the `scalaVersion` key, for example:

```scala
scalaVersion := "2.13.12"
```

Play 2.9 also supports Scala 3:

```scala
scalaVersion := "3.3.1"
```

> It's important to emphasize that Play exclusively supports [Scala LTS (Long-Term Support)](https://www.scala-lang.org/blog/2022/08/17/long-term-compatibility-plans.html) versions. As a result, any Scala release between Scala 3.3 LTS and the subsequent LTS version will not be officially supported by Play. However, it might still be feasible to use Play with such Scala versions.

If you have a single project build, then this setting can just be placed on its own line in `build.sbt`.  However, if you have a multi-project build, then the scala version setting must be set on each project.  Typically, in a multi-project build, you will have some common settings shared by every project, this is the best place to put the setting, for example:

```scala
def commonSettings = Seq(
  scalaVersion := "2.13.12"
)

val projectA = (project in file("projectA"))
  .enablePlugins(PlayJava)
  .settings(commonSettings)

val projectB = (project in file("projectB"))
  .enablePlugins(PlayJava)
  .settings(commonSettings)
```

### Renaming towards more consistent method names

Some classes and methods where renamed to enhance consistency accross different APIs, especially methods for adding, removing and clearing data.
To enable a smooth migration, the older methods are now deprecated and will be removed in a future version.

Methods renamed in trait [`play.api.i18n.MessagesApi`](api/scala/play/api/i18n/MessagesApi.html)

| **Deprecated method**                         | **New method**
| ----------------------------------------------|-------------------------------------------
| `clearLang(result: Result)`                   | `withoutLang(result: Result)`

Methods renamed in object [`play.api.mvc.Results`](api/scala/play/api/mvc/Results.html)

| **Deprecated method**                         | **New method**
| ----------------------------------------------|-------------------------------------------
| `clearingLang(result: Result)`                | `withoutLang(result: Result)`

Methods renamed in [`play.api.libs.typedmap.TypedMap`](api/scala/play/api/libs/typedmap/TypedMap.html):

| **Deprecated method** | **New method** |
|-----------------------|----------------|
| `+`                   | `updated`      |
| `-`                   | `removed`      |

Following classes have been renamed:

| **Deprecated class** | **New class**               |
|----------------------|-----------------------------|
| `HttpExecutionContext` | `ClassLoaderExecutionContext` |
| `HttpExecution`        | `ClassLoaderExecution`        |

### Deprecated APIs were removed

Many APIs that were deprecated in earlier versions were removed in Play 2.9. If you are still using them we recommend migrating to the new APIs before upgrading to Play 2.9. Check the Javadocs and Scaladocs for migration notes. See also the [[migration guide for Play 2.8|Migration28]] for more information.

### Changing CSP report types

There have been some changes in CSP reports, according to the [w3.org specification](https://www.w3.org/TR/CSP2/). Since Play 2.9 fields `lineNumber` and `columnNumber` are Longs. If your implementation bases on these fields being Strings, Play will fallback to parsing them from String to Long and throw an error only if parsing fails. It is, however, encouraged to use number types, not Strings, as this may be changed when CSP3 comes out.

### `Request.asJava` now always wraps bodies in `RequestBody`

When converting a Scala request to a Java request using the [`asJava`](api/scala/play/api/mvc/Request.html#asJava:play.mvc.Http.Request) method, the resulting Java request will now always have the body wrapped in a [`Http.RequestBody`](api/java/play/mvc/Http.RequestBody.html) class. This change ensures consistency because in Play Java, request bodies are always wrapped within this class.

Previously, this conversion was not performed cleanly, and it was possible for a converted Java request to directly contain the Scala request body without being wrapped in `Http.RequestBody`.

However, it's important to note that despite wrapping the request body in a `RequestBody` class, the body itself will not be automatically converted to its Java equivalent when converting a Scala request to a Java request. For example, if the Scala request contains a [`play.api.mvc.RawBuffer`](api/scala/play/api/mvc/RawBuffer.html), it will not be converted into its Java equivalent [`play.mvc.Http.RawBuffer`](api/java/play/mvc/Http.RawBuffer.html). Similarly, a Scala [`AnyContentAsEmpty`](api/scala/play/api/mvc/AnyContentAsEmpty$.html) will not be converted into a `java.util.Optional.empty()` (which is the Play Java equivalent of an empty body). Consequently, helper methods like `request.asJava.body().asRaw()`, `.asJson()`, etc. will likely not work as expected.

To retrieve any stored Play Scala body object, you can use `request.asJava.body().as(classOf[Object])`.

### Transition from Java Persistence API to Jakarta Persistence API

To finally support Hibernate ORM 6+ and EclipseLink 3+, Play needed to transition its [[JPA module|JavaJPA]] to the [Jakarta Persistence API](https://jakarta.ee/specifications/persistence/3.1/). To migrate your code, you must first upgrade Hibernate or EclipseLink in the `libraryDependencies` within your `build.sbt`. Then, adjust the imports in your Java files as follows:

From:

```java
import javax.persistence.*;
```

To:

```java
import jakarta.persistence.*;
```

Additionally, update your `persistence.xml` as shown below:

From:

```xml
<persistence xmlns="http://xmlns.jcp.org/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/persistence http://xmlns.jcp.org/xml/ns/persistence/persistence_2_1.xsd"
             version="2.1">
...
```

To:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<persistence xmlns="https://jakarta.ee/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence https://jakarta.ee/xml/ns/persistence/persistence_3_0.xsd"
             version="3.0">
...
```

Furthermore, update all configuration parameters by replacing the prefix `javax.persistence` with `jakarta.persistence`. For instance:

From:

```xml
<property name="javax.persistence.jdbc.driver" value="..."/>
```

To:

```xml
<property name="jakarta.persistence.jdbc.driver" value="..."/>
```

Additionally, we have received [reports](https://github.com/orgs/playframework/discussions/11985#discussioncomment-7379124) indicating that it may be necessary, if applicable, to change the provider in the configuration from:

```xml
<provider>org.hibernate.ejb.HibernatePersistence</provider>
```

to:

```xml
<provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
```

in order to ensure that Hibernate works properly with Play 2.9.

> Although `version="3.0"` and `persistence_3_0.xsd` are used, this XML declaration is correct for the [latest](https://jakarta.ee/specifications/persistence/3.1/) Jakarta Persistence 3.1. This is because in Jakarta Persistence 3.1, the `persistence.xml` schema remains unchanged. To avoid duplication, the 3.0 schema is reused for the version update: https://jakarta.ee/xml/ns/persistence/

#### JPA Bean Validation Not Currently Supported in Play 2.9

It's important to note that Bean Validation is currently not supported in Play 2.9. For more information, refer to the details provided [[here|JavaJPA#Bean-Validation-Not-Currently-Supported-in-Play-2.9]].

### JNotify watch service no longer available

The `JNotifyFileWatchService` was removed from [`play-file-watch`](https://github.com/playframework/play-file-watch), therefore

```sbt
play.dev.filewatch.FileWatchService.jnotify
```

is no longer available as a value for `PlayKeys.fileWatchService`.

### Removed some settings from `PlayKeys`

The settings `playOmnidoc`, `playDocsName`, `playDocsModule`, and `playDocsJar` have been removed from `PlayKeys`. These settings are no longer in use, as it has not been possible to run the Play documentation locally for quite some time.

### Namespace Change: `org.fluentlenium` to `io.fluentlenium`

FluentLenium has migrated its namespace from `org.fluentlenium` to `io.fluentlenium`. Therefore, you might need to update your imports accordingly.

## Configuration changes

This section lists changes and deprecations in configurations.

### Application Secret enforces a minimum length

The [[application secret|ApplicationSecret]] configuration `play.http.secret.key` now enforces a minimum length check in all modes (prod, dev and test). If the minimum length is not met, an error is thrown, rendering the configuration invalid and preventing the application from starting.

The minimum length depends on the algorithm used to sign the session or flash cookie, which can be set via the config keys `play.http.session.jwt.signatureAlgorithm` and `play.http.flash.jwt.signatureAlgorithm`. By default, both configs use the `HS256` algorithm, requiring a secret of at least 256 bits (32 bytes). For `HS384`, the minimum size is 384 bits (48 bytes), and for `HS512`, it's 512 bits (64 bytes).

When this error occurs, it provides detailed information for resolution:

```
Configuration error [
  The application secret is too short and does not have the recommended amount of entropy for algorithm HS256 defined at play.http.session.jwt.signatureAlgorithm.
  Current application secret bits: 248, minimal required bits for algorithm HS256: 256.
  To set the application secret, please read https://playframework.com/documentation/latest/ApplicationSecret
]
```

You can resolve this error by ensuring that the secret contains the required number of bits/bytes. For example, you can generate a secret with at least 32 bytes of completely random input using `head -c 32 /dev/urandom | base64`, or you can use the application secret generator with `playGenerateSecret` or `playUpdateSecret`.

### New Logback configuration format

Starting with version 1.3, Logback uses a new canonical format for its configuration files. Play has upgraded to the latest Logback version, so you should update your Logback config files to this new format. You can easily do this using the [online translator](https://logback.qos.ch/translator/) provided by the Logback team. Log in with your GitHub account, then copy and paste your existing Logback config for conversion.

Although the legacy config format will still work, it's recommended to upgrade now, as the process is straightforward.

Additionally, the Play specific `coloredLevel` converter has been deprecated. Logback [has provided built-in patterns for coloring](https://logback.qos.ch/manual/layouts.html#coloring) for a while. Therefore, remove the following line from your Logback config files:

```xml
<conversionRule conversionWord="coloredLevel" converterClass="play.api.libs.logback.ColoredLevel" />
```

Instead, replace `%coloredLevel` with `%highlight(%-5level)` in your patterns:

```xml
<!-- Deprecated: -->
<pattern>... %coloredLevel ...</pattern>

<!-- Recommended: -->
<pattern>... %highlight(%-5level) ...</pattern>
```

### Removed `play.akka.config` setting

When bootstrapping an actor system, Akka looks up its settings from a hardcoded `akka` prefix within the "root" configuration it is provided. This behavior is inherent to Akka and is not specific to Play.

By default, Play instructs Akka to load its actor system settings directly from the application configuration root path. Consequently, you typically configure Play's actor system in `application.conf` under the `akka.*` namespace.

Up until Play 2.9, you had the option to use the `play.akka.config` configuration to specify an alternative location for Play to retrieve its Akka settings. This was useful if you intended to utilize the `akka.*` settings for a separate Akka actor system. However, this configuration has been removed for two key reasons:

1. Lack of Documentation: The `play.akka.config` setting was not well-documented. The documentation failed to clarify that even if you set `play.akka.config = "my-akka"`, the `akka.*` settings from the configuration root path would still be loaded as a fallback. This meant that any changes made to the `akka.*` configuration would also impact the actor system defined in `my-akka.akka.*`. Consequently, these two actor systems did not operate independently, and the promise implied by `play.akka.config` (enabling the use of `akka.*` exclusively for another Akka actor system) was not fulfilled.

2. Configuration Expectations: Play anticipates that the actor system configuration will reside within the `akka.*` namespace and sets various configurations within that prefix to ensure seamless operation. If you were to employ an entirely different actor system for Play, your application would likely cease to function correctly.

Due to these considerations, starting from Play 2.9, the `akka.*` prefix is designated solely for Play's Akka configuration and cannot be altered. You can still implement your own actor systems, but it is imperative to ensure that they do not read their configuration from Play's `akka` prefix in the root path.

#### Dispatchers defined directly in the `akka.*` config won't load automatically anymore

As a side effect of removing the `play.akka.config` configuration, it is no longer possible to define dispatchers directly inside the `akka.*` config key, as they won't get loaded automatically anymore by the default actor system. Instead, you will run into an exception like:

```
play.api.http.HttpErrorHandlerExceptions$$anon$1: Execution exception[[ConfigurationException: Dispatcher [my-context] not configured]]
...
Caused by: akka.ConfigurationException: Dispatcher [my-context] not configured
...
```

You now have to:

- specify the `akka.*` prefix when looking up a dispatcher (e.g., `lookup("akka.my-context")`) or
- put the `my-context` dispatcher config directly in the root of your configuration or
- move the dispatchers to a different sub-key and look them up from there (e.g., in the config `contexts.my-context { ... }` and `lookup("contexts.my-context")`).

Actually, the behavior in Play 2.9 and newer is correct: A dispatcher is not supposed to be read from the `akka.*` key automatically. This wouldn't also work in a plain Akka application. Being able to put the dispatcher in the `akka.*` key was just a Play-specific side effect of the broken `play.akka.config` implementation.

## Defaults changes

Several default values used by Play have changed, which may impact your application. This section outlines these default changes.

### Generation of Self-Signed Certificates Fails in Java 17 and Java 21

In the process of binding an HTTPS port, Play typically generates a self-signed certificate as a default procedure. However, when using Java 17, this operation may lead to the following exception:

```
 java.lang.IllegalAccessError: class com.typesafe.sslconfig.ssl.FakeKeyStore$ (in unnamed module @0x68c8dd0e) cannot access class sun.security.x509.X509CertInfo (in module java.base) because module java.base does not export sun.security.x509 to unnamed module @0x68c8dd0e (FakeKeyStore.scala:89)
com.typesafe.sslconfig.ssl.FakeKeyStore$.createSelfSignedCertificate(FakeKeyStore.scala:89)
com.typesafe.sslconfig.ssl.FakeKeyStore$.generateKeyStore(FakeKeyStore.scala:79)
play.core.server.SelfSigned$.x$1$lzycompute(SelfSigned.scala:24)
play.core.server.SelfSigned$.x$1(SelfSigned.scala:23)
play.core.server.SelfSigned$.sslContext$lzycompute(SelfSigned.scala:23)
play.core.server.SelfSigned$.sslContext(SelfSigned.scala:23)
play.core.server.SelfSignedSSLEngineProvider.sslContext(SelfSigned.scala:36)
...
```

To circumvent this exception within a standard Play application setup, the [[binding of the HTTPS port for tests has been disabled|Migration29#Test-servers-no-longer-bind-an-HTTPS-port-by-default]] as a default configuration. It is noteworthy that the unbinding of the HTTPS port in tests is a reversion to an older behavior, as HTTPS port binding was unintentionally activated by us after being disabled in earlier Play versions.

Should you desire to conduct tests against the HTTPS port while using a Play-generated self-signed certificate, a workaround is required for Java 17. It involves adding an `--add-export` flag to the Java 17 command line. Moreover, it is crucial to fork a new Java Virtual Machine (JVM) instance during test execution:

```sbt
Test / javaOptions += "--add-exports=java.base/sun.security.x509=ALL-UNNAMED"
// Test / fork := true // This is the default behavior in Play; a reminder in case the setting was changed to false previously
```

Alternatively, you can set the `JAVA_TOOL_OPTIONS` environment variable:

```sh
export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS --add-exports=java.base/sun.security.x509=ALL-UNNAMED";
```

Unfortunately, the aforementioned workaround is not sufficient for Java 21. In this case, you may encounter the following exception:

```sh
 java.lang.NoSuchMethodError: 'void sun.security.x509.X509CertInfo.set(java.lang.String, java.lang.Object)' (FakeKeyStore.scala:92)
com.typesafe.sslconfig.ssl.FakeKeyStore$.createSelfSignedCertificate(FakeKeyStore.scala:92)
com.typesafe.sslconfig.ssl.FakeKeyStore$.generateKeyStore(FakeKeyStore.scala:79)
play.core.server.SelfSigned$.x$1$lzycompute(SelfSigned.scala:24)
play.core.server.SelfSigned$.x$1(SelfSigned.scala:23)
play.core.server.SelfSigned$.sslContext$lzycompute(SelfSigned.scala:23)
play.core.server.SelfSigned$.sslContext(SelfSigned.scala:23)
play.core.server.SelfSignedSSLEngineProvider.sslContext(SelfSigned.scala:36)
...
```

As of now, it's not feasible to use Java 21 for self-signed certificate generation within Play. It is recommended to continue utilizing Java 17 for this purpose. You can monitor the resolution of this issue through the following [GitHub link](https://github.com/lightbend/ssl-config/issues/367).

Typically, self-signed certificates are not intended for serving websites. For more comprehensive HTTPS configuration information, please refer to [["Configuring HTTPS"|ConfiguringHttps]] or [["Using Play in production"|Production]].

### Changes in Handling Empty Bodies for the `AnyContent` Body Parser

If you employ the `AnyContent` body parser, such as `parse.anyContent` in Play Scala or `@BodyParser.Of(BodyParser.AnyContent.class)` in Play Java, it's important to note that when the body parsing process yields an empty body, the behavior has been updated. In Play Scala, the request body will now be assigned to `AnyContentAsEmpty`, and in Play Java, it will be set to `Optional.empty()`.
Previously, an empty body was associated with a specific "empty" type based on the `Content-Type` header. For instance, if Play parsed an empty body as a raw buffer, the request would contain a `RawBuffer` object with a size of zero.

### Handling Incorrect Percent-Encoded URI Characters

Since akka-http 10.2.0, incorrect percent-encoded URI characters will be immediately treated as a 400 bad request by akka-http. This behavior does not give Play a chance to handle the error. To ensure compatibility between both HTTP backends, we have made netty behave in the same way – not going through the error handler when parsing incorrect percent-encoded characters. This approach aims to maximize compatibility in case one backend is replaced by the other.

### Caffeine has a new default value defined by `play.cache.caffeine.defaults.maximum-size`.

If your application adds many items to the cache over time, with the Caffeine-based cache, the JVM may eventually crash due to an `Out of Memory` error. We are defining a maximum size of 10,000 as the project's default value for Caffeine configuration.

### Caffeine uses Play's default dispatcher internally now

The dispatcher internally used by Caffeine by default is now Play's default dispatcher. Before Play 2.9.0 Java's common pool (`ForkJoinPool.commonPool()`) was used.
You can change the dispatcher used via the config `play.cache.dispatcher` or even set different executors for different caches as described in the [[Scala cache API|ScalaCache#Caffeine1]] and the [[Java cache API|JavaCache#Caffeine1]].

### Test servers use a random port now

When writing functional tests using [[Play Java|JavaFunctionalTest#Testing-with-a-server]], [[Play Scala specs2|ScalaFunctionalTestingWithSpecs2#WithServer]], or [[ScalaTest + Play|ScalaFunctionalTestingWithScalaTest#Testing-with-a-server]], if you don't explicitly specify a port for a test server to run on, it will now default to using a random port instead of the previous default, which was port `19001`. This change is designed to facilitate running tests in parallel. Instead of providing an explicit port, you can also set the system property `testserver.port` (e.g., to `19001` to enable the previous behavior).

It's important that when using random ports, you retrieve the correct port in your tests. Instead of using `running(TestServer(...))(block(..))`, you can now utilize the newly introduced method `runningWithPort(TestServer(...))(port => block(..))`, which provides the actual used port as a parameter.

### Test servers no longer bind an HTTPS port by default

In addition to the previous section, where we discussed the new random port behavior, test servers no longer bind an HTTPS port by default. However, you can re-enable this behavior by passing the `testserver.httpsport` system property. If the HTTPS port is bound, a self-signed certificate will be used.

## Dependency graph changes

The `play-jdbc-api` artifact no longer depends on the `play` core artifact.

## Updated libraries

Play 2.9 upgraded following of our own libraries:

| **Dependency**  | **From** | **To** |
|-----------------|----------|--------|
| Play File Watch | 1.1.16   | 1.2.0  |
| Play JSON       | 2.8.2    | 2.10.0 |
| Play WS         | 2.1.11   | 2.2.0  |
| Twirl           | 1.5.1    | 1.6.0  |

Besides updates to newer versions of our own libraries, many other important 3rd party dependencies were updated to the newest versions:

| **Dependency**                   | **From** | **To**      |                              |
|----------------------------------|----------|-------------|------------------------------|
| Akka HTTP                        | 10.1.15  | 10.2.10     |                              |
| Guice                            | 4.2.3    | 6.0.0       | When using `guice`           |
| HikariCP                         | 3.4.5    | 5.0.1       | When using `jdbc`            |
| scala-xml                        | 1.3.1    | 2.2.0       |                              |
| Jackson and Jackson Module Scala | 2.11.4   | 2.14.3      |                              |
| SBT Native Packager              | 1.5.2    | 1.9.16      |                              |
| Logback                          | 1.2.12   | 1.4.11      |                              |
| SLF4J API                        | 1.7.36   | 2.0.7       |                              |
| Caffeine                         | 2.8.8    | 3.1.7       | When using `caffeine`        |
| sbt-web                          | 1.4.4    | 1.5.0       |                              |
| sbt-js-engine                    | 1.2.3    | 1.3.0       |                              |
| Guava                            | 30.1.1   | 32.1.2      |                              |
| Lightbend SSL Config             | 0.4.3    | 0.6.1       |                              |
| Java JSON Web Token (JJWT)       | 0.9.1    | 0.11.5      |                              |
| TypeTools                        | 0.5.0    | 0.6.3       | Used by the Java routing DSL |
| FluentLenium                     | 3.7.1    | 6.0.0       | Now `io.fluentlenium`        |
| Selenium                         | 3.141.59 | 4.11.0      |                              |
| Selenium HtmlUnitDriver          | 2.36.0   | 4.11.0      | Same version as Selenium now |
| specs2                           | 4.8.3    | 4.20.2      |                              |
| JUnit Interface                  | 0.11     | 0.13.3      |                              |
| Joda-Time                        | 2.10.14  | 2.12.5      | When using `jodaForms`       |
| Hibernate Validator              | 6.1.7    | 6.2.5.Final | When using `PlayJava` plugin |

### Changed groupId

We now publish all Play artifacts and libraries under the `com.typesafe.play` namespace. Consequently, we had to make adjustments to the following libraries:

| **Artifact**      | **Old groupId**      | **New groupId**     |
|-------------------|----------------------|---------------------|
| `play-file-watch` | `com.lightbend.play` | `com.typesafe.play` |
| `sbt-twirl`       | `com.typesafe.sbt`   | `com.typesafe.play` |

### Changed artifact names

To ensure consistent naming across all artifacts, where each name is prefixed with `play-`, we have made adjustments to the remaining artifactIds

| **Old artifactId** | **New artifactId**     |
|--------------------|------------------------|
| `build-link`       | `play-build-link`      |
| `filters-helpers`  | `play-filters-helpers` |
| `routes-compiler`  | `play-routes-compiler` |
| `run-support`      | `play-run-support`     |

## Removed libraries

Following dependencies have been removed from Play:

- `"org.scala-lang.modules" %% "scala-java8-compat"`
  Because Scala 2.12 support has been dropped in Play 2.9, it's no longer necessary to pull in this dependency. Use the classes under `scala.jdk` instead which were added to the standard library in Scala 2.13.

- `"jakarta.xml.bind" % "jakarta.xml.bind-api"`
  The upgraded JJWT dependency does not use JAXB anymore.

- `"org.mortbay.jetty.alpn" % "jetty-alpn-agent"`
  and
  `"com.lightbend.sbt" % "sbt-javaagent"`
  Since Java 11 is the minimum required Java versions for Play 2.9 and supports ALPN out of the box these dependencies are not required anymore.

- `"jakarta.transaction" % "jakarta.transaction-api"`
  It was of no use and it seems slipped through until now.
