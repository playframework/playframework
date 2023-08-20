<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# IP Filter

Play provides an IP filter which allows or denies access to resources based on a request's IP address, configured with white and black lists. 

## Enabling the IP filter

To enable the IP filter, add it to `application.conf`:

```
play.filters.enabled += play.filters.ip.IPFilter
```

## White listing IP addresses

If not empty, a request will only be allowed if its IP address exists in the whitelist.

```
# IPv4 and IPv6 adresses allowed in different notations
play.filters.ip.whiteList = [ "192.168.0.1", "8f:f3b:0:0:0:0:0:ff", "2001:cdba::3257:9652", "2001:cdba:0000:0000:0000:0000:3257:9653" ]
```

## Black listing IP addresses

The black list is only active if the white list is empty.  If not empty, a request will be denied if its IP address exists in the blacklist.

```
# The black list is only active if the whitelist is empty,
# otherwise it will be ignored.
# IPv4 and IPv6 adresses allowed in different notations
play.filters.ip.blackList = [ "192.168.0.1", "8f:f3b:0:0:0:0:0:ff", "2001:cdba::3257:9652", "2001:cdba:0000:0000:0000:0000:3257:9653" ]
```

## HTTP Status Code

The default HTTP status code for a forbidden request blocked by the IP filter is `403 Forbidden`.

You can customize the returned HTTP status using following config:

```
# 401 Unauthorized
play.filters.ip.accessDeniedHttpStatusCode = 401
```

## Selectively disabling the filter with Route Modifier

When using the default config Play ships with and you have a white- or blacklist defined, the IP filter will check the IP of every request.  There may be individual routes where you do not want the filter to apply, and the `anyip` route modifier may be used here, using the [[route modifier syntax|ScalaRouting#The-routes-file-syntax]].

In your `conf/routes` file:

```
+ anyip
GET     /path1         controllers.HomeController.myAction
```

This exclude the `GET /path1` route from the IP filter, always giving access to the route, no matter if the IP is white- or blacklisted at all.

The full range of configuration options available to the IP filter can be found in the Play Filters [`reference.conf`](resources/confs/play-filters-helpers/reference.conf). As you can see in this reference conf you can also reverse the route modifier behaviour by defining a route modifier blacklist only, meaning by default all routes are permissive and only specific routes may be checked by introducing e.g. a `checkip` blacklist route modifier.
