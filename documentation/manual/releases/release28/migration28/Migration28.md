<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->

# Play 2.8 Migration Guide

This is a guide for migrating from Play 2.7 to Play 2.8. If you need to migrate from an earlier version of Play then you must first follow the [[Play 2.7 Migration Guide|Migration27]].

## How to migrate

The following steps need to be taken to update your sbt build before you can load/run a Play project in sbt.

### Play upgrade

Update the Play version number in `project/plugins.sbt` to upgrade Play:

```scala
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.x")
```

Where the "x" in `2.8.x` is the minor version of Play you want to use, for instance `2.8.0`.

### sbt upgrade to x.x.x

Although Play 2.8 still supports sbt x.x series, we recommend that you use sbt x.x from now. This new version is actively maintained and supported. To update, change your `project/build.properties` so that it reads:

```
sbt.version=x.x.x
```

At the time of this writing `x.x.x` is the latest version in the sbt x.x family, you may be able to use newer versions too. Check for details in the release notes of your minor version of Play 2.8.x. More information at the list of [sbt releases](https://github.com/sbt/sbt/releases).

## API Changes

Multiple APIs changes were made following our policy of deprecating the existing APIs before removing them. This section details these changes.

### Scala 2.11 support discontinued

Play 2.8 has dropped support for Scala 2.11, which has reached it's end of life, and now only supports Scala 2.12 and 2.13.

### How to migrate

**Both Scala and Java users** must configure sbt to use Scala 2.12 or 2.13.  Even if you have no Scala code in your project, Play itself uses Scala and must be configured to use the right Scala libraries.

To set the Scala version in sbt, simply set the `scalaVersion` key, eg:

```scala
scalaVersion := "2.13.0"
```

If you have a single project build, then this setting can just be placed on its own line in `build.sbt`.  However, if you have a multi project build, then the scala version setting must be set on each project.  Typically, in a multi project build, you will have some common settings shared by every project, this is the best place to put the setting, eg:

```scala
def common = Seq(
  scalaVersion := "2.13.0"
)

lazy val projectA = (project in file("projectA"))
  .enablePlugins(PlayJava)
  .settings(common: _*)

lazy val projectB = (project in file("projectB"))
  .enablePlugins(PlayJava)
  .settings(common: _*)
```

### Deprecated APIs were removed

Many APIs that deprecated in earlier versions were removed in Play 2.8. If you are still using them, we recommend migrating to the new APIs before upgrading to Play 2.8. Both Javadocs and Scaladocs usually have proper documentation on how to migrate. See the [[migration guide for Play 2.7|Migration27]] for more information.

#### Scala API

1. xxx
1. xxx

Some new methods were added to improve the Scala API too:

xxx

#### Java API

1. xxx
1. xxx

Some new methods were added to improve the Java API too:

xxx

### Internal changes

Many changes have been made to Play's internal APIs. These APIs are used internally and don't follow a normal deprecation process. Changes may be mentioned below to help those who integrate directly with Play internal APIs.

## Configuration changes

## Defaults changes

Some of the default values used by Play had changed and that can have an impact on your application. This section details the default changes.

## Updated libraries

This section lists significant updates made to our dependencies.

## Removed libraries

To make the default play distribution a bit smaller we removed some libraries. The following libraries are no longer dependencies in Play 2.8, so you will need to add them manually to your build if you use them.

## Other important changes
