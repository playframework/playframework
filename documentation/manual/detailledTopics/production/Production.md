# Starting your application in production mode

There are several ways to deploy a Play application in production mode. Let's start by using the simplest way, using a local Play installation.

## Using the start command

The easiest way to start an application in production mode is to use the `start` command from the Play console. This requires a Play 2.0 installation on the server.

```bash
[My first application] $ start
```

> Note that the `run` command is only for development mode and should never be used to run an application in production. For each request a complete check is handled by sbt.

[[images/start.png]]

When you run the `start` command, Play forks a new JVM and runs the default Netty HTTP server. The standard output stream is redirected to the Play console, so you can monitor its status.

> The server’s process id is displayed at bootstrap and written to the `RUNNING_PID` file. To kill a running Play server, it is enough to send a `SIGTERM` to the process to properly shutdown the application.

If you type `Ctrl+D`, the Play console will quit, but the created server process will continue running in background. The forked JVM’s standard output stream is then closed, and logging can be read from the `logs/application.log` file.

If you type `Ctrl+C`, you will kill both JVMs: the Play console and the forked Play server. 

Alternatively you can directly use `play start` at your OS command prompt, which does the same thing:

```bash
$ play start
```

> Note: the HTTP port can be set by passing -Dhttp.port system variable

## Using the stage task

The problem with the `start` command is that it starts the application interactively, which means that human interaction is needed, and `Ctrl+D` is required to detach the process. This solution is not really convenient for automated deployment.

You can use the `stage` task to prepare your application to be run in place. The typical command for preparing a project to be run in place is:

```bash
$ play clean compile stage
```

[[images/stage.png]]

This cleans and compiles your application, retrieves the required dependencies and copies them to the `target/staged` directory. It also creates a `target/start` script that runs the Play server.

You can start your application using:

```bash
$ target/start
```

The generated `start` script is very simple - in fact, you could even execute the `java` command directly.

If you don’t have Play installed on the server, you can use sbt to do the same thing:

```bash
$ sbt clean compile stage
```