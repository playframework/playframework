# Configuring the Akka HTTP server backend

By default, Play uses the [[Akka HTTP server backend|AkkaHttpServer]].

like the rest of Play, the Akka HTTP server backend is configured with Typesafe Config.

@[](/confs/play-akka-http-server/reference.conf)

You can read more about the configuration settings in the [Akka HTTP documentation](http://doc.akka.io/docs/akka-http/current/scala/http/configuration.html).

> **Note:** In dev mode, when you use the `run` command, your `application.conf` settings will not be picked up by the server. This is because in dev mode the server starts before the application classpath is available. There are several [[other options|Configuration#Using-with-the-run-command]] you'll need to use instead.
