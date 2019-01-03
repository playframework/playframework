<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Configuring gzip encoding

Play provides a gzip filter that can be used to gzip responses.

## Enabling the gzip filter

To enable the gzip filter, add the filter to `application.conf`:

```
play.filters.enabled += "play.filters.gzip.GzipFilter"
```

## Configuring the gzip filter

The gzip filter supports a small number of tuning configuration options, which can be configured from `application.conf`.  To see the available configuration options, see the Play filters [`reference.conf`](resources/confs/filters-helpers/reference.conf).

## Controlling which responses are gzipped

You can control which responses are and aren't gzipped based on their content types via `application.conf`:

```
play.filters.gzip {

    contentType {

        # If non empty, then a response will only be compressed if its content type is in this list.
        whiteList = [ "text/*", "application/javascript", "application/json" ]

        # The black list is only used if the white list is empty.
        # Compress all responses except the ones whose content type is in this list.
        blackList = []
    }
}
```

As a more flexible alternative you can use the `shouldGzip` parameter of the gzip filter itself, which accepts a function of a request header and a response header to a boolean.

For example, the code below only gzips HTML responses:

Scala
: @[should-gzip](code/GzipEncoding.scala)

Java
: @[gzip-filter](code/detailedtopics/configuration/gzipencoding/CustomFilters.java)
