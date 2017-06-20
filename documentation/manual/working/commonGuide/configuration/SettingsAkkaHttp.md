# Configuring the Akka HTTP server backend

By default, Play uses the [[Akka HTTP server backend|AkkaHttpServer]].

Like the rest of Play, the Akka HTTP server backend is configured with Typesafe Config.

@[](/confs/play-akka-http-server/reference.conf)

The configurations above are specific to Akka HTTP server backend, but other more generic configurations are also available:
 
@[](/confs/play-server/reference.conf)

You can read more about the configuration settings in the [Akka HTTP documentation](http://doc.akka.io/docs/akka-http/current/scala/http/configuration.html).

> **Note:** Akka HTTP has a number of [timeouts configurations](http://doc.akka.io/docs/akka-http/10.0.7/scala/http/common/timeouts.html#server-timeouts) that you can use to protect your application from attacks or programming mistakes. The Akka HTTP Server in Play will automatically recognize all these Akka configurations. For example, if you have `idle-timeout` and `request-timeout` configurations like below:
>
> ```
> akka.http.server.idle-timeout = 20s
> akka.http.server.request-timeout = 30s
> ```
>
> They will be automatically recognized. Keep in mind that Play configurations listed above will override the Akka ones.

There is also a separate configuration file for the HTTP/2 support in Akka HTTP, if you have enabled the `AkkaHttp2Support` plugin:

@[](/confs/play-akka-http2-support/reference.conf)

> **Note:** In dev mode, when you use the `run` command, your `application.conf` settings will not be picked up by the server. This is because in dev mode the server starts before the application classpath is available. There are several [[other options|Configuration#Using-with-the-run-command]] you'll need to use instead.
