<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Creating a new application

To learn about Play hands-on, try the examples as described below, they contain everything you need to build and run them. If you have [sbt installed](https://www.scala-sbt.org/1.x/docs/Setup.html), you can create a Play  project with a single command, using our giter8 Java or Scala  template. The templates set up the project structure and dev environment for you. You can also easily integrate Play projects into your favorite IDE.

## Downloading and building examples

[Lightbend Tech Hub](https://developer.lightbend.com/start/?group=play) offers a variety of Play examples for Java and Scala. We recommend trying the Hello World tutorial for Java or Scala first:

1. [Play Java Starter Example](https://developer.lightbend.com/start/?group=play&project=play-java-starter-example)
2. [Play Scala Starter Example](https://developer.lightbend.com/start/?group=play&project=play-scala-starter-example)

The downloadable zip files include everything you need to build and run the examples, including a distribution of the sbt and Gradle. Check out the `README.md` file in the top level project directory to learn more about the example.

## Using a project template

If you already have [sbt installed](https://www.scala-sbt.org/1.x/docs/Setup.html), you can use a [giter8](http://www.foundweekends.org/giter8/) template, similar to a Maven archetype, to create a new Play project. This gives you the advantage of setting up your project folders, build structure, and development environment - all with one command.

In a command window, enter one of the following lines to create a new project:

### Java template

```bash
sbt new playframework/play-java-seed.g8
```

### Scala template

```bash
sbt new playframework/play-scala-seed.g8
```

After the template creates the project:

1. Enter `sbt run` to download dependencies and start the system.
1. In a browser, enter <http://localhost:9000/> to view the welcome page.

## Play Example Projects

Play has many features, so rather than pack them all into one project, we've organized many example projects that showcase a feature or use case of Play so that you can see Play at work.

> **Note**: the example projects are not configured for out of the box security, and are intended to showcase particular areas of Play functionality.

See [Lightbend Tech Hub](https://developer.lightbend.com/start/?group=play) to get more details about how to use the download and use the example projects.
