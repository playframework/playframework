## Play Framework - The High Velocity Web Framework 

The Play Framework combines productivity and performance making it easy to build scalable web applications with Java and Scala.  Play is developer friendly with a "just hit refresh" workflow and built-in testing support.  With Play, applications scale predictably due to a stateless and non-blocking architecture.  By being RESTful by default, including assets compilers, JSON & WebSocket support, Play is a perfect fit for modern web & mobile applications.

### Learn More
- [www.playframework.com](http://www.playframework.com)
- [Download](http://www.playframework.com/download)
- [Install](http://www.playframework.com/documentation/latest/Installing)
- [Get Started with Java](http://www.playframework.com/documentation/latest/JavaTodoList)
- [Get Started with Scala](http://www.playframework.com/documentation/latest/ScalaTodoList)
- [Build from source](http://www.playframework.com/documentation/latest/BuildingFromSource)
- [Search or create issues](https://github.com/playframework/playframework/issues)
- [Get help](http://stackoverflow.com/questions/tagged/playframework)
- [Contribute](http://www.playframework.com/documentation/latest/Guidelines)


## TLS differences

This branch differs from the main play branch in that it supports TLS client authentication as well
as server authentivation. The `RequestHeader` trait has a cert method that allows you to request a client
certificate asynchronously - the client will be asked for his certificate if he has one.

```scala
   def certs(required:Boolean): Future[Seq[Certificate]]
```

Certificates that are signed by CAs in the trust store can be requested, or by
specifying the empty trust store any client certificate will be requested. This
is useful for implementing protocols such as [WebID](http://webid.info/), as is done
by [RWW-Play](http://github.com/read-write-web/rww-play)

```scala
  run  -Dhttps.port=8443 -Dhttps.trustStore=noCA
```

This API is still prone to change, see:
* [ticket 828: client certificate support](https://play.lighthouseapp.com/projects/82401-play-20/tickets/828-client-certificate-support)
* [ticket 787: passing certificate-validation-level info to the client](https://play.lighthouseapp.com/projects/82401/tickets/787)

## Installing

For your convenience the most recent builds that pass the test suites [are uploaded here](http://bblfish.net/work/repo/builds/Play2/).

## Running the sample applications

There are several samples applications included in the `samples/` directory. For example, to run the included Scala Hello World application:

```bash
$ cd ~/workspace/play2.1/samples/scala/helloworld/
$ play run
```
> The application will be available on port 9000.

## Issues tracker

Report issues at https://github.com/playframework/Play20/issues.

## Contributors

Check for all contributors at https://github.com/playframework/Play20/contributors.

## Licence

This software is licensed under the Apache 2 license, quoted below.

Copyright 2013 Typesafe (http://www.typesafe.com).

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
