<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Managing library dependencies

> **Note:** Some sections of this page were copied from the sbt manual, specifically from the [Library Dependencies](https://www.scala-sbt.org/0.13/docs/Library-Dependencies.html) page. You can refer to that page for a more detailed and updated version of the information here.

## Unmanaged dependencies

Most people end up using managed dependencies - which allows for fine-grained control, but unmanaged dependencies can be simpler when starting out.

Unmanaged dependencies work like this: create a `lib/` directory in the root of your project and then add jar files to that directory. They will automatically be added to the application classpath. There's not much else to it.

There's nothing to add to `build.sbt` to use unmanaged dependencies, although you could change a configuration key if you'd like to use a directory different than `lib`.

## Managed dependencies

Play uses [Apache Ivy](http://ant.apache.org/ivy/) (via sbt) to implement managed dependencies, so if you're familiar with Maven or Ivy, you are already used to managed dependencies.

Most of the time you can simply list your dependencies in the `build.sbt` file.

Declaring a dependency looks like this (defining `group`, `artifact` and `revision`):

@[single-dep](code/dependencies.sbt)

Or like this, with an optional `configuration`:

@[single-dep-test](code/dependencies.sbt)

Multiple dependencies can be added either by multiple declarations like the above, or you can provide a Scala sequence:

@[multi-deps](code/dependencies.sbt)

Of course, sbt (via Ivy) has to know where to download the module. If your module is in one of the default repositories sbt comes with then this will just work.

### Getting the right Scala version with `%%`

If you use `groupID %% artifactID % revision` rather than `groupID % artifactID % revision` (the difference is the double `%%` after the `groupID`), sbt will add your project's Scala version to the artifact name. This is just a shortcut. You could write this without the `%%`:

@[explicit-scala-version-dep](code/dependencies.sbt)

Assuming the `scalaVersion` for your build is `2.11.1`, the following is identical (note the double `%%` after `"org.scala-tools"`):

@[auto-scala-version-dep](code/dependencies.sbt)

The idea is that many dependencies are compiled for multiple Scala versions, and you'd like to get the one that matches your project to ensure binary compatibility.

### Resolvers

sbt uses the standard Maven2 repository and the Typesafe Releases (<https://repo.typesafe.com/typesafe/releases>) repositories by default. If your dependency isn't on one of the default repositories, you'll have to add a resolver to help Ivy find it.

Use the `resolvers` setting key to add your own resolver. For example:

@[resolver](code/dependencies.sbt)

sbt can search your local Maven repository if you add it as a repository:

@[local-maven-repos](code/dependencies.sbt)

## Handling conflicts between dependencies

sbt has extensive documentation about how to manage conflict between your dependencies:

[sbt: Dependencies Conflict Management](https://www.scala-sbt.org/0.13/docs/Library-Management.html#Conflict+Management)

You can also use [sbt-dependency-graph](https://github.com/jrudolph/sbt-dependency-graph) to have a better visualization of your dependency tree. See also our page about [[debugging sbt|sbtDebugging]] common problems.
