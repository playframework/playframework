<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Creating a New Application

Play expects a specific [[project structure|Anatomy]]. If you already have [sbt installed](https://www.scala-sbt.org/1.x/docs/Setup.html), you can use a [giter8](http://www.foundweekends.org/giter8/) template, similar to a Maven archetype, to create a new Play project. This gives you the advantage of setting up your project folders, build structure, and development environment — all with one command.

In a command window, enter the following line to create a new project:

```bash
sbt new
```

In the interactive menu choose:

- `playframework/play-scala-seed.g8` for a Play Scala project
- `playframework/play-java-seed.g8` for a Play Java project

Then respond to the prompts. After the template creates the project:

1. Change into the top level project directory.
1. Enter `sbt run` to download dependencies and start the system.
1. In a browser, enter <http://localhost:9000/> to view the welcome page.

### Using the Java template directly instead

Enter:

```bash
sbt new playframework/play-java-seed.g8
```

To create a Play Java 2.9 project:

```bash
sbt new playframework/play-java-seed.g8 --branch 2.9.x
```

### Using the Scala template directly instead

Enter:

```bash
sbt new playframework/play-scala-seed.g8
```

To create a Play Scala 2.9 project:

```bash
sbt new playframework/play-scala-seed.g8 --branch 2.9.x
```

> Besides replacing Akka with Pekko, Play 3.0 is identical to Play 2.9. If you don't know what Akka or Pekko is, we recommend using Play 3.0. For more details on which version to use, read "[[How Play Deals with Akka’s License Change|General#How-Play-Deals-with-Akkas-License-Change]]".