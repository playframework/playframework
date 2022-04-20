<!--- Copyright (C) Lightbend Inc. <https://www.lightbend.com> -->

# Build documentation

This is the README for the Play documentation project.  The documentation project does not build with the rest of the Play projects, and uses its own sbt setup instead.  Please refer to the [main README file](../README.md) for how to build Play in general and how to [contribute](../CONTRIBUTING.md).

If you are completely lost and want to run Play for the first time, go to the [download page](https://www.playframework.com/download) and pick out a starter project: the starter projects are explicitly written for new users and are the best place to start.

## Dependencies

This project depends on [play-doc](https://github.com/playframework/play-doc).  If you want to tweak the format or change the includes, you should do so there.

## Existing Docs

All the documentation is under the /manual folder, and is in [Markdown](https://daringfireball.net/projects/markdown/syntax) format with an extension that looks like this

    @[label](some/relative/path)

Code snippets are identified using the hash symbol prepended to the label, like this:

    //#label
    println("Hello world")
    //#label

As an example, if you open up `main/akka/JavaAkka.md` then you'll see:

    @[actor-for](code/javaguide/akka/JavaAkka.java)

which refers to the code snippet in the file code/javaguide/JavaAkka.java (note it is relative to the akka directory):

    //#actor-for
    ActorRef myActor = Akka.system().actorOf(new Props(MyActor.class));
    //#actor-for

And this code snippet is included in the generated documentation.

## Code

Any directory under /manual called "code" is treated as a root of a test directory.  You can put configuration files, Java files, or Scala files in there.  Source files do not have to be part of a test suite, but it is highly encouraged to ensure that all included code snippets can compile and pass some internal checks.

All documentation code samples must be adequately namespaced.  For example, no code samples should ever create a class called "controllers.Application", nor should any code samples ever create a routes file called "routes", they should instead namespace them, eg something like "javaguide.async.routes".

For more information, please see the [Play documentation guidelines](https://www.playframework.com/documentation/latest/Documentation).

## IDE integration

There is no out of the box integration, but you can use IntelliJ IDEA's [Scala plugin](https://blog.jetbrains.com/scala/) or the [Eclipse](https://github.com/typesafehub/sbteclipse) plugin to generate a project for you.  If you are using IntelliJ IDEA, the [Markdown Support](https://www.jetbrains.com/help/idea/markdown.html) plugin will make editing documentation much easier.

## Testing

Before you run the tests make sure you have the latest snapshot version of the Play library in your local repository. This can be achieved through:

```
(cd .. && sbt publishLocal)
```

You can run the test suite for the documentation using:

```
sbt
> test
```

## Validating

You can validate the integrity of the documentation internal links by calling:

```
sbt
> validateDocs
```

To validate the availability of the external links use:

```
sbt
> validateExternalLinks
```

## Packaging

There is no distinct packaging of HTML files in the project.  Instead, the main project has a `/project/Docs` sbt file that will package the documentation with the rest of the application.

```
cd $PLAY_HOME
sbt compile doc package
```

## Running

You can run a built-in documentation server directly from the documentation project, without packaging.  This will give you quick access to the documentation without having to build everything.  It is available at [http://localhost:9000](http://localhost:9000).

```
cd documentation
sbt run
```
