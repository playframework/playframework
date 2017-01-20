<!--- Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com> -->
# Redirect plain filter

Play provides a filter which will redirect plain requests to secure requests.

## Enabling the redirect plain filter

To enable the filter, add the Play filters project to your `libraryDependencies` in `build.sbt`:

@[content](code/filters.sbt)

Now add the filter to your filters, which is typically done by creating a `Filters` class in the root of your project:

Scala
: @[filters](code/RedirectPlainFilter.scala)

Java
: @[filters](code/detailedtopics/configuration/redirectplain/Filters.java)

## Configuring the redirect plain filter

The only thing you can configure is the `Strict-Transport-Security` header value:

`play.filters.redirectplain.strict-transport-security.max-age = 86400`
