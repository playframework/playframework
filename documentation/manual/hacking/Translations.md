# Translating the Play Documentation

Play 2.3+ provides infrastructure to aid documentation translators in translating the Play documentation and keeping it up to date.

As described in the [[Documentation Guidelines|Documentation]], Play's documentation is written in markdown format with code samples extracted to external files.  Play allows the markdown components of the documentation to be translated, while allowing the original code samples from the English documentation to be included in the translated documentation.  This assists translators in maintaining translation quality - the code samples are kept up to date as part of the core Play project, while the translated descriptions have to be maintained manually.

In addition to this, Play also provides facilities for validating the integrity of translated documentation.  This includes validating all internal links, including links to code snippets, in the translation.

## Prerequisites

You need to have `activator` or `sbt` installed.  It will also be very useful to have a clone of the Play repository, with the branch that you're translating checked out, so that you have something to copy to start with.

If you're translating an unreleased version of the Play documentation, then you'll need to build that version of Play and publish it locally on your machine first.  This can be done by running:

```
./build publish-local
```

in the `framework` directory of the Play project.

## Setting up a translation

Create a new SBT project with the following structure:

```
translation-project
  |- manual
  | |- javaGuide
  | |- scalaGuide
  | |- gettingStarted
  | `- etc...
  |- project
  | |- build.properties
  | `- plugins.sbt
  `- build.sbt
```

`build.properties` should contain the SBT version, ie:

```
sbt.version=0.13.5
```

`plugins.sbt` should include the Play docs sbt plugin, ie:

```scala
addSbtPlugin("com.typesafe.play" % "play-docs-sbt-plugin" % "%PLAY_VERSION%")
```

Finally, `build.sbt` should enable the Play docs plugin, ie:

```scala
lazy val root = (project in file(".")).enablePlugins(PlayDocsPlugin)
```

Now you're ready to start translating!

## Translating documentation

First off, start the documentation server.  The documentation server will serve your documentation up for you so you can see what it looks like as you're going.  To do this you'll need `sbt` or `activator` installed, either one is fine, in the examples here we'll be using `sbt`:

```
$ sbt run
[info] Set current project to root (in build file:/Users/jroper/tmp/foo-translation/)
[info] play - Application started (Dev)
[info] play - Listening for HTTP on /0:0:0:0:0:0:0:0:9000

Documentation server started, you can now view the docs by going to http://0:0:0:0:0:0:0:0:9000
```

Now open <http://localhost:9000> in your browser.  You should be able to see the default Play documentation.  It's time to translate your first page.

Copy a markdown page from the Play repository into your project.  It is important to ensure that the directory structure in your project matches the directory in Play, this will ensure that the code samples work.

For example, if you choose to start with `manual/scalaGuide/main/http/ScalaActions.md`, then you need to ensure that it is in `manual/scalaGuide/main/http/ScalaActions.md` in your project.

> **Note:** It may be tempting to start by copying the entire Play manual into your project.  If you do do this, make sure you only copy the markdown files, that you don't copy the code samples as well.  If you copy the code samples, they will override the code samples from Play, and you will lose the benefit of having those code samples automatically maintained for you.

Now you can start translating the file.

## Dealing with code samples

The Play documentation is full of code samples.  As described in the [[Documentation Guidelines|Documentation]], these code samples live outside of the markdown documentation and live in compiled and tested source files.  Snippets from these files get included in the documentation using the following syntax:

```
@[label](code/path/to/SourceFile.java)
```

Generally, you will want to leave these snippets as is in your translation, this will ensure that the code snippets your translation stays up to date with Play.

In some situations, it may make sense to override them.  You can either do this by putting the code directly in the documentation, using a fenced block, or by extracting them into your projects own compile code samples.  If you do that, checkout the Play documentation sbt build files for how you might setup SBT to compile them.

## Validating the documentation

The Play docs sbt plugin provides a documentation validation task that runs some simple tests over the documentation, to ensure the integrity of links and code sample references.  You can run this by running:

```
sbt validate-docs
```

You can also validate the links to external sites in Play's documentation.  This is a separate task because it's dependent on many sites on the internet that Play's documentation links to, and the validation task in fact actually triggers DDoS filters on some sites.  To run it, run:

```
sbt validate-external-links
```

## Deploying documentation to playframework.com

[playframework.com](http://playframework.com) serves documentation out of git repositories.  If you want your translation to be served from playframework.com, you'll need to put your documentation into a GitHub repository, and contact the Play team to have them add it to playframework.com.

The git repository needs to be in a very particular format.  The current master branch is for the documentation of the latest development version of Play.  Documentation for stable versions of Play must be in branches such as 2.3.x.  Documentation specific to a particular release of Play will be served from a tag of the repository with that name, for example, 2.3.1.

Once the Play team has configured playframework.com to serve your translation, any changes pushed to your GitHub repository will be picked up within about 10 minutes, as playframework.com does a `git fetch` on all repos it uses once every 10 minutes.

## Documentation for older releases

If you want to provide documentation for releases prior to Play 2.3, this can be done, however note that the tooling provided only works with Play 2.3 onwards.
