<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Configuring the Akka HTTP server backend

By default, Play uses the [[Akka HTTP server backend|AkkaHttpServer]].

Like the rest of Play, the Akka HTTP server backend is configured with Typesafe Config.

@[](/confs/play-akka-http-server/reference.conf)

The configurations above are specific to Akka HTTP server backend, but other more generic configurations are also available:
 
@[](/confs/play-server/reference.conf)

You can read more about the configuration settings in the [Akka HTTP documentation](https://doc.akka.io/docs/akka-http/current/configuration.html?language=scala).

> **Note:** Akka HTTP has a number of [timeouts configurations](https://doc.akka.io/docs/akka-http/current/common/timeouts.html?language=scala#server-timeouts) that you can use to protect your application from attacks or programming mistakes. The Akka HTTP Server in Play will automatically recognize all these Akka configurations. For example, if you have `idle-timeout` and `request-timeout` configurations like below:
>
> ```
> akka.http.server.idle-timeout = 20s
> akka.http.server.request-timeout = 30s
> ```
>
> They will be automatically recognized. Keep in mind that Play configurations listed above will override the Akka ones.

There is also a separate configuration file for the HTTP/2 support in Akka HTTP, if you have [[enabled the `AkkaHttp2Support` plugin|AkkaHttpServer#HTTP/2-support-(experimental)]]:

@[](/confs/play-akka-http2-support/reference.conf)

> **Note:** In dev mode, when you use the `run` command, your `application.conf` settings will not be picked up by the server. This is because in dev mode the server starts before the application classpath is available. There are several [[other options|ConfigFile#Using-with-the-run-command]] you'll need to use instead.

## Direct Akka HTTP configuration

If you need direct access to Akka HTTP's `ServerSettings` and `ParserSettings` objects you can do this by extending Play's `AkkaHttpServer` class with your own. The `AkkaHttpServer` class has several protected methods which can be overridden to change how Play configures its Akka HTTP backend.

Note that writing your own server class is advanced usage. Usually you can do all the configuration you need through normal configuration settings.

The code below shows an example of a custom server which modifies some Akka HTTP settings. Below the server class is a `ServerProvider` class which acts as a factory for the custom server.

@[custom-akka-http-server](code/CustomAkkaHttpServer.scala)

Once you've written a custom server and `ServerProvider` class you'll need to tell Play about them by setting the `play.server.provider` configuration option to the full name of your `ServerProvider` class.

For example, adding the following settings to your `build.sbt` and `application.conf` will tell Play to use your new server for both the sbt `run` task and when your application is deployed.

`build.sbt`:

@[custom-akka-http-server-provider](code/build.sbt)

`application.conf`:

@[custom-akka-http-server-provider](code/application.conf)