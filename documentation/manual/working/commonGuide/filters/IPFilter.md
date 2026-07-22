<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# IP Filter

Play provides an IP filter that allows or denies access to resources by matching the request's typed selected remote identity against white and black lists. The selected identity comes from `request.remote.node`; it can be an IP address, RFC 7239 `unknown`, or an obfuscated identifier.

The filter uses Play's normalized remote value rather than reading forwarding headers directly. When Play is behind a proxy, configure [[trusted proxies|HTTPServer#configuring-trusted-proxies]] correctly and ensure they remove or overwrite client-supplied forwarding fields.

## Enabling the IP filter

To enable the IP filter, add it to `application.conf`:

```
play.filters.enabled += play.filters.ip.IPFilter
```

## Supported entries

Each list entry must use one of these forms:

```hocon
play.filters.ip.whiteList = [
  "192.0.2.10",       # exact IPv4
  "10.0.0.0/8",       # IPv4 CIDR network
  "2001:db8::/32",    # IPv6 CIDR network
  "unknown",          # every RFC 7239 unknown identity
  "_trusted-client"   # one exact obfuscated identity
]
```

IP literals are parsed without DNS resolution. Entries must use printable ASCII. IPv4 literals must use canonical four-part dotted-decimal notation: each octet is `0`–`255`, with no leading zero unless the octet is exactly `0`. Hostnames, whitespace-padded or non-ASCII values, IPv4 shorthand/integer/leading-zero forms, bracketed addresses, endpoint ports, zone identifiers, malformed obfuscated identifiers, and invalid CIDR prefixes make application configuration fail at startup. CIDR prefix lengths are `0`–`32` for IPv4 and `0`–`128` for IPv6.

Older Play versions passed every list entry to `InetAddress.getByName`. Depending on the host and name service, that accepted DNS names such as `localhost`, IPv4 shorthand such as `127.1`, integer IPv4 such as `2130706433`, leading-zero IPv4 such as `001.002.003.004`, bracketed IPv6 such as `[::1]`, scoped IPv6 such as `fe80::1%1`, and the empty string as the loopback address. Those forms are not accepted by the strict parser. Entries named `unknown` or beginning with `_` were previously passed to name resolution and could match an IP if resolvable; they now deliberately denote typed RFC 7239 remote identities. Whitespace-padded values and endpoint notation such as `192.0.2.1:443` were not valid entries under the former parser and remain invalid.

`unknown` is recognized case-insensitively. Obfuscated identifiers follow the RFC 7239 `_...` grammar and are compared exactly and case-sensitively; wildcards are not supported. A node's numeric or obfuscated port is ignored, as are its RFC `by` node and the direct `request.transport.peer`.

Allowlisting `unknown` permits every request whose trusted forwarding metadata selects the undifferentiated unknown identity. [RFC 7239 §6.3](https://www.rfc-editor.org/rfc/rfc7239.html#section-6.3) permits obfuscated identifiers to be regenerated for each request, so use one for access control only when a trusted proxy deliberately assigns a stable identifier.

## White listing remote identities

If the whitelist is not empty, only a matching selected remote node is allowed. This is the fail-closed mode: IP, `unknown`, and obfuscated nodes that are not explicitly matched are denied.

If both lists are configured, the existing whitelist precedence is retained and the blacklist is ignored.

## Black listing remote identities

The blacklist is active only if the whitelist is empty. A request is denied only when its selected remote node matches a blacklist entry; every unmatched node is allowed. For example, an unrelated IP blacklist entry does not deny `unknown` or `_hidden`. List those identities explicitly to deny them, or use a whitelist when every unlisted identity must be denied.

```hocon
# The black list is only active if the whitelist is empty,
# otherwise it will be ignored.
play.filters.ip.blackList = [ "192.168.0.1", "2001:db8::/32", "unknown", "_blocked-client" ]
```

## HTTP Status Code

The default HTTP status code for a forbidden request blocked by the IP filter is `403 Forbidden`.

You can customize the returned HTTP status using following config:

```
# 401 Unauthorized
play.filters.ip.accessDeniedHttpStatusCode = 401
```

## Selectively disabling the filter with Route Modifier

When using the default config Play ships with and you have a white- or blacklist defined, the IP filter will check the selected remote identity of every request. There may be individual routes where you do not want the filter to apply, and the `anyip` route modifier may be used here, using the [[route modifier syntax|ScalaRouting#The-routes-file-syntax]].

In your `conf/routes` file:

```
+ anyip
GET     /path1         controllers.HomeController.myAction
```

This excludes the `GET /path1` route from the IP filter, always giving access to the route regardless of its selected remote identity.

The full range of configuration options available to the IP filter can be found in the Play Filters [`reference.conf`](resources/confs/play-filters-helpers/reference.conf). As you can see in this reference conf you can also reverse the route modifier behaviour by defining a route modifier blacklist only, meaning by default all routes are permissive and only specific routes may be checked by introducing e.g. a `checkip` blacklist route modifier.
