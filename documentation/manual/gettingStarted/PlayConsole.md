<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->

# Using the sbt console

You can manage the complete development cycle of a Play application with [sbt](https://www.scala-sbt.org/). The sbt tool has an interactive mode or you can enter commands one at a time. Interactive mode can be faster over time because sbt only needs to start once. When you enter commands one at a time, sbt restarts each time you run it.

## Single commands

You can run single sbt commands directly. For example, to build and run Play, change to the directory of your project and run:

```bash
$ sbt run
```

You will see something like:

```bash
[info] Loading project definition from /Users/play-developer/my-first-app/project
[info] Set current project to my-first-app (in build file:/Users/play-developer/my-first-app/)

--- (Running the application from sbt, auto-reloading is enabled) ---

[info] play - Listening for HTTP on /0:0:0:0:0:0:0:0:9000

(Server started, use Enter to stop and go back to the console...)
The application starts directly. When you quit the server using Ctrl+D or Enter, the command prompt returns.
```

## Interactive mode

To launch sbt in interactive mode, change into the top level of your project and enter sbt with no arguments:

```bash
$ cd my-first-app
my-first-app $  sbt
```

And you will see something like:

```
[info] Loading global plugins from /Users/play-developer/.sbt/0.13/plugins
[info] Loading project definition from /Users/play-developer/my-first-app/project
[info] Updating {file:/Users/play-developer/my-first-app/project/}my-first-app-build...
[info] Resolving org.fusesource.jansi#jansi;1.4 ...
[info] Done updating.
[info] Set current project to my-first-app (in build file:/Users/play-developer/my-first-app/)
[my-first-app] $
```

## Development mode

In this mode, sbt launches Play with the auto-reload feature enabled. When you make a request, Play will automatically recompile and restart your server if any files have changed. If needed the application will restart automatically.

With sbt in interactive mode, run the current application in development mode, use the `run` command:

```bash
[my-first-app] $ run
```

And you will see something like:

```bash
$ sbt
[info] Loading global plugins from /Users/play-developer/.sbt/0.13/plugins
[info] Loading project definition from /Users/play-developer/my-first-app/project
[info] Set current project to my-first-app (in build file:/Users/play-developer/my-first-app/)
[my-first-app] $ run

--- (Running the application, auto-reloading is enabled) ---

[info] p.c.s.AkkaHttpServer - Listening for HTTP on /0:0:0:0:0:0:0:0:9000

(Server started, use Ctrl+D to stop and go back to the console...)
```


## Compiling only

You can also compile your application without running the HTTP server. The compile command displays any application errors in the command window. For example, in interactive mode, enter:

```bash
[my-first-app] $ compile
```

And you will see something like:

```bash
[my-first-app] $ compile
[info] Compiling 1 Scala source to /Users/play-developer/my-first-app/target/scala-2.11/classes...
[error] /Users/play-developer/my-first-app/app/controllers/HomeController.scala:21: not found: value Actionx
[error]   def index = Actionx { implicit request =>
[error]               ^
[error] one error found
[error] (compile:compileIncremental) Compilation failed
[error] Total time: 1 s, completed Feb 6, 2017 2:00:07 PM
[my-first-app] $
```

If there are no errors with your code, you will see:

```bash
[my-first-app] $ compile
[info] Updating {file:/Users/play-developer/my-first-app/}root...
[info] Resolving jline#jline;2.12.2 ...
[info] Done updating.
[info] Compiling 8 Scala sources and 1 Java source to /Users/play-developer/my-first-app/target/scala-2.11/classes...
[success] Total time: 3 s, completed Feb 6, 2017 2:01:31 PM
[my-first-app] $
```

## Testing options

You can run tests without running the server. For example, in interactive mode, use the `test` command

```bash
[my-first-app] $ test
```

The `test` commands will run all the tests in your project. You can also use `testOnly` to select specific tests:

```bash
[my-first-app] $ testOnly com.acme.SomeClassTest
```

## Launch the interactive console

Type `console` to enter the interactive Scala console, which allows you to test your code interactively:

```bash
[my-first-app] $ console
```

To start application inside scala console (e.g. to access database):

@[consoleapp](code/PlayConsole.scala)

## Debugging

You can ask Play to start a **JPDA** debug port when starting the console. You can then connect using Java debugger. Use the `sbt -jvm-debug <port>` command to do that:

```bash
$ sbt -jvm-debug 9999
```

When a JPDA port is available, the JVM will log this line during boot:

```bash
Listening for transport dt_socket at address: 9999
```

## Using sbt features

You can use sbt features such as **triggered execution**.

For example, using `~ compile`:

```bash
[my-first-app] $ ~ compile
```

The compilation will be triggered each time you change a source file.

If you are using `~ run`:

```bash
[my-first-app] $ ~ run
```

The triggered compilation will be enabled while a development server is running.

You can also do the same for `~ test`, to continuously test your project each time you modify a source file:

```bash
[my-first-app] $ ~ test
```

This could be especially useful if you want to run just a small set of your tests using `testOnly` command. For instance:

```bash
[my-first-app] $ ~ testOnly com.acme.SomeClassTest 
```

Will trigger the execution of `com.acme.SomeClassTest` test every time you modify a source file.

## Using the play commands directly

You can also run commands directly without entering the Play console. For example, enter `sbt run`:

```bash
$ sbt run
[info] Loading project definition from /Users/play-developer/my-first-app/project
[info] Set current project to my-first-app (in build file:/Users/play-developer/my-first-app/)

--- (Running the application from sbt, auto-reloading is enabled) ---

[info] play - Listening for HTTP on /0:0:0:0:0:0:0:0:9000

(Server started, use Enter to stop and go back to the console...)
```

The application starts directly. When you quit the server using `Ctrl+D` or `Enter`, you will come back to your OS prompt.

By default the server is bound to the default port 9000. A custom port can be port (e.g. 8080) can be specified: `sbt 'run 8080'`

Of course, the **triggered execution** is available here as well:

```bash
$ sbt ~run
```


## Getting help

Use the `help` command to get basic help about the available commands.  You can also use this with a specific command to get information about that command:

```bash
[my-first-app] $ help run
```