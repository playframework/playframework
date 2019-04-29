<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# About Play releases

Visit the [download page](https://www.playframework.com/download) to get started with the latest Play releases. This page lists all past major Play releases starting from 2.x.

Since Play 2.0.0, Play is versioned as *epoch.major.minor*. Play currently releases a new major version about every year. Major versions can break APIs, but we try to make sure most existing code will compile with deprecation. Each major release has a Migration Guide that explains how to upgrade from the previous release. *Note that everything in the `play.core` package is considered internal API, and may change without notice.*

Minor versions in our current scheme are backwards binary compatible for all public APIs. *It is generally safe to upgrade to a new minor version with no code changes*, and we will be sure to announce any exceptions to this rule.

The Play team also maintains a number of external projects that integrate with Play, such as play-slick, play-json, play-ws etc. For these, either Play already has a dependency on a compatible version, or we will tell you which version is compatible in the documentation.

We eventually plan to switch Play to use the *major.minor.patch* versioning scheme, and some Play libraries like play-ws are already using this scheme. In that case, the minor version is incremented for major features that don't significantly break APIs, and the patch version is incremented for small bugfixes and binary-compatible changes. Minor versions will maintain backwards compatibility when possible, except for deprecated APIs.

@toc@
