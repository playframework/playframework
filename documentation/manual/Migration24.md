<!--- Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com> -->
# Play 2.4 Migration Guide

This guide is for migrating to Play 2.4 from Play 2.3. To migrate to Play 2.3, first follow the [[Play 2.3 Migration Guide|Migration23]].

## Body Parsers

The default body parser is now `play.api.mvc.BodyParsers.parse.default`. It is similar to `anyContent` parser, except that it only parses the bodies of PATCH, POST, and PUT requests. To parse bodies for requests of other methods, explicitly pass the `anyContent` parser to `Action`.

```scala
def foo = Action(play.api.mvc.BodyParsers.parse.anyContent) { request =>
  Ok(request.body.asText)
}
```
