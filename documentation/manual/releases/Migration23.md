<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Play 2.3 Migration Guide

This is a guide for migrating from Play 2.2 to Play 2.3. If you need to migrate from an earlier version of Play then you must first follow the [[Play 2.2 Migration Guide|Migration22]].

## Activator

In Play 2.3 the `play` command has become the `activator` command. Play has been updated to use [Activator](https://typesafe.com/activator).

### Activator command

All the features that were available with the `play` command are still available with the `activator` command.

* `activator new` to create a new project. See [[Creating a new application|NewApplication]].
* `activator` to run the console. See [[Using the Play console|PlayConsole]].
* `activator ui` is a new command that launches a web user interface.

> The new `activator` command and the old `play` command are both wrappers around [sbt](http://www.scala-sbt.org/). If you prefer, you can use the `sbt` command directly. However, if you use sbt you will miss out on several Activator features, such as templates (`activator new`) and the web user interface (`activator ui`). Both sbt and Activator support all the usual console commands such as `test` and `run`.

### Activator distribution

Play is distributed as an Activator distribution that contains all Play's dependencies. You can download this distribution from the [Play download](http://www.playframework.com/download) page.

If you prefer, you can also download a minimal (1MB) version of Activator from the [Activator site](https://typesafe.com/activator). Look for the "mini" distribution on the download page. The minimal version of Activator will only download dependencies when they're needed.

Since Activator is a wrapper around sbt, you can also download and use [sbt](http://www.scala-sbt.org/) directly, if you prefer.

## Build changes

### sbt

Play uses sbt 0.13.5. If you're updating an existing project, change your `project/build.properties` file to:

```
sbt.version=0.13.5
```

### Plugin changes

Change the version of the Play plugin in `project/plugins.sbt`:

```scala
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.XXX")
```

Where `2.3.XXX` is the version of Play you want to use.

You will also need to add some sbt-web plugins, see the *sbt-web* section below.

### Auto Plugins and plugin settings

sbt 0.13.5 brings a new feature named "auto plugins".

Auto plugins permit sbt plugins to be declared in the `project` folder (typically the `plugins.sbt`) as before. What has changed though is that plugins may now declare their requirements of other plugins and what triggers their enablement for a given build. Before auto plugins, plugins added to the build were always available; now plugins are enabled selectively for given modules.

What this means for you is that declaring `addSbtPlugin` may not be sufficient for plugins that now utilize to the auto plugin functionality. This is a good thing. You may now be selective as to which modules of your project should have which plugins e.g.:

```scala
lazy val root = (project in file(".")).enablePlugins(SbtWeb)
```

The above example shows `SbtWeb` being added to the root project of a build. In the case of `SbtWeb` there are other plugins that become enabled if it is e.g. if you also had added the `sbt-less-plugin` via `addSbtPlugin` then it will become enabled just because `SbtWeb` has been enabled. `SbtWeb` is thus a "root" plugin for that category of plugins.

Play itself is now added using the auto plugin mechanism. The mechanism used in Play 2.2 where `playJavaSettings` and `playScalaSettings` were used has been removed. You now use one of the following instead:

```java
lazy val root = (project in file(".")).enablePlugins(PlayJava)
```

or

```scala
lazy val root = (project in file(".")).enablePlugins(PlayScala)
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

  val main = Project(appName, file(".")).enablePlugins(play.PlayScala).settings(
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

### Explicit scalaVersion

Play 2.3 supports both Scala 2.11 and Scala 2.10. The Play plugin previously set the `scalaVersion` sbt setting for you. Now you should indicate which version of Scala you wish to use.

Update your `build.sbt` or `Build.scala` to include the Scala version:

For Scala 2.11:

```scala
scalaVersion := "2.11.1"
```

For Scala 2.10:

```scala
scalaVersion := "2.10.4"
```

### sbt-web

The largest new feature for Play 2.3 is the introduction of [sbt-web](https://github.com/sbt/sbt-web#sbt-web). In summary sbt-web allows Html, CSS and JavaScript functionality to be factored out of Play's core into a family of pure sbt plugins. There are two major advantages to you:

* Play is less opinionated on the Html, CSS and JavaScript; and
* sbt-web can have its own community and thrive in parallel to Play's.

There are other advantages including the fact that sbt-web plugins are able to run within the JVM via [Trireme](https://github.com/apigee/trireme#trireme), or natively using [Node.js](http://nodejs.org/). Note that sbt-web is a development environment and does not participate in the execution of a Play application. Trireme is used by default, but if you have Node.js installed and want blistering performance for your builds then you can provide a system property via sbt's SBT_OPTS environment variable. For example:

```bash
export SBT_OPTS="$SBT_OPTS -Dsbt.jse.engineType=Node"
```

A feature of sbt-web is that it is not concerned whether you use "javascripts" or "stylesheets" as your folder names. Any files with the appropriate filename extensions are filtered from within the `app/assets` folder.

A nuance with sbt-web is that *all* assets are served from the `public` folder. Therefore if you previously had assets reside outside of the `public` folder i.e. you used the `playAssetsDirectories` setting as per the following example:

```scala
playAssetsDirectories <+= baseDirectory / "foo"
```

...then you should now use the following:

```scala
unmanagedResourceDirectories in Assets += baseDirectory.value / "foo"
```

...however note that the files there will be aggregated into the target public folder. This means that a file at "public/a.js" will be overwritten with the file at "foo/a.js". Alternatively use sub folders off your project's public folder in order to namespace them.

The following lists all sbt-web related components and their versions at the time of releasing Play 2.3.

#### Libraries
```scala
"com.typesafe" %% "webdriver" % "1.0.0"
"com.typesafe" %% "jse" % "1.0.0"
"com.typesafe" %% "npm" % "1.0.0"
```

#### sbt plugins
```scala
"com.typesafe.sbt" % "sbt-web" % "1.0.0"
"com.typesafe.sbt" % "sbt-webdriver" % "1.0.0"
"com.typesafe.sbt" % "sbt-js-engine" % "1.0.0"

"com.typesafe.sbt" % "sbt-coffeescript" % "1.0.0"
"com.typesafe.sbt" % "sbt-digest" % "1.0.0"
"com.typesafe.sbt" % "sbt-gzip" % "1.0.0"
"com.typesafe.sbt" % "sbt-less" % "1.0.0"
"com.typesafe.sbt" % "sbt-jshint" % "1.0.0"
"com.typesafe.sbt" % "sbt-mocha" % "1.0.0"
"com.typesafe.sbt" % "sbt-rjs" % "1.0.1"
```

#### WebJars

[WebJars](http://www.webjars.org/) now play an important role in the provision of assets to a Play application. For example you can declare that you will be using the popular [Bootstrap library](http://getbootstrap.com/) simply by adding the following dependency in your build file:

```scala
libraryDependencies += "org.webjars" % "bootstrap" % "3.2.0"
```

WebJars are automatically extracted into a `lib` folder relative to your public assets for convenience. For example if you declared a dependency on [RequireJs](http://requirejs.org/) then you can reference it from a view using a line like:

```html
<script data-main="@routes.Assets.at("javascripts/main.js")" type="text/javascript" src="@routes.Assets.at("lib/requirejs/require.js")"></script>
```

Note the `lib/requirejs/require.js` path. The `lib` folder denotes the extract WebJar assets, the `requirejs` folder corresponds to the WebJar artifactId, and the `require.js` refers to the required asset at the root of the WebJar.

#### npm

[npm](https://www.npmjs.org/) can be used as well as WebJars by declaring a `package.json` file in the root of your project. Assets from npm packages are extracted into the same `lib` folder as WebJars so that, from a code perspective, there is no concern whether the asset is sourced from a WebJar or from an npm package.

***

From your perspective we aim to offer feature parity with previous releases of Play. While things have changed significantly under the hood the transition for you should be minor. The remainder of this section looks at each part of Play that has been replaced with sbt-web and describes what should be changed.

#### CoffeeScript

You must now declare the plugin, typically in your plugins.sbt file:

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.0")
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
addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.0")
```

Entry points are now declared using a filter. For example, to declare that `foo.less` and `bar.less` are required:

```scala
includeFilter in (Assets, LessKeys.less) := "foo.less" | "bar.less"
```

If you previously used the behavior where files with an underscore preceding the less filename were ignored but all other less files were compiled then use the following filters:

```scala
includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := "_*.less"
```

Unlike Play 2.2, the sbt-less plugin allows any user to download the original LESS source file and generated source maps. It allows easier debugging in modern web browsers. This feature is enabled even in production mode.

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
addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.0")
```

Options can be specified in accordance with the [JSHint website](http://www.jshint.com/docs) and they share the same set of defaults. To set an option you can provide a `.jshintrc` file within your project's base directory. If there is no such file then a `.jshintrc` file will be searched for in your home directory. This behaviour can be overridden by using a `JshintKeys.config` setting for the plugin.
`JshintKeys.config` is used to specify the location of a configuration file.

For more information please consult [the plugin's documentation](https://github.com/sbt/sbt-jshint#sbt-jshint).

UglifyJS 2 is presently provided via the RequireJS plugin (described next). The intent in future is to provide a standalone UglifyJS 2 plugin also for situations where RequireJS is not used.

#### RequireJS

The RequireJS Optimizer (rjs) has been entirely replaced with one that should be a great deal easier to use. The new rjs is part of sbt-web's asset pipeline functionality. Unlike its predecessor which was invoked on every build, the new one is invoked only when producing a distribution via Play's `stage` or `dist` tasks.

To use rjs you must declare it, typically in your plugins.sbt file:

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.1")
```

To add the plugin to the asset pipeline you can declare it as follows:

```scala
pipelineStages := Seq(rjs)
```

We also recommend that sbt-web's sbt-digest and sbt-gzip plugins are included in the pipeline. sbt-digest will provide Play's asset controller with the ability to fingerprint asset names for far-future caching. sbt-gzip produces a gzip of your assets that the asset controller will favor when requested. Your plugins.sbt file for this configuration will then look like:

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")
```

and your pipeline configuration now becomes:

```scala
pipelineStages := Seq(rjs, digest, gzip)
```

The order of stages is significant. You first want to optimize the files, produce digests of them and then produce gzip versions of all resultant assets.

The options for RequireJs optimization have changed entirely. The plugin's options are:

Option                  | Description
------------------------|------------
appBuildProfile         | The project build profile contents.
appDir                  | The top level directory that contains your app js files. In effect, this is the source folder that rjs reads from.
baseUrl                 | The dir relative to the assets or public folder where js files are housed. Will default to "js", "javascripts" or "." with the latter if the other two cannot be found.
buildProfile            | Build profile key -> value settings in addition to the defaults supplied by appBuildProfile. Any settings in here will also replace any defaults.
dir                     | By default, all modules are located relative to this path. In effect this is the target directory for rjs.
generateSourceMaps      | By default, source maps are generated.
mainConfig              | By default, 'main' is used as the module for configuration.
mainConfigFile          | The full path to the above.
mainModule              | By default, 'main' is used as the module.
modules                 | The json array of modules.
optimize                | The name of the optimizer, defaults to uglify2.
paths                   | RequireJS path mappings of module ids to a tuple of the build path and production path. By default all WebJar libraries are made available from a CDN and their mappings can be found here (unless the cdn is set to None).
preserveLicenseComments | Whether to preserve comments or not. Defaults to false given source maps (see http://requirejs.org/docs/errors.html#sourcemapcomments).
webJarCdns              | CDNs to be used for locating WebJars. By default "org.webjars" is mapped to "jsdelivr".
webJarModuleIds         | A sequence of webjar module ids to be used.

For more information please consult [the plugin's documentation](https://github.com/sbt/sbt-rjs#sbt-rjs).

### Default ivy local repository and cache

Due to Play now using Activator as a launcher, it now uses the default ivy cache and local repository.  This means anything previously published to your Play ivy cache that you depend on will need to be published to the local ivy repository in the `.ivy2` folder in your home directory.

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

## Templates

The template engine is now a separate project, [Twirl](https://github.com/playframework/twirl).

### Content types

The template content types have moved to the twirl package. If the `play.mvc.Content` type is referenced then it needs to be updated to `play.twirl.api.Content`. For example, the following code in a Play Java project:

```java
import play.mvc.Content;

Content html = views.html.index.render("42");
```

will produce the error:

```
[error] ...: incompatible types
[error] found   : play.twirl.api.Html
[error] required: play.mvc.Content
```

and requires `play.twirl.api.Content` to be imported instead.

### sbt settings

The sbt settings for templates are now provided by the sbt-twirl plugin.

Adding additional imports to templates was previously:

```scala
templatesImport += "com.abc.backend._"
```

and is now:

```scala
TwirlKeys.templateImports += "org.abc.backend._"
```

Specifying custom template formats was previously:

```scala
templatesTypes += ("html" -> "my.HtmlFormat.instance")
```

and is now:

```scala
TwirlKeys.templateFormats += ("html" -> "my.HtmlFormat.instance")
```

For sbt builds that use the full scala syntax, `TwirlKeys` can be imported with:

```scala
import play.twirl.sbt.Import._
```

### Html.empty replaced by HtmlFormat.empty

If you were using before `Html.empty` (`play.api.templates.Html.empty`), you must now use `play.twirl.api.HtmlFormat.empty`.

## Play WS

The WS client is now an optional library. If you are using WS in your project then you will need to add the library dependency. For Java projects you will also need to update to a new package.

#### Java projects

Add library dependency to `build.sbt`:

```scala
libraryDependencies += javaWs
```

Update to the new library package in source files:

```java
import play.libs.ws.*;
```

#### Scala projects

Add library dependency to `build.sbt`:

```scala
libraryDependencies += ws
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

Type *`Pk[A]`* has been deprecated. You can still use it as column mapping, but need to explicitly pass either `Id[A]` or `NotAssigned` as query parameter (as consequence of type safety improvements):

```scala
// Column mapping, deprecated but Ok
val pk: Pk[Long] = SQL("SELECT id FROM test WHERE name={n}").
  on('n -> "mine").as(get[Pk[Long]].single)

// Wrong parameter
val pkParam: Pk[Long] = Id(1l)
val name1 = "Name #1"
SQL"INSERT INTO test(id, name) VALUES($pkParam, $name1)".execute()
// ... pkParam is passed as Pk in query parameter, 
// which is now wrong as a parameter type (won't compile)

// Right parameter Id
val idParam: Id[Long] = Id(2l) // same as pkParam but keep explicit Id type
val name2 = "Name #2"
SQL"INSERT INTO test(id, name) VALUES($idParam, $name2)".execute()

// Right parameter NotAssigned
val name2 = "Name #3"
SQL"INSERT INTO test(id, name) VALUES($NotAssigned, $name2)".execute()
```

As deprecated `Pk[A]` is similar to `Option[A]`, which is supported by Anorm in column mapping and as query parameter, it's preferred to replace `Id[A]` by `Some[A]` and `NotAssigned` by `None`:

```scala
// Column mapping, deprecated but Ok
val pk: Option[Long] = SQL("SELECT id FROM test WHERE name={n}").
  on('n -> "mine").as(get[Option[Long]].single)

// Assigned primary key as parameter
val idParam: Option[Long] = Some(2l)
val name1 = "Id"
SQL"INSERT INTO test(id, name) VALUES($idParam, $name1)".execute()

// Right parameter NotAssigned
val name2 = "NotAssigned"
SQL"INSERT INTO test(id, name) VALUES($None, $name2)".execute()
```

## Bootstrap

The in-built Bootstrap field constructor has been deprecated, and will be removed in a future version of Play.

There are a few reasons for this, one is that we have found that Bootstrap changes too drastically between versions and too frequently, such that any in-built support provided by Play quickly becomes stale and incompatible with the current Bootstrap version.

Another reason is that the current Bootstrap requirements for CSS classes can't be implemented with Play's field constructor alone, a custom input template is also required.

Our view going forward is that if this is a feature that is valuable to the community, a third party module can be created which provides a separate set of Bootstrap form helper templates, specific to given Bootstrap versions, allowing a much better user experience than can currently be provided.

## Session timeouts

The session timeout configuration item, `session.maxAge`, used to be an integer, defined to be in seconds.  Now it's a duration, so can be specified with values like `1h` or `30m`.  Unfortunately, the default unit if specified with no time unit is milliseconds, which means a config value of `3600` was previously treated as one hour, but is now treated as 3.6 seconds.  You will need to update your configuration to add a time unit.

## Java JUnit superclasses

The Java `WithApplication`, `WithServer` and `WithBrowser` JUnit test superclasses have been modified to define an `@Before` annotated method.  This means, previously where you had to explicitly start a fake application by defining:

```java
@Before
public void setUp() {
    start();
}
```

Now you don't need to. If you need to provide a custom fake application, you can do so by overriding the `provideFakeApplication` method:

```java
@Override
protected FakeApplication provideFakeApplication() {
    return Helpers.fakeApplication(Helpers.inMemoryDatabase());
}
```

## Session and Flash implicits

The Scala Controller provides implicit `Session`, `Flash` and `Lang` parameters, that take an implicit `RequestHeader`.  These exist for convenience, so a template for example can take an implicit argument and they will be automatically provided in the controller.  The name of these was changed to avoid conflicts where these parameter names might be shadowed by application local variables with the same name. `session` became `request2Session`, `flash` became `flash2Session`, `lang` became `lang2Session`.  Any code that invoked these explicitly consequently will break.

It is not recommended that you invoke these implicit methods explicitly, the `session`, `flash` and `lang` parameters are all available on the `RequestHeader`, and using the `RequestHeader` properties makes it much clearer where they come from when reading the code.  It is recommended that if you have code that uses the old methods, that you modify it to access the corresponding properties on `RequestHeader` directly.
