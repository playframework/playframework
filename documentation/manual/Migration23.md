<!--- Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com> -->
# Play 2.3 Migration Guide

This guide is for migrating to Play 2.3 from Play 2.2. To migrate to Play 2.2, first follow the [[Play 2.2 Migration Guide|Migration22]].

## Distribution

Play is no longer distributed as a zip file that needs to installed.  Instead, the preferred way to obtain and run Play is using [Typesafe Activator](https://typesafe.com/activator).  Typesafe Activator provides an `activator` command, which, like the Play command, delegates to sbt.  So generally, where you previously run commands like `play run`, now you run `activator run`.

To download and get started with Activator, follow the instructions [here](https://typesafe.com/platform/getstarted).

## Build tasks

### Auto Plugins

sbt 0.13.5 is now the version used by Play. This version brings a new feature named "auto plugins".

Auto plugins permit sbt plugins to be declared in the `project` folder (typically the `plugins.sbt`) as before. What has changed though is that plugins may now declare their requirements of other plugins and what triggers their enablement for a given build. Before auto plugins, plugins added to the build were always available; now plugins are enabled selectively for given modules.

What this means for you is that declaring `addSbtPlugin` may not be sufficient for plugins that now utilize to the auto plugin functionality. This is a good thing. You may now be selective as to which modules of your project should have which plugins e.g.:

```scala
lazy val root = (project in file(".")).addPlugins(SbtWeb)
```

The above example shows `SbtWeb` being added to the root project of a build. In the case of `SbtWeb` there are other plugins that become enabled if it is e.g. if you also had added the `sbt-less-plugin` via `addSbtPlugin` then it will become enabled just because `SbtWeb` has been enabled. `SbtWeb` is thus a "root" plugin for that category of plugins.

Play itself is now added using the auto plugin mechanism. The mechanism used in Play 2.2 where `playJavaSettings` and `playScalaSettings` were used has been removed. You now use one of the following instead:

```java
lazy val root = (project in file(".")).addPlugins(PlayJava)
```

or

```scala
lazy val root = (project in file(".")).addPlugins(PlayScala)
```

If you were previously using play.Project, for example a Scala project:

```scala
object ApplicationBuild extends Build {

  val appName = "myproject"
  val appVersion = "1.0-SNAPSHOT"

  val appDependencies = Seq()

  val main = play.Project(appName, appVersion, appDependencies).settings(
  )

}
```

...then you can continue to use a similar approach via native sbt:

```scala
object ApplicationBuild extends Build {

  val appName = "myproject"
  val appVersion = "1.0-SNAPSHOT"

  val appDependencies = Seq()

  val main = Project(appName, file(".")).addPlugins(play.PlayScala).settings(
    version := appVersion,
    libraryDependencies ++= appDependencies
  )

}
```

By moving to the above style settings are now automatically imported when a plugin is enabled.

The keys provided by Play must now also be referenced within the `PlayKeys` object. For example to reference `playVersion` you must do so either by importing:

```scala
import PlayKeys._
```

or qualifying it with `PlayKeys.playVersion`.

Outside of using a `.sbt` file i.e. if you're using Scala to describe your build then you may do the following to have `PlayKeys` within scope:

```scala
import play.Play.autoImport._
import PlayKeys._
```

or

```scala
import play.Play.autoImport._
import PlayKeys._
```

### sbt-web

The largest new feature for Play 2.3 is the introduction of [sbt-web](https://github.com/sbt/sbt-web#sbt-web). In summary sbt-web allows Html, CSS and JavaScript functionality to be factored out of Play's core into a family of pure sbt plugins. There are two major advantages to you:

* Play is less opinionated on the Html, CSS and JavaScript; and
* sbt-web can have its own community and thrive in parallel to Play's.

There are other advantages including the fact that sbt-web plugins are able to run within the JVM via [Trireme](https://github.com/apigee/trireme#trireme), or natively using [Node.js](http://nodejs.org/). Note that sbt-web is a development environment and does not participate in the execution of a Play application. Trireme is used by default, but if you have Node.js installed and want blistering performance for your builds then you can provide a system property via sbt's SBT_OPTS environment variable. For example:

```bash
export SBT_OPTS="$SBT_OPTS -Dsbt.jse.engineType=Node"
```

An interesting feature of sbt-web is that it is not concerned whether you use "javascripts" or "stylesheets" as your folder names. Any files with the appropriate filename extensions are filtered from within the `app/assets` folder.

A nuance with sbt-web is that *all* assets are served from the `public` folder. Therefore if you previously had assets reside outside of the `public` folder i.e. you used the `playAssetsDirectories` setting as per the following example:

```scala
playAssetsDirectories <+= baseDirectory / "foo"
```

...then you should now use the following:

```scala
unmanagedResourceDirectories in Assets += baseDirectory.value / "foo"
```

...however note that the files there will be aggregated into the target public folder. This means that a file at "public/a.js" will be overwritten with the file at "foo/a.js". Alternatively use sub folders off your project's public folder in order to namespace them.

The following lists all sbt-web related components and their versions at the time of releasing Play 2.3-M1. Note that any dependency of sbt-js-engine is `1.0.0-M2a` given the accidental release of a snapshot with `"sbt-js-engine" % "1.0.0-M2"`.

#### Libraries
```scala
"com.typesafe" %% "webdriver" % "1.0.0-M2"
"com.typesafe" %% "jse" % "1.0.0-M2"
"com.typesafe" %% "npm" % "1.0.0-M2"
```

#### sbt plugins
```scala
"com.typesafe.sbt" % "sbt-web" % "1.0.0-M2"
"com.typesafe.sbt" % "sbt-webdriver" % "1.0.0-M2"
"com.typesafe.sbt" % "sbt-js-engine" % "1.0.0-M2a"

"com.typesafe.sbt" % "sbt-coffeescript" % "1.0.0-M2a"
"com.typesafe.sbt" % "sbt-digest" % "1.0.0-M2"
"com.typesafe.sbt" % "sbt-gzip" % "1.0.0-M2b"
"com.typesafe.sbt" % "sbt-less" % "1.0.0-M2a"
"com.typesafe.sbt" % "sbt-jshint" % "1.0.0-M2a"
"com.typesafe.sbt" % "sbt-mocha" % "1.0.0-M2a"
"com.typesafe.sbt" % "sbt-rjs" % "1.0.0-M2a"
```

***

From your perspective we aim to offer feature parity with previous releases of Play. While things have changed significantly under the hood the transition for you should be minor. The remainder of this section looks at each part of Play that has been replaced with sbt-web and describes what should be changed.

#### CoffeeScript

You must now declare the plugin, typically in your plugins.sbt file:

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.0-M2a")
```

Coffeescript options have changed. The new options are:

* `sourceMaps` When set, generates sourceMap files. Defaults to `true`.

  `CoffeeScriptKeys.sourceMaps := true`

* `bare` When set, generates JavaScript without the [top-level function safety wrapper](http://coffeescript.org/#lexical-scope). Defaults to `false`.

  `CoffeeScriptKeys.bare := false`

For more information please consult [the plugin's documentation](https://github.com/sbt/sbt-coffeescript#sbt-coffeescript).

#### LESS

You must now declare the plugin, typically in your plugins.sbt file:

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.0-M2a")
```

Entry points are now declared using a filter. For example, to declare that `foo.less` and `bar.less` are required:

```scala
includeFilter in (Assets, LessKeys.less) := "foo.less" | "bar.less"
```

The plugin's options are:

Option              | Description
--------------------|------------
cleancss            | Compress output using clean-css.
cleancssOptions     | Pass an option to clean css, using CLI arguments from https://github.com/GoalSmashers/clean-css .
color               | Whether LESS output should be colorised
compress            | Compress output by removing some whitespaces.
ieCompat            | Do IE compatibility checks.
insecure            | Allow imports from insecure https hosts.
maxLineLen          | Maximum line length.
optimization        | Set the parser's optimization level.
relativeUrls        | Re-write relative urls to the base less file.
rootpath            | Set rootpath for url rewriting in relative imports and urls.
silent              | Suppress output of error messages.
sourceMap           | Outputs a v3 sourcemap.
sourceMapFileInline | Whether the source map should be embedded in the output file
sourceMapLessInline | Whether to embed the less code in the source map
sourceMapRootpath   | Adds this path onto the sourcemap filename and less file paths.
strictImports       | Whether imports should be strict.
strictMath          | Requires brackets. This option may default to true and be removed in future.
strictUnits         | Whether all unit should be strict, or if mixed units are allowed.
verbose             | Be verbose.

For more information please consult [the plugin's documentation](https://github.com/sbt/sbt-less#sbt-less).

#### Closure Compiler

The Closure Compiler has been replaced. Its two important functions of validating JavaScript and minifying it have been factored out into [JSHint](http://www.jshint.com/) and [UglifyJS 2](https://github.com/mishoo/UglifyJS2#uglifyjs-2) respectively.

To use JSHint you must declare it, typically in your plugins.sbt file:

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.0-M2a")
```

Options can be specified in accordance with the [JSHint website](http://www.jshint.com/docs) and they share the same set of defaults. To set an option you can provide a `.jshintrc` file within your project's base directory. If there is no such file then a `.jshintrc` file will be searched for in your home directory. This behaviour can be overridden by using a `JshintKeys.config` setting for the plugin.
`JshintKeys.config` is used to specify the location of a configuration file.

For more information please consult [the plugin's documentation](https://github.com/sbt/sbt-jshint#sbt-jshint).

UglifyJS 2 is presently provided via the RequireJS plugin (described next). The intent in future is to provide a standalone UglifyJS 2 plugin also for situations where RequireJS is not used.

#### RequireJS

The RequireJS Optimizer (rjs) has been entirely replaced with one that should be a great deal easier to use. The new rjs is part of sbt-web's asset pipeline functionality. Unlike its predecessor which was invoked on every build, the new one is invoked only when producing a distribution via Play's `stage` or `dist` tasks.

To use rjs you must declare it, typically in your plugins.sbt file:

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.0-M2a")
```

The options have changed entirely. A standard build profile for the RequireJS optimizer is provided and should suffice for most projects. However if you would prefer to provide your own build profile then create an `app.build.js` file in your project's folder. For more information on build profiles see http://requirejs.org/docs/optimization.html. Note that one requirement for these build profiles is to accept the last line being a line to receive five parameters passed by this plugin. Whether you use them or not is at your discretion, but that last line must be there.

Here is the default app.build.js profile which you should use as a basis for any of your own:

```javascript
(function (appDir, baseUrl, dir, paths, buildWriter) {
    return {
        appDir: appDir,
        baseUrl: baseUrl,
        dir: dir,
        generateSourceMaps: true,
        mainConfigFile: appDir + "/" + baseUrl + "/main.js",
        modules: [
            {
                name: "main"
            }
        ],
        onBuildWrite: buildWriter,
        optimize: "uglify2",
        paths: paths,
        preserveLicenseComments: false
    }
}(undefined, undefined, undefined, undefined, undefined))
```

For more information please consult [the plugin's documentation](https://github.com/sbt/sbt-rjs#sbt-rjs).

## Results restructure

In Play 2.2, a number of result types were deprecated, and to facilitate migration to the new results structure, some new types introduced.  Play 2.3 finishes this restructuring.

### Scala results

The following deprecated types and helpers from Play 2.1 have been removed:

* `play.api.mvc.PlainResult`
* `play.api.mvc.ChunkedResult`
* `play.api.mvc.AsyncResult`
* `play.api.mvc.Async`

If you have code that is still using these, please see the [[Play 2.2 Migration Guide|Migration22]] to learn how to migrate to the new results structure.

As planned back in 2.2, 2.3 has renamed `play.api.mvc.SimpleResult` to `play.api.mvc.Result` (replacing the existing `Result` trait).  A type alias has been introduced to facilitate migration, so your Play 2.2 code should be source compatible with Play 2.3, however we will eventually remove this type alias so we have deprecated it, and recommend switching to `Result`.

### Java results

The following deprecated types and helpers from Play 2.1 have been removed:

* `play.mvc.Results.async`
* `play.mvc.Results.AsyncResult`

If you have code that is still using these, please see the [[Play 2.2 Migration Guide|Migration22]] to learn how to migrate to the new results structure.

As planned back in 2.2, 2.3 has renamed `play.mvc.SimpleResult` to `play.mvc.Result`.  This should be transparent to most Java code.  The most prominent places where this will impact is in the `Global.java` error callbacks, and in custom actions.

## Play WS

The WS client is now an optional library. If you are using WS in your project then you will need to add the library dependency. For Java projects you will also need to update to a new package.

#### Java projects

Add library dependency to `build.sbt`:

```scala
libraryDependencies += PlayKeys.javaWs
```

Update to the new library package in source files:

```java
import play.libs.ws.*;
```

#### Scala projects

Add library dependency to `build.sbt`:

```scala
libraryDependencies += PlayKeys.ws
```

In addition, usage of the WS client now requires a Play application in scope. Typically this is achieved by adding:

```scala
import play.api.Play.current
```

The WS API has changed slightly, and `WS.client` now returns an instance of `WSClient` rather than the underlying `AsyncHttpClient` object.  You can get to the `AsyncHttpClient` by calling `WS.client.underlying`.

## Anorm

There are various changes included for Anorm in this new release.

For improved type safety, type of query parameter must be visible, so that it [can be properly converted](https://github.com/playframework/playframework/blob/master/documentation/manual/scalaGuide/main/sql/ScalaAnorm.md#edge-cases). Now using `Any` as parameter value, explicitly or due to erasure, leads to compilation error `No implicit view available from Any => anorm.ParameterValue`.

```scala
// Wrong
val p: Any = "strAsAny"
SQL("SELECT * FROM test WHERE id={id}").
  on('id -> p) // Erroneous - No conversion Any => ParameterValue

// Right
val p = "strAsString"
SQL("SELECT * FROM test WHERE id={id}").on('id -> p)

// Wrong
val ps = Seq("a", "b", 3) // inferred as Seq[Any]
SQL("SELECT * FROM test WHERE (a={a} AND b={b}) OR c={c}").
  on('a -> ps(0), // ps(0) - No conversion Any => ParameterValue
    'b -> ps(1), 
    'c -> ps(2))

// Right
val ps = Seq[anorm.ParameterValue]("a", "b", 3) // Seq[ParameterValue]
SQL("SELECT * FROM test WHERE (a={a} AND b={b}) OR c={c}").
  on('a -> ps(0), 'b -> ps(1), 'c -> ps(2))
```

If passing value without safe conversion is required, `anorm.Object(anyVal)` can be used to set an opaque parameter.

Moreover, erasure issues about parameter value is fixed: type is no longer `ParameterValue[_]` but simply `ParameterValue`.

Types for parameter names are also unified (when using `.on(...)`). Only `String` and `Symbol` are now supported as name.

## Twitter Bootstrap

The in-built Twitter Bootstrap field constructor has been deprecated, and will be removed in a future version of Play.

There are a few reasons for this, one is that we have found that Bootstrap changes too drastically between versions and too frequently, such that any in-built support provided by Play quickly becomes stale and incompatible with the current Bootstrap version.

Another reason is that the current Bootstrap requirements for CSS classes can't be implemented with Play's field constructor alone, a custom input template is also required.

Our view going forward is that if this is a feature that is valuable to the community, a third party module can be created which provides a separate set of Bootstrap form helper templates, specific to given Bootstrap versions, allowing a much better user experience than can currently be provided.
