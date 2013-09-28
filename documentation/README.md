# Build documentation

This is the Play documentation project.  It does not build with the rest of the Play projects, and uses its own sbt
project instead.

## Dependencies

This project depends on [play-doc](http://github.com/playframework/play-doc].  If you want to tweak the format or change the includes, you should do so there.

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

Any directory under /manual called "code" is treated as a root of a test directory.  You can put configuration files, Java files, or Scala files in there.  Source files do not have to be part of a test suite, but it is highly encouraged to ensure that all included code snippets can compile and pass some internal checks.

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

There is no distinct packaging of HTML files in the project.  Instead, the main project has a `/project/Docs` SBT file that will package the documentation with the rest of the application.

```
cd $PLAY_HOME/framework
./build compile doc package
```

All Play projects can see documentation embedded by going to [http://localhost:9000/@documentation](http://localhost:9000/@documentation).  Internally, the @documentation route goes to `DocumentationServer` in the play-docs subproject, which relies on [play-doc](http://github.com/playframework/play-s] for generating HTML from the raw Markdown.  

## Running

You can run a built-in documentation server directly from the documentation project, without packaging.  This will give you quick access to the documentation without having to build everything.  It is available at [http://localhost:9000](http://localhost:9000).

```
cd documentation
./build run
```
