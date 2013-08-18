# Build documentation

This is the Play documentation project.  It does not build with the rest of the Play document, and has its own sbt
project.

## Dependencies

This project depends on [play-doc](http://github.com/playframework/play-doc].  If you want to tweak the format or change the includes, you should do so there.

## Output

The output from this project is pushed to [play-doc](http://github.com/playframework/play-generated-docs], and eventually appears on http://www.playframework.com/documentation/2.2-SNAPSHOT/Home
(depending on the Play version).

## Existing Docs

All the documentation is under the /manual folder, and is in [Markdown](http://daringfireball.net/projects/markdown/syntax) format with an extension that looks like this

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

Any directory under /manual called "code" is treated as a root of a test directory.  You can put configuration files, Java files, or Scala files in there.  Source files do not have to be part of a test suite, but it is highly encouraged to ensure that all included code snippets both compile and pass.

## IDE integration

There is no out of the box integration, but you can use the [sbt-idea](https://github.com/mpeltonen/sbt-idea) plugin or the [Eclipse](https://github.com/typesafehub/sbteclipse) plugin to generate a project for you.

NOTE: if you use sbt-idea, the generated project defines "com.typesafe.play" and other libraries as "runtime" and so does not expose the included libraries to the IDE.  Setting them to "test" manually makes everything work.

## Testing

You can run the test suite for the documentation using:

```
./build
> test
```

## Packaging

```
./build package
```

## Publishing

```
./build publish-local
```

## Running

You can run a built-in documentation server.  This will give you quick access to the documentation without having
to build everything.  It is available at http://localhost:9000 by default.

```
./build run
```