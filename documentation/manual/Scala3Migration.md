<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Scala 3 Migration Guide

This guide is for migrating Play applications from Scala 2 to Scala 3.

## Requirements

### Scala 3 version

You need to use at least Scala 3.3.0. Make sure you set

```scala
scalaVersion := "3.3.0"
```

or newer in your `build.sbt`.

### Play version

To be able to run you Play application with Scala 3 you need to use at least Play 2.9.x, so check your `project/plugins.sbt`:

```scala
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.9.x")
```

Where the "x" in `2.9.x` is the minor version of Play you want to use, for instance `2.9.0`.
Older versions of Play do not provide Scala 3 artifacts.

### sbt version

Play with Scala 3 also only works with sbt 1.8.x or newer, so make sure to your `project/build.properties` reads:

```properties
sbt.version=1.8.2
```

At the time of this writing `1.8.2` is the latest version in the sbt 1.x family, you may be able to use newer versions too. Check the release notes for both Play's minor version [releases](https://github.com/playframework/playframework/releases) and sbt's [releases](https://github.com/sbt/sbt/releases) for details.

## How to migrate

TODO:
- [ ] Mention mockito
- [ ] Mention the `running()` wrapper when using scalatestplus-play
- [ ] Likely the same for spec2 (but still needs to be done)
- [ ] Mention the unapply changes
- [ ] Mention the sird changes: Not everything is in the `UrlContext` implicit class anymore, so importing this class is not enough, it should be `._`
