# Building on top of Play

_Note: this is a somewhat advanced topic, most users safely can ignore this_

While one can really take advantage of most play features while building a full application from scratch using play, it's very easy to drop play into existing sbt/maven projects and with just a little work use it as a REST/HTTP library. Below you can see how to do that.

# Rolling your own

Because play2's core is written in scala, the easiest way to provide a Java API is by creating the required API in scala. 

In practice it means that one needs to extend ```GobalSettings``` as ```Global``` in the global name space. ```GobalSettings``` has a method ``` def onRouteRequest(request: RequestHeader): Option[Handler] ``` that's handling play's routing. Once an alternative implementation is in place, play can be used as a REST/HTTP library! 

an annotation based basic implementation can be found [here](https://github.com/typesafehub/play2-mini/blob/master/src/main/scala/com/typesafe/play/mini/Setup.scala#L58) for an example.