<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Configuring the Pekko HTTP server backend

By default, Play uses the [[Pekko HTTP server backend|PekkoHttpServer]].

Like the rest of Play, the Pekko HTTP server backend is configured with Typesafe Config.

@[](/confs/play-pekko-http-server/reference.conf)

The configurations above are specific to Pekko HTTP server backend, but other more generic configurations are also available:
 
@[](/confs/play-server/reference.conf)

You can read more about the configuration settings in the [Pekko HTTP documentation](https://pekko.apache.org/docs/pekko-http/1.0/configuration.html?language=scala).

> **Note:** Pekko HTTP has a number of [timeouts configurations](https://pekko.apache.org/docs/pekko-http/1.0/common/timeouts.html?language=scala#server-timeouts) that you can use to protect your application from attacks or programming mistakes. The Pekko HTTP Server in Play will automatically recognize all these Pekko configurations. For example, if you have `idle-timeout` and `request-timeout` configurations like below:
>
> ```
> pekko.http.server.idle-timeout = 30s
> pekko.http.server.request-timeout = 20s
> ```
>
> They will be automatically recognized. Keep in mind that Play configurations listed above will override the Pekko ones.
>
> When setting the request-timeout, make sure it is smaller than the idle-timeout. Otherwise the idle-timeout will kick in first and reset the TCP connection without a response.

There is also a separate configuration file for the HTTP/2 support in Pekko HTTP, if you have [[enabled the `PekkoHttp2Support` plugin|PekkoHttpServer#HTTP/2-support-(incubating)]]:

@[](/confs/play-pekko-http2-support/reference.conf)

> **Note:** In dev mode, when you use the `run` command, your `application.conf` settings will not be picked up by the server. This is because in dev mode the server starts before the application classpath is available. There are several [[other options|ConfigFile#Using-with-the-run-command]] you'll need to use instead.

## Direct Pekko HTTP configuration

If you need direct access to Pekko HTTP's `ServerSettings` and `ParserSettings` objects you can do this by extending Play's `PekkoHttpServer` class with your own. The `PekkoHttpServer` class has several protected methods which can be overridden to change how Play configures its Pekko HTTP backend.

Note that writing your own server class is advanced usage. Usually you can do all the configuration you need through normal configuration settings.

The code below shows an example of a custom server which modifies some Pekko HTTP settings. Below the server class is a `ServerProvider` class which acts as a factory for the custom server.

@[custom-pekko-http-server](code/CustomPekkoHttpServer.scala)

Once you've written a custom server and `ServerProvider` class you'll need to tell Play about them by setting the `play.server.provider` configuration option to the full name of your `ServerProvider` class.

For example, adding the following settings to your `build.sbt` and `application.conf` will tell Play to use your new server for both the sbt `run` task and when your application is deployed.

`build.sbt`:

@[custom-pekko-http-server-provider](code/build.sbt)

`application.conf`:

@[custom-pekko-http-server-provider](code/application.conf)
