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
sbt.version=1.3.10
```

At the time of this writing `1.3.10` is the latest version in the sbt 1.x family, you may be able to use newer versions too. Check the release notes for both Play's minor version releases and sbt's [releases](https://github.com/sbt/sbt/releases) for details.

## API Changes

Play 2.9 contains multiple API changes. As usual, we follow our policy of deprecating existing APIs before removing them. This section details these changes.

### Deprecated APIs were removed

Many APIs that were deprecated in earlier versions were removed in Play 2.9. If you are still using them we recommend migrating to the new APIs before upgrading to Play 2.9. Check the Javadocs and Scaladocs for migration notes. See also the [[migration guide for Play 2.8|Migration28]] for more information.

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
