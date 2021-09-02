<!--- Copyright (C) Lightbend Inc. <https://www.lightbend.com> -->

# Play 2.9 Migration Guide

This guide is for migrating from Play 2.8 to Play 2.9. See the [[Play 2.8 Migration Guide|Migration28]] to upgrade from Play 2.7.

## How to migrate

Before starting `sbt`, make sure to make the following upgrades.

### Play update

Update the Play version number in `project/plugins.sbt`:

```scala
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.9.x")
```

Where the "x" in `2.9.x` is the minor version of Play you want to use, for instance `2.9.0`.

### sbt upgrade

Play 2.9 only supports sbt 1.5. To update, change your `project/build.properties` so that it reads:

```properties
sbt.version=1.5.5
```

At the time of this writing `1.5.5` is the latest version in the sbt 1.x family, you may be able to use newer versions too. Check the release notes for both Play's minor version [releases](https://github.com/playframework/playframework/releases) and sbt's [releases](https://github.com/sbt/sbt/releases) for details.

### Minimum required Java version

Play 2.9 requires at least [OpenJDK 8u252](https://mail.openjdk.java.net/pipermail/jdk8u-dev/2020-April/011566.html) (the equivalent Oracle version is named [8u251](https://www.oracle.com/technetwork/java/javase/8u251-relnotes-5972664.html)) because it removed the [Jetty ALPN Agent](https://github.com/jetty-project/jetty-alpn-agent), which, starting with those Java versions, is not strictly required anymore since the standard ALPN APIs have been [backported](https://mail.openjdk.java.net/pipermail/jdk8u-dev/2019-November/010573.html) from Java 9.
In case you passed a `-javaagent:jetty-alpn-agent-*.jar` flag to your Play application(s) (maybe via `SBT_OPTS` environment variable) you have to remove that flags now.

## API Changes

Play 2.9 contains multiple API changes. As usual, we follow our policy of deprecating existing APIs before removing them. This section details these changes.

### Scala 2.12 support discontinued

Play 2.9 supports Scala 2.13, but not 2.12 anymore.

### Setting `scalaVersion` in your project

**Both Scala and Java users** must configure sbt to use Scala 2.13.  Even if you have no Scala code in your project, Play itself uses Scala and must be configured to use the right Scala libraries.

To set the Scala version in sbt, simply set the `scalaVersion` key, for example:

```scala
scalaVersion := "2.13.6"
```

If you have a single project build, then this setting can just be placed on its own line in `build.sbt`.  However, if you have a multi-project build, then the scala version setting must be set on each project.  Typically, in a multi-project build, you will have some common settings shared by every project, this is the best place to put the setting, for example:

```scala
def commonSettings = Seq(
  scalaVersion := "2.13.6"
)

val projectA = (project in file("projectA"))
  .enablePlugins(PlayJava)
  .settings(commonSettings)

val projectB = (project in file("projectB"))
  .enablePlugins(PlayJava)
  .settings(commonSettings)
```

### Deprecated APIs were removed

Many APIs that were deprecated in earlier versions were removed in Play 2.9. If you are still using them we recommend migrating to the new APIs before upgrading to Play 2.9. Check the Javadocs and Scaladocs for migration notes. See also the [[migration guide for Play 2.8|Migration28]] for more information.

### Changing CSP report types

There has been some changes in CSP reports, according to [w3.org specification](https://www.w3.org/TR/CSP2/). Since Play 2.9 fields `lineNumber` and `columnNumber` are Longs. If your implementation bases on these fields being Strings, Play will fallback to parsing them from String to Long and throw an error only if parsing fails. It is however encouraged to use number types, not Strings, as this may be changed when CSP3 comes out.

## Configuration changes

This section lists changes and deprecations in configurations.

### Application Secret enforces a minimum length

The [[application secret|ApplicationSecret]] configuration `play.http.secret.key` is checked for a minimum length in all modes now (prod, dev and test). If that minimum length isn't met, then an error is thrown and the configuration is invalid, causing the application not to start.

The minimum length depends on the algorithm used to sign the session or flash cookie, which can be set via the config keys `play.http.session.jwt.signatureAlgorithm` and `play.http.flash.jwt.signatureAlgorithm`. By default the algorithm for both configs is `HS256` which requires the secret to contain at least 256 bits / 32 bytes. When choosing `HS384`, the minimums size is 384 bits / 48 bytes, for `HS512` it's 512 bits / 64 bytes.

The error that gets thrown contains all the information you need to resolve the problem:
```
Configuration error [
  The application secret is too short and does not have the recommended amount of entropy for algorithm HS256 defined at play.http.session.jwt.signatureAlgorithm.
  Current application secret bits: 248, minimal required bits for algorithm HS256: 256.
  To set the application secret, please read https://playframework.com/documentation/latest/ApplicationSecret
]
```

You can resolve such an error by setting the secret to contain the required amount of bits / bytes, like in this example at least 32 bytes of completely random input, such as `head -c 32 /dev/urandom | base64` or by the application secret generator, using `playGenerateSecret` or `playUpdateSecret`.

## Defaults changes

Some default values used by Play had changed and that can have an impact on your application. This section details the default changes.

### TBA

TBA

## Updated libraries

Besides updates to newer versions of our own libraries (play-json, play-ws, twirl, cachecontrol, etc), many other important dependencies were updated to the newest versions:

* Guice 5.0.0
