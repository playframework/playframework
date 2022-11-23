<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Allowed IP Filter

Play provides a filter which will check all HTTP requests if it's included in white list or black list. 

## Enabling the Allowed IP filter

To enable the filter, add it to `play.filters.enabled`:

```
play.filters.enabled += play.filters.ip.AllowedIPFilter
```

By default, the IP filter is disabled. To override this, set `play.filters.ip.enabled = true`.

## White List

If non empty, then requests will be checked if the IP is not in this list.

To set remote address to white list you can use `play.filters.ip.whiteList`.

## Black List

The black list is only used if the white list is empty.

To set remote address to black list you can use `play.filters.ip.blackList`.

## HTTP Status Code

The default HTTP status code for forbidden IP is `403 Forbidden`.

If you want to customize, you could set your own HTTP status using `play.filters.ip.httpStatuCode`.

## Route filter

If you don't want to check the HTTP request remote address to any route path you can use `+ noipcheck` in your route file.

```
+ noipcheck
GET /path1 MyController.foo

+ noipcheck
GET /path2 MyController.bar     
```

## All configurations

```
play.filters.ip.enabled = true
play.filters.ip.httpStatusCode = 403
play.filters.ip.whiteList = [ '192.168.0.1', '8f:f3b:0:0:0:0:0:ff' ]
play.filters.ip.blackList = [ '192.168.0.5', 'ff:ffb:0:0:0:0:0:ff' ]
```
