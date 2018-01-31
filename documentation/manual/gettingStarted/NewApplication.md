<!--- Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com> -->
# Creating a new application

## Using Play Starter Projects

If you've never used Play before, then you can [download a starter project](https://playframework.com/download#starters). The starter projects have lots of comments explaining how everything works and have links to documentation that goes more in depth.

If you download and unzip one of the .zip files [at the starter projects](https://playframework.com/download#starters), you'll see the `sbt` executable file -- this is a packaged version of [sbt](https://www.scala-sbt.org), the build tool Play uses. If you're on Windows, you need to use `sbt.bat` instead.

See [our download page](https://playframework.com/download#starters) to get more details about how to use the starter projects.

## Create a new application using SBT

If you have [sbt 0.13.13 or higher](https://www.scala-sbt.org) installed, you can create your Play project using `sbt new` using a minimal [giter8](http://foundweekends.org/giter8) template (roughly like a maven archetype). This is a good choice if you already know Play and want to create a new project immediately.

> **Note**: If running Windows, you may need to run sbt using `sbt.bat` instead of `sbt`. This documentation assumes the command is `sbt`.

Note that the seed templates are already configured with [[CSRF|ScalaCsrf]] and [[security headers filters|SecurityHeaders]], whereas the other projects are not explicitly set up for security out of the box.

### Play Java Seed

```bash
sbt new playframework/play-java-seed.g8
```

### Play Scala Seed

```bash
sbt new playframework/play-scala-seed.g8
```

After that, use `sbt run` and then go to <http://localhost:9000> to see the running server.

You can also [create your own giter8 seeds](http://www.foundweekends.org/giter8/usage.html) and based off this one by forking from the <https://github.com/playframework/play-java-seed.g8> or <https://github.com/playframework/play-scala-seed.g8> GitHub projects.

## Play Example Projects

Play has many features, so rather than pack them all into one project, we've organized many example projects that showcase a feature or use case of Play so that you can see Play at work.

> **Note**: the example projects are not configured for out of the box security, and are intended to showcase particular areas of Play functionality.

See [our download page](https://playframework.com/download#examples) to get more details about how to use the download and use the example projects.
