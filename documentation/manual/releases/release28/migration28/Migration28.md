<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->

# Play 2.8 Migration Guide

This guide is for migrating from Play 2.7 to Play 2.8. See the [[Play 2.7 Migration Guide|Migration27]] to upgrade from Play 2.6.

## How to migrate

Before starting `sbt`, make sure to make the following upgrades.

### Play update

Update the Play version number in `project/plugins.sbt`:

```scala
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.x")
```

Where the "x" in `2.8.x` is the minor version of Play you want to use, for instance `2.8.0`.

### sbt upgrade

Although Play 2.8 still supports sbt 0.13, we recommend that you use sbt 1. This new version is supported and actively maintained. To update, change your `project/build.properties` so that it reads:

```
sbt.version=1.2.8
```

At the time of this writing `1.2.8` is the latest version in the sbt 1.x family, you may be able to use newer versions too. Check the release notes for both Play's minor version releases and sbt's [releases](https://github.com/sbt/sbt/releases) for details.

## API Changes

Multiple API changes were made following our policy of deprecating the existing APIs before removing them. This section details these changes.

### Scala 2.11 support discontinued

Play 2.8 support Scala 2.12 and 2.13, dropping support for 2.11, which has reached its end of life.

### Setting `scalaVersion` in your project

**Both Scala and Java users** must configure sbt to use Scala 2.12 or 2.13.  Even if you have no Scala code in your project, Play itself uses Scala and must be configured to use the right Scala libraries.

To set the Scala version in sbt, simply set the `scalaVersion` key, for example:

```scala
scalaVersion := "2.13.0"
```

If you have a single project build, then this setting can just be placed on its own line in `build.sbt`.  However, if you have a multi-project build, then the scala version setting must be set on each project.  Typically, in a multi-project build, you will have some common settings shared by every project, this is the best place to put the setting, for example:

```scala
def commonSettings = Seq(
  scalaVersion := "2.13.0"
)

val projectA = (project in file("projectA"))
  .enablePlugins(PlayJava)
  .settings(commonSettings)

val projectB = (project in file("projectB"))
  .enablePlugins(PlayJava)
  .settings(commonSettings)
```

### Deprecated APIs were removed

Many APIs that were deprecated in earlier versions were removed in Play 2.8. If you are still using them we recommend migrating to the new APIs before upgrading to Play 2.8. Check the Javadocs and Scaladocs for migration notes. See also the [[migration guide for Play 2.7|Migration27]] for more information.

#### Scala API

1. xxx
1. xxx

Some new methods were added to improve the Scala API too:

xxx

#### Java API

1. In Play 2.7 we deprecate `play.mvc.Http.Context` in favor of directly using `play.mvc.Http.RequestHeader` or `play.mvc.Http.Request`. We have now removed `Http.Context` and if your application was still depending on it, you should read [[Play 2.7 migration guide instructions|JavaHttpContextMigration27]].
1. xxx

Some new methods were added to improve the Java API too:

xxx

### Internal changes

Many changes have been made to Play's internal APIs. These APIs are used internally and don't follow a normal deprecation process. Changes may be mentioned below to help those who integrate directly with Play internal APIs.

## Configuration changes

This section lists changes and deprecations in configurations.

### Dropped the overrides for `akka.actor.default-dispatcher.fork-join-executor`

The overrides that Play had under `akka.actor.default-dispatcher.fork-join-executor` have been dropped in favour of using Akka's new-and-improved defaults.

See the section related to [changes in the default dispatch][akka-migration-guide-default-dispatcher] in Akka's migration guide for more details.

[akka-migration-guide-default-dispatcher]: https://doc.akka.io/docs/akka/2.6/project/migration-guide-2.5.x-2.6.x.html#default-dispatcher-size

### Configuration loading changes

Until Play 2.7, when loading configuration, Play was not considering the default [Java System Properties](https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html) if the user provides some properties. Now, System Properties are always considered, meaning that you can reference them in your `application.conf` file even if you are also defining custom properties. For example, when [[embedding Play|ScalaEmbeddingPlayAkkaHttp]] like the code below, both `userProperties` and System Properties are used:

```scala
import java.util.Properties

import play.api.mvc.Results
import play.core.server.AkkaHttpServer
import play.core.server.ServerConfig
import play.api.routing.sird._

class MyApp {
  def main(args: Array[String]): Unit = {
    // Define some user properties here
    val userProperties = new Properties()
    userProperties.setProperty("my.property", "some value")

    val serverConfig = ServerConfig(properties = userProperties)

    val server = AkkaHttpServer.fromRouterWithComponents(serverConfig) { components => {
      case GET(p"/hello") => components.defaultActionBuilder {
        Results.Ok
      }
    }}
  }
}
```

Keep in mind that user-defined properties have precedence over default System Properties.

### Debugging SSL Connections

Until Play 2.7, both Play and Play-WS were using a version of [ssl-config](https://lightbend.github.io/ssl-config/) which had a debug system that relied on undocumented modification of internal JSSE debug settings. These are usually set using `javax.net.debug` and `java.security.debug` system properties on startup.

This debug system has been removed, the debug flags that do not have a direct correlation in the new system are deprecated, and the new configuration is documented in [ssl-config docs](https://lightbend.github.io/ssl-config/DebuggingSSL.html).

## Defaults changes

Some of the default values used by Play had changed and that can have an impact on your application. This section details the default changes.

## Updated libraries

This section lists significant updates made to our dependencies.

## Removed libraries

To make the default play distribution a bit smaller we removed some libraries. The following libraries are no longer dependencies in Play 2.8, so you will need to add them manually to your build if you use them.

## Other important changes
