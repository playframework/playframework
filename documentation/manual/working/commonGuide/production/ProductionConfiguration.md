<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Production Configuration

There are a number of different types of configuration that you can configure in production.  The three mains types are:

* [General configuration](#General-configuration)
* [Logging configuration](#Logging-configuration)
* [JVM configuration](#JVM-configuration)

Each of these types have different methods to configure them.

## General configuration

Play has a number of configurable settings. You can configure database connection URLs, the application secret, the HTTP port, SSL configuration, and so on.

Most of Play's configuration is defined in various `.conf` files, which use the [HOCON format](https://github.com/typesafehub/config/blob/master/HOCON.md). The main configuration file that you'll use is the `application.conf` file. You can find this file at `conf/application.conf` within your project. The `application.conf` file is loaded from the classpath at runtime (or you can override where it is loaded from). There can only be one `application.conf` per project.

Other `.conf` files are loaded too. Libraries define default settings in `reference.conf` files. These files are stored in the libraries' JARs—one `reference.conf` per JAR—and aggregated together at runtime. The `reference.conf` files provide defaults; they are overridden by any settings defined in the `application.conf` file.

Play's configuration can also be defined using system properties and environment variables. This can be handy when settings change between environments; you can use the `application.conf` for common settings, but use system properties and environment variables to change settings when you run the application in different environments.

System properties override settings in `application.conf`, and `application.conf` overrides the default settings in the various `reference.conf` files.

You can override runtime configuration in several ways. This can be handy when settings vary between environments; you can changing the configuration dynamically for each environment. Here are your choices for runtime configuration:

* Using an alternate `application.conf` file.
* Overriding individual settings using system properties.
* Injecting configuration values using environment variables.

### Specifying an alternate configuration file

The default is to load the `application.conf` file from the classpath. You can specify an alternative configuration file if needed:

#### Using `-Dconfig.resource`

This will search for an alternative configuration file in the application classpath (you usually provide these alternative configuration files into your application `conf/` directory before packaging). Play will look into `conf/` so you don't have to add `conf/`.

```
$ /path/to/bin/<project-name> -Dconfig.resource=prod.conf
```

#### Using `-Dconfig.file`

You can also specify another local configuration file not packaged into the application artifacts:

```
$ /path/to/bin/<project-name> -Dconfig.file=/opt/conf/prod.conf
```

> Note that you can always reference the original configuration file in a new `prod.conf` file using the `include` directive, such as:
>
> ```
> include "application.conf"
>
> key.to.override=blah
> ```

### Overriding configuration with system properties

Sometimes you don't want to specify another complete configuration file, but just override a bunch of specific keys. You can do that by specifying then as Java System properties:

```
$ /path/to/bin/<project-name> -Dplay.http.secret.key=abcdefghijk -Ddb.default.password=toto
```

#### Specifying the HTTP server address and port using system properties

You can provide both HTTP port and address easily using system properties. The default is to listen on port `9000` at the `0.0.0.0` address (all addresses).

```
$ /path/to/bin/<project-name> -Dhttp.port=1234 -Dhttp.address=127.0.0.1
```

#### Changing the path of RUNNING_PID

It is possible to change the path to the file that contains the process id of the started application. Normally this file is placed in the root directory of your play project, however it is advised that you put it somewhere where it will be automatically cleared on restart, such as `/var/run`:

```
$ /path/to/bin/<project-name> -Dpidfile.path=/var/run/play.pid
```

> Make sure that the directory exists and that the user that runs the Play application has write permission for it.

Using this file, you can stop your application using the `kill` command, for example:

```
$ kill $(cat /var/run/play.pid)
```

To prevent Play from creating it's own PID, you can set the path to `/dev/null` in your `application.conf` file:

```
pidfile.path = "/dev/null"
```

### Using environment variables

You can also reference environment variables from your `application.conf` file:

```
my.key = defaultvalue
my.key = ${?MY_KEY_ENV}
```

Here, the override field `my.key = ${?MY_KEY_ENV}` simply vanishes if there's no value for `MY_KEY_ENV`, but if you set an environment variable `MY_KEY_ENV` for example, it would be used.

### Server configuration options

Play's default HTTP server implementation is Akka HTTP, and this provides a large number of ways to tune and configure the server, including the size of parser buffers, whether keep alive is used, and so on.

A full list of server configuration options, including defaults, can be seen here:

@[](/confs/play-akka-http-server/reference.conf)

You can also use Netty as the HTTP server, which also provides its own configurations. A full list of Netty server configuration, including the defaults, can be seen below:

@[](/confs/play-netty-server/reference.conf)

> **Note**: The Netty server backend is not the default in 2.6.x, and so must be specifically enabled.

## Logging configuration

Logging can be configured by creating a logback configuration file.  This can be used by your application through the following means:

### Bundling a custom logback configuration file with your application

Create an alternative logback config file called `logback.xml` and copy that to `<app>/conf`

You can also specify another logback configuration file via a System property. Please note that if the configuration file is not specified then play will use the default `logback.xml` that comes with play in the production mode. This means that any log level settings in `application.conf` file will be overridden. As a good practice always specify your `logback.xml`.

### Using `-Dlogger.resource`

Specify another logback configuration file to be loaded from the classpath:

```
$ /path/to/bin/<project-name> -Dlogger.resource=prod-logger.xml
```

### Using `-Dlogger.file`

Specify another logback configuration file to be loaded from the file system:

```
$ /path/to/bin/<project-name> -Dlogger.file=/opt/prod/prod-logger.xml
```

### Using `-Dlogger.url`

Specify another logback configuration file to be loaded from an URL:

```
$ /path/to/bin/<project-name> -Dlogger.url=http://conf.mycompany.com/logger.xml
```

> **Note**: To see which file is being used, you can set a system property to debug it: `-Dlogback.debug=true`.

## JVM configuration

You can specify any JVM arguments to the application startup script. Otherwise the default JVM settings will be used:

```
$ /path/to/bin/<project-name> -J-Xms128M -J-Xmx512m -J-server
```
