<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->

# Creating a New Application

Play expects a specific project structure. If you already have [sbt installed](https://www.scala-sbt.org/1.x/docs/Setup.html), you can use a [giter8](http://www.foundweekends.org/giter8/) template, similar to a Maven archetype, to create a new Play project. This gives you the advantage of setting up your project folders, build structure, and development environment - all with one command.

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


