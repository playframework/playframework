<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Building Play from source

If you want to use some unreleased changes for Play, or you want to contribute to the development of Play yourself, you'll need to compile Play from source. You’ll need a [Git client](https://git-scm.com/) to fetch the source.

## Prerequisites

To build Play, you need to have [sbt](https://www.scala-sbt.org/) installed.

## Grab the source

From the shell, first checkout the Play source:

```bash
$ git clone git://github.com/playframework/playframework.git
```

Checkout the branch you want, the current development branch is called `master`, while stable branches for major releases are named with a `.x`, for example, `2.5.x`.

Now go to the `framework` directory and run `sbt`:

```bash
$ sbt
```

To build and publish Play, run `publishLocal`:

```bash
> publishLocal
```

This will build and publish Play for the default Scala version (currently 2.11.12). If you want to publish for all versions of Scala, you can cross build:

```bash
> +publishLocal
```

Or to publish for a specific Scala version:

```bash
> ++2.11.12 publishLocal
```

## Build the documentation

The documentation is available at `playframework/documentation` as Markdown files. To see HTML, run the following:

```bash
$ cd playframework/documentation
$ sbt run
```

You can now see the documentation at <http://localhost:9000/@documentation>.

For more details on developing the Play documentation, see the [[Documentation Guidelines|Documentation]].

## Run tests

You can run basic tests from the sbt console using the `test` task:

```bash
> test
```

Like with publishing, you can prefix the command with `+` to run the tests against all supported Scala versions.

The Play PR validation runs a few more tests than just the basic tests, including scripted tests, testing the documentation code samples, and testing the Play templates.  The scripts that are run by the PR validation can be found in the `framework/bin` directory, you can run each of these to run the same tests that the PR validation runs.

## Use in projects

When you publish Play locally, it will publish a snapshot version to your local repository.  To use this, you need to update your build configuration to use this version.

Navigate to your existing Play project and make the following edits in `project/plugins.sbt`:

```scala
// Change the sbt plugin to use the local Play build (2.6.0-SNAPSHOT)
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.0-SNAPSHOT")
```

Once you have done this, you can start the console and interact with your project normally:

```bash
$ cd <projectdir>
$ sbt
```

## Using Code in Eclipse

You can find at [Stackoverflow](https://stackoverflow.com/questions/10053201/how-to-setup-eclipse-ide-work-on-the-playframework-2-0/10055419#10055419) some information how to setup eclipse to work on the code.
