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

Play 2.9 only supports sbt 1. To update, change your `project/build.properties` so that it reads:

```properties
sbt.version=1.3.7
```

At the time of this writing `1.3.7` is the latest version in the sbt 1.x family, you may be able to use newer versions too. Check the release notes for both Play's minor version releases and sbt's [releases](https://github.com/sbt/sbt/releases) for details.

## API Changes

Play 2.9 contains multiple API changes. As usual, we follow our policy of deprecating existing APIs before removing them. This section details these changes.

### Deprecated APIs were removed

Many APIs that were deprecated in earlier versions were removed in Play 2.9. If you are still using them we recommend migrating to the new APIs before upgrading to Play 2.9. Check the Javadocs and Scaladocs for migration notes. See also the [[migration guide for Play 2.8|Migration28]] for more information.

## Configuration changes

This section lists changes and deprecations in configurations.

### TBA

TBA

## Defaults changes

Some default values used by Play had changed and that can have an impact on your application. This section details the default changes.

### TBA

TBA

## Updated libraries

Besides updates to newer versions of our own libraries (play-json, play-ws, twirl, cachecontrol, etc), many other important dependencies were updated to the newest versions:

* TBA
