<!--- Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com> -->
# Play 2.3 Migration Guide

This guide is for migrating a Play 2.2 application to Play 2.3.  To migrate from Play 2.1, first follow the [[Play 2.2 Migration Guide|Migration22]].

## Distribution

Play is no longer distributed as a zip file that needs to installed.  Instead, the preferred way to obtain and run Play is using [Typesafe Activator](https://typesafe.com/activator).  Typesafe activator provides an `activator` command, which, like the Play command, delegates to sbt.  So generally, where you previously run commands like `play run`, now you run `activator run`.

To download and get started with Activator, follow the instructions [here](https://typesafe.com/platform/getstarted).

## Build tasks

sbt-web, autoplugins etc.

## Results structure

In Play 2.2, a number of result types were deprecated, and to facilitate migration to the new results structure, some new types introduced.  Play 2.3 finishes this restructuring.

### Scala results

The following deprecated types and helpers from Play 2.1 have been removed:

* `play.api.mvc.PlainResult`
* `play.api.mvc.ChunkedResult`
* `play.api.mvc.AsyncResult`
* `play.api.mvc.Async`

If you have code that is still using these, please see the [[Play 2.2 Migration Guide|Migration22]] to learn how to migrate to the new results structure.

As planned back in 2.2, 2.3 has renamed `play.api.mvc.SimpleResult` to `play.api.mvc.Result` (replacing the existing `Result` trait).  A type alias has been introduced to facilitate migration, so your Play 2.2 code should be source compatible with Play 2.3, however we will eventually remove this type alias so we have deprecated it, and recommend switching to `Result`.
