<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Using the Play console

## Launching the console

The Play console is a development console based on sbt that allows you to manage a Play application’s complete development cycle.

To launch the Play console, change to the directory of your project, and run Activator:

```bash
$ cd my-first-app
$ activator
```

[[images/console.png]]

## Getting help

Use the `help` command to get basic help about the available commands.  You can also use this with a specific command to get information about that command:

```bash
[my-first-app] $ help run
```

## Running the server in development mode

To run the current application in development mode, use the `run` command:

```bash
[my-first-app] $ run
```

[[images/consoleRun.png]]

In this mode, the server will be launched with the auto-reload feature enabled, meaning that for each request Play will check your project and recompile required sources. If needed the application will restart automatically.

If there are any compilation errors you will see the result of the compilation directly in your browser:

[[images/errorPage.png]]

To stop the server, type `Crtl+D` key, and you will be returned to the Play console prompt.

## Compiling

In Play you can also compile your application without running the server. Just use the `compile` command:

```bash
[my-first-app] $ compile
```

[[images/consoleCompile.png]]

## Launch the interactive console

Type `console` to enter the interactive Scala console, which allows you to test your code interactively:

```bash
[my-first-app] $ console
```

To start application inside scala console (e.g. to access database):
```bash
scala> new play.core.StaticApplication(new java.io.File("."))
```

[[images/consoleEval.png]] 

## Debugging

You can ask Play to start a **JPDA** debug port when starting the console. You can then connect using Java debugger. Use the `activator -jvm-debug <port>` command to do that:

```
$ activator -jvm-debug 9999
```

When a JPDA port is available, the JVM will log this line during boot:

```
Listening for transport dt_socket at address: 9999
```

## Using sbt features

The Play console is just a normal sbt console, so you can use sbt features such as **triggered execution**. 

For example, using `~ compile`

```bash
[my-first-app] $ ~ compile
```

The compilation will be triggered each time you change a source file.

If you are using `~ run`

```bash
[my-first-app] $ ~ run
```

The triggered compilation will be enabled while a development server is running.

You can also do the same for `~ test`, to continuously test your project each time you modify a source file:

```bash
[my-first-app] $ ~ test
```

## Using the play commands directly

You can also run commands directly without entering the Play console. For example, enter `activator run`:

```bash
$ activator run
[info] Loading project definition from /Users/jroper/tmp/my-first-app/project
[info] Set current project to my-first-app (in build file:/Users/jroper/tmp/my-first-app/)

--- (Running the application from SBT, auto-reloading is enabled) ---

[info] play - Listening for HTTP on /0:0:0:0:0:0:0:0:9000

(Server started, use Ctrl+D to stop and go back to the console...)
```

The application starts directly. When you quit the server using `Ctrl+D`, you will come back to your OS prompt.
