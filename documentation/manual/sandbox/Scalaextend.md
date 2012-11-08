# Building on top of Play

_Note: this is a somewhat advanced topic, most users safely can ignore this_

While one can really take advantage of most play features while building a full application from scratch using play, it's very easy to drop play into existing sbt/maven projects and with just a little work use it as a REST/HTTP library. Below you can see how to do that.

# Rolling your own

A ```Global``` object extending ```GlobalSettings``` in the global name space is the key wiring point. GlobalSettings has a method ``` def onRouteRequest(request: RequestHeader): Option[Handler] ``` that's handling play's routing. 

One could easily provide an alternative implementation and start using play as a REST library! 

See [this](https://github.com/typesafehub/play2-mini/blob/master/src/main/scala/com/typesafe/play/mini/Setup.scala) for an example.