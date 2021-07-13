<!--- Copyright (C) Lightbend Inc. <https://www.lightbend.com> -->
# Allowed IP Filter

Play provides a filter which will check all HTTP requests if it's included in allow list.
If the HTTP request isn't allowed, then we'll return `403 Forbidden`.

## Enabling the Allowed IP filter

To enable the filter, add it to `play.filters.enabled`:

```
play.filters.enabled += play.filters.ip.AllowedIPFilter
```

By default, the IP filter is enabled. To override this, set `play.filters.ip.enabled = false`.

## Allow List

To set remote address to allow list you can use `play.filters.ip.allowList`.

```
play.filters.ip.allowList = [ '192.168.0.1', '127.0.0.1' ]
```

If you don't want to check the HTTP request remote address to any route path you can use `+ noipcheck` in your route file.

```
+ noipcheck
GET /path1 MyController.foo

+ noipcheck
GET /path2 MyController.bar     
```