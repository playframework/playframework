<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Configuring gzip encoding

Play provides a gzip filter that can be used to gzip responses.

## Enabling the gzip filter

To enable the gzip filter, add the Play filters project to your `libraryDependencies` in `build.sbt`:

@[content](code/filters.sbt)

Now add the gzip filter to your filters, which is typically done by creating a `Filters` class in the root of your project:

Scala
: @[filters](code/GzipEncoding.scala)

Java
: @[filters](code/detailedtopics/configuration/gzipencoding/Filters.java)

## Configuring the gzip filter

The gzip filter supports a small number of tuning configuration options, which can be configured from `application.conf`.  To see the available configuration options, see the Play filters [`reference.conf`](resources/confs/filters-helpers/reference.conf).

## Controlling which responses are gzipped

To control which responses are and aren't implemented, use the `shouldGzip` parameter, which accepts a function of a request header and a response header to a boolean.

For example, the code below only gzips HTML responses:

@[should-gzip](code/GzipEncoding.scala)
