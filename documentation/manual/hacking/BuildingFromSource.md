<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Building Play from source

To benefit from the latest improvements and bug fixes after the initial beta release, you may want to compile Play from source. You’ll need a [Git client](http://git-scm.com/) to fetch the source.

## Grab the source
From the shell, first checkout the Play source:

```bash
$ git clone git://github.com/playframework/playframework.git
```

Then go to the `playframework/framework` directory and launch the `build` script to enter the sbt build console:

```bash
$ cd playframework/framework
$ ./build
> publishLocal
```

This will build and publish Play for the default Scala version (currently 2.10.4).  If you want to publish for all versions, you can cross build:

```bash
> +publishLocal
```

Or to publish for a specific Scala version:

```bash
> +++2.11.5 publishLocal
```

## Build the documentation

Documentation is available at `playframework/documentation` as Markdown files.  To see HTML, run the following:

```bash
$ cd playframework/documentation
$ ./build run
```

To see documentation at [http://localhost:9000/@documentation](http://localhost:9000/@documentation)

For more details on developing the Play documentation, see the [[Documentation Guidelines|Documentation]].

## Run tests

You can run basic tests from the sbt console using the `test` task:

```
> test
```

Like with publishing, you can prefix the command with `+` to run the tests against all supported Scala versions.

The Play PR validation runs a few more tests than just the basic tests, including scripted tests, testing the documentation code samples, and testing the Play activator templates.  To run all the tests, run the `framework/runtests` script:

```
$ cd playframework/framework
$ ./runtests
```

## Use in projects

Compiling and running projects using the Play version you have built from source requires some custom configuration.

Navigate to your existing Play project and make the following edits in `project/plugins.sbt`:

```
// Change the sbt plugin to use the local Play build (2.4.0-SNAPSHOT)
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.0-SNAPSHOT")
```

Once you have done this, you can start the console and interact with your project normally:

```
$ cd <projectdir>
$ activator
```

## Using Code in Eclipse

You can find at [Stackoverflow](http://stackoverflow.com/questions/10053201/how-to-setup-eclipse-ide-work-on-the-playframework-2-0/10055419#10055419) some information how to setup eclipse to work on the code.
