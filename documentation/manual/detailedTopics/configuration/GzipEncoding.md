<!--- Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com> -->
# Configuring gzip encoding

Play provides a gzip filter that can be used to gzip responses.  It can be added to the applications filters using the `Global` object. To enable the gzip filter, add the Play filters helpers dependency to your project in `build.sbt`:

```scala
libraryDependencies += filters
```

## Enabling gzip in Scala

The simplest way to enable the gzip filter in a Scala project is to use the `WithFilters` helper:

@[global](code/GzipEncoding.scala)

To control which responses are and aren't implemented, use the `shouldGzip` parameter, which accepts a function of a request header and a response header to a boolean.

For example, the code below only gzips HTML responses:

@[should-gzip](code/GzipEncoding.scala)

## Enabling GZIP in Java

To enable gzip in Java, add it to the list of filters in the `Global` object:

@[global](code/detailedtopics/configuration/gzipencoding/Global.java)
