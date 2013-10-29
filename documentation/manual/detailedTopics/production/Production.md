# Starting your application in production mode

There are several ways to deploy a Play application in production mode. Let's start by using the simplest way, using a local Play installation.

> Note, different play apps require different configuration in production. The default configuration is setup for apps that are mainly processing requests asynchronously. If your app is executing mainly blocking calls, then it's recommended to increase the number of available threads and the timeouts.

## Using the start command

The easiest way to start an application in production mode is to use the `start` command from the Play console. This requires a Play installation on the server.

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

The `start` command starts the application interactively, which means that human interaction is needed, and `Ctrl+D` is required to detach the process. This solution is not really convenient for automated deployment.

You can use the `stage` task to prepare your application to be run in place. The typical command for preparing a project to be run in place is:

```bash
$ play clean stage
```
[[images/stage.png]]

This cleans and compiles your application, retrieves the required dependencies and copies them to the `target/universal/stage` directory. It also creates a `bin/<start>` script where `<start>` is the project's name. The script runs the Play server on Unix style systems and there is also a corresponding `bat` file for Windows.

For example to start an application of the project 'foo' from the project folder you can:

```bash
$ target/universal/stage/bin/foo
```

You can also specify a different configuration file for a production environment, from the command line:

```bash
$ target/universal/stage/bin/foo -Dconfig.file=/full/path/to/conf/application-prod.conf
```

For a full description of usage invoke the start script with a "-h" option.

> **Next:** [[Creating a standalone distribution|ProductionDist]]