<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Setting up a front end HTTP server

You can easily deploy your application as a stand-alone server by setting the application HTTP port to 80:

```bash
$ /path/to/bin/<project-name> -Dhttp.port=80
```

> **Note**: you probably need root permissions to bind a process on this port.

However, if you plan to host several applications in the same server or load balance several instances of your application for scalability or fault tolerance, you can use a front end HTTP server.

Note that using a front end HTTP server will rarely give you better performance than using Play server directly.  However, HTTP servers are very good at handling HTTPS, conditional GET requests and static assets, and many services assume a front end HTTP server is part of your architecture.

## Set up with lighttpd

This example shows you how to configure [lighttpd](http://www.lighttpd.net/) as a front end web server. Note that you can do the same with Apache, but if you only need virtual hosting or load balancing, lighttpd is a very good choice and much easier to configure.

The `/etc/lighttpd/lighttpd.conf` file should define configuration like this:

```
server.modules = (
      "mod_access",
      "mod_proxy",
      "mod_accesslog"
)

$HTTP["host"] =~ "www.myapp.com" {
    proxy.balance = "round-robin" proxy.server = ( "/" =>
        ( ( "host" => "127.0.0.1", "port" => 9000 ) ) )
}

$HTTP["host"] =~ "www.loadbalancedapp.com" {
    proxy.balance = "round-robin" proxy.server = ( "/" => (
          ( "host" => "127.0.0.1", "port" => 9001 ),
          ( "host" => "127.0.0.1", "port" => 9002 ) )
    )
}
```

See [lighttpd's documentation](https://redmine.lighttpd.net/projects/lighttpd/wiki/Docs_ModProxy) for more details about how to configure `mod_proxy`.

## Set up with nginx

This example shows you how to configure [nginx](https://www.nginx.com/resources/wiki/start/) as a front end web server. Note that you can do the same with Apache, but if you only need virtual hosting or load balancing, nginx is a very good choice and much easier to configure.

> **Note**: nginx has extensive documentation about how to configure it as a load balancer. See the [HTTP Load Balance Guide](https://docs.nginx.com/nginx/admin-guide/load-balancer/http-load-balancer/) for detailed information.

The `/etc/nginx/nginx.conf` file should define `upstream` and `server` block like this:

```
upstream playapp {
  server 127.0.0.1:9000;
}

server {
  listen 80;
  server_name www.domain.com;
  location / {
    proxy_pass http://playapp;
  }
}
```

For more details, see a [full example configuration](https://www.nginx.com/resources/wiki/start/topics/examples/full/), and if you want to use nginx to do SSL termination, see the [documentation here](https://docs.nginx.com/nginx/admin-guide/security-controls/terminating-ssl-http/).

> **Note**: make sure you are using version 1.2 or greater of Nginx otherwise chunked responses won't work properly.

## Set up with Apache

The example below shows a simple set up with [Apache httpd server](https://httpd.apache.org/) running in front of a standard Play configuration.

```
LoadModule proxy_module modules/mod_proxy.so
…
<VirtualHost *:80>
  ProxyPreserveHost On
  ServerName www.loadbalancedapp.com
  ProxyPass  /excluded !
  ProxyPass / http://127.0.0.1:9000/
  ProxyPassReverse / http://127.0.0.1:9000/
</VirtualHost>
```

## Advanced proxy settings

When using an HTTP frontal server, request addresses are seen as coming from the HTTP server. In a usual set-up, where you both have the Play app and the proxy running on the same machine, the Play app will see the requests coming from `127.0.0.1`.

Proxy servers can add a specific header to the request to tell the proxied application where the request came from. Most web servers will add an `X-Forwarded-For` header with the remote client IP address as first argument. If the proxy server is running on `localhost` and connecting from `127.0.0.1`, Play will trust its `X-Forwarded-For` header.

However, the host header is untouched, it’ll remain issued by the proxy. If you use Apache 2.x, you can add a directive like:

```
ProxyPreserveHost on
```

The `Host` header will be the original host request header issued by the client. By combining theses two techniques, your app will appear to be directly exposed.

If you don't want this play app to occupy the whole root, add an exclusion directive to the proxy config:

```
ProxyPass /excluded !
```

## Apache as a front proxy to allow transparent upgrade of your application

The basic idea is to run two Play instances of your web application and let the front-end proxy load-balance them. In case one is not available, it will forward all the requests to the available one.

Let’s start the same Play application two times: one on port `9999` and one on port `9998`.

```bash
start -Dhttp.port=9998
start -Dhttp.port=9999
```

Now, let’s configure our Apache web server to have a load balancer. In Apache, add the following configuration:

```
<VirtualHost mysuperwebapp.com:80>
  ServerName mysuperwebapp.com
  <Location /balancer-manager>
    SetHandler balancer-manager
    Order Deny,Allow
    Deny from all
    Allow from .mysuperwebapp.com
  </Location>
  <Proxy balancer://mycluster>
    BalancerMember http://localhost:9999
    BalancerMember http://localhost:9998 status=+H
  </Proxy>
  <Proxy *>
    Order Allow,Deny
    Allow From All
  </Proxy>
  ProxyPreserveHost On
  ProxyPass /balancer-manager !
  ProxyPass / balancer://mycluster/
  ProxyPassReverse / balancer://mycluster/
</VirtualHost>
```

The important part is `balancer://mycluster`. This declares a load balancer. The `+H` option means that the second Play application is on standby. But you can also instruct it to load balance.

Apache also provides a way to view the status of your cluster. Simply point your browser to `/balancer-manager` to view the current status of your clusters.

Because Play is completely stateless you don’t have to manage sessions between the 2 clusters. You can actually easily scale to more than 2 Play instances.

To use WebSockets, you must use [mod_proxy_wstunnel](http://httpd.apache.org/docs/2.4/mod/mod_proxy_wstunnel.html), which was introduced in Apache 2.4.

Note that [ProxyPassReverse might rewrite incorrectly headers](https://bz.apache.org/bugzilla/show_bug.cgi?id=51982) adding an extra / to the URIs, so you may wish to use this workaround:

```
ProxyPassReverse / http://localhost:9999
ProxyPassReverse / http://localhost:9998
```

## Configuring trusted proxies

Play supports various forwarded headers used by proxies to indicate the incoming remote identity, IP address, receiving proxy node, port, protocol, and host of requests. Play uses this configuration to calculate the correct value for the `remoteNode`, `remoteIdentity`, `remoteIpAddress`, `byNode`, `remotePort`, and `secure` fields of `RequestHeader.connection`. When explicitly enabled, Play can also use a trusted RFC 7239 `host` parameter for `RequestHeader.host`.

It is trivial for an HTTP client, whether it's a browser or other client, to forge forwarded headers, thereby spoofing the remote identity and protocol that Play reports. Consequently, Play needs to know which proxies are trusted. Play provides configuration options to configure trusted proxies, and will validate the incoming forwarded headers to verify that they are trusted, taking the first untrusted remote identity that it finds as the reported user remote identity (or the first identity if all proxies are trusted.)

To configure the list of trusted proxies, you can configure `play.http.forwarded.trustedProxies`.  This takes a list of IP address or CIDR subnet ranges.  Both IPv4 and IPv6 are supported.  For example:

```
play.http.forwarded.trustedProxies=["192.168.0.0/24", "::1", "127.0.0.1"]
```

This says all IP addresses that start with `192.168.0`, as well as the IPv6 and IPv4 loopback addresses, are trusted.  By default, Play will just trust the loopback address, that is `::1` and `127.0.0.1`.

### Trusting all proxies

Many cloud providers, most notably AWS, provide no guarantees for which IP addresses their load balancer proxies will use.  Consequently, the only way to support forwarded headers with these services is to trust all IP addresses.  This can be done by configuring the trusted proxies like so:

```
play.http.forwarded.trustedProxies=["0.0.0.0/0", "::/0"]
```

### Forwarded header version

Play supports two different versions of forwarded headers:

* the legacy method with X-Forwarded headers
* the [RFC 7239](https://tools.ietf.org/html/rfc7239) with Forwarded headers

This is configured using `play.http.forwarded.version`, with valid values being `x-forwarded` or `rfc7239`. The default is `x-forwarded`.

`x-forwarded` uses the de facto standard `X-Forwarded-For`, `X-Forwarded-Port`, and `X-Forwarded-Proto` headers to determine the correct remote identity, port, and protocol for the request. These headers are widely used, however, they have some serious limitations, for example, if you have multiple proxies, and only one of them adds the `X-Forwarded-Proto` header, it's impossible to reliably determine which proxy added it and therefore whether the request from the client was made using https or http. `rfc7239` uses the new `Forwarded` header standard, and solves many of the limitations of the `X-Forwarded-*` headers.

For more information, please read the [RFC 7239](https://tools.ietf.org/html/rfc7239) specification.

### RFC 7239 syntax validation

Play validates `Forwarded` field values using the RFC 7239 token, quoted-string, parameter, and HTTP list syntax. Parameter names cannot be repeated within one forwarded element. Empty HTTP list elements are ignored.

RFC 7239 requires IPv6 addresses and node identifiers containing a port to be quoted because `:` is not valid in an unquoted token:

```http
Forwarded: for="[2001:db8:cafe::17]:4711"
Forwarded: for="192.0.2.43:4711"
```

For compatibility with Play 3.0, Play also accepts these node values without quotes in the `for` parameter. Play applies the same allowance to `by` for consistent node parsing. New and updated proxy configurations should emit the quoted RFC 7239 syntax. This compatibility does not allow non-token characters in other parameter values.

If a `Forwarded` field value is malformed, Play treats that field as an unverifiable proxy boundary. Trusted-proxy scanning stops when it reaches that field and keeps the last verified connection information; it never skips malformed forwarding information to trust an earlier entry.

### RFC 7239 remote identities

RFC 7239 `Forwarded` headers can identify the remote client with an IP address, the `unknown` identifier, or an obfuscated identifier such as `_hidden`. Play exposes this value through `RequestHeader.connection.remoteNode`.

Use `RequestHeader.connection.remoteIdentity` when you need the selected remote identity as a string. `RequestHeader.remoteIdentity` is available as a request-level shortcut. When the selected remote node is an IP address, `RequestHeader.connection.remoteIpAddress` contains that address. When the selected remote node is `unknown` or obfuscated, `remoteIpAddress` is empty. The deprecated `RequestHeader.remoteAddress` method still returns a fallback IP address for compatibility, usually the previous trusted proxy address, and should not be used when applications need the actual RFC 7239 remote identity.

When an RFC 7239 `Forwarded` element contains a `by` parameter, Play exposes it through `RequestHeader.connection.byNode`. This identifies the proxy interface that received the request represented by `remoteNode`; it is not the selected remote client identity.

If Play selects an `unknown` or untrusted obfuscated remote node while scanning a trusted proxy chain, it stops scanning at that node because it cannot determine whether the non-IP identifier represents a trusted proxy.

### RFC 7239 forwarded hosts

RFC 7239 `Forwarded` headers can include a `host` parameter that identifies the original `Host` value received by the proxy. Host forwarding is disabled by default because the effective host affects request routing, URL generation, and cache keys. Enable it explicitly:

```hocon
play.http.forwarded.version = "rfc7239"
play.http.forwarded.trustForwardedHost = true
```

Play then uses the `host` parameter from the selected trusted `Forwarded` element as `RequestHeader.host`.

For example:

```http
Host: play.internal
Forwarded: for=203.0.113.43;proto=https;host=www.example.com
```

When the proxy that sent this header is trusted, `request.host` is `www.example.com`. A host containing a port must be quoted because `:` is not valid in an RFC 7239 token:

```http
Forwarded: for=203.0.113.43;proto=https;host="www.example.com:8443"
```

IPv6 literals must also be quoted because their brackets are not token characters. As required by [RFC 7239](https://www.rfc-editor.org/rfc/rfc7239.html#section-5.3), the value must conform to the HTTP [`Host` field grammar](https://www.rfc-editor.org/rfc/rfc9110.html#section-7.2), including brackets around IPv6 addresses. If the proxy is not trusted, the selected `Forwarded` element has no valid `host` parameter, or host forwarding is disabled, Play keeps the original `Host` header.

RFC 7239 parameters are independent, so a trusted proxy can send an element containing `host` without `for`. Play can use that host, but stops scanning the forwarded identity chain at the current connection because it cannot verify the preceding node without `for`.

Only rely on forwarded hosts when your trusted edge proxy overwrites or removes any incoming client-supplied `Forwarded` header before setting the correct value. Otherwise, clients may be able to spoof the request host.

The [[Allowed Hosts filter|AllowedHostsFilter]] validates `RequestHeader.host`. When forwarded host handling is enabled, configure `play.filters.hosts.allowed` with the public forwarded hosts rather than only the internal proxy-facing host.

### Trusting RFC 7239 obfuscated proxy identifiers

RFC 7239 allows proxies to use obfuscated identifiers, such as `_edge`, instead of IP addresses. By default, Play stops scanning the forwarded chain when it reaches an obfuscated identifier because `play.http.forwarded.trustedProxies` can only verify IP addresses and CIDR ranges.

When `play.http.forwarded.version = "rfc7239"`, known obfuscated proxy identifiers can be trusted explicitly:

```hocon
play.http.forwarded.trustedProxyIdentifiers = ["_edge", "_internal"]
```

Each value must start with `_` and then contain only ASCII letters, digits, `.`, `_`, or `-`, as required for an RFC 7239 obfuscated identifier. Invalid values, including `unknown`, cause configuration loading to fail.

For example, with `_edge` configured as trusted, Play can continue past this proxy and select the original client:

```http
Forwarded: for=203.0.113.43;proto=https
Forwarded: for=_edge
Forwarded: for=192.168.1.10
```

Only add identifiers that are generated by trusted infrastructure and cannot be supplied by clients. The setting matches identifiers exactly and only applies to RFC 7239 `Forwarded` headers.

### Trusting a single X-Forwarded-Proto value

Some proxy chains append to `X-Forwarded-For`, but set a single `X-Forwarded-Proto` value. In that case, Play cannot normally match each forwarded address to a protocol value, so it discards the protocol information and treats the forwarded connection as insecure.

If your trusted edge proxy is known to set `X-Forwarded-Proto` to the protocol used by the original client request, you can enable:

```
play.http.forwarded.trustSingleXForwardedProto = true
```

This setting only applies when `play.http.forwarded.version = "x-forwarded"`. It associates a single `X-Forwarded-Proto` value with the client address from `X-Forwarded-For`. It does not apply to RFC 7239 `Forwarded` headers, and it does not use `X-Forwarded-Proto` when `X-Forwarded-For` is absent.

Only enable this when your trusted edge proxy overwrites or removes any incoming client-supplied `X-Forwarded-Proto` header before setting the correct value. Otherwise, clients may be able to spoof whether a request was secure.

### Trusting X-Forwarded-Proto without X-Forwarded-For

Some proxy setups send `X-Forwarded-Proto` without sending `X-Forwarded-For`. By default, Play ignores that protocol value because there is no forwarded address chain to attach it to.

If your trusted proxy is known to set `X-Forwarded-Proto` to the protocol used by the request before it reached Play, you can enable:

```
play.http.forwarded.trustXForwardedProtoWithoutXForwardedFor = true
```

This setting only applies when `play.http.forwarded.version = "x-forwarded"`. It uses a single `X-Forwarded-Proto` value only when `X-Forwarded-For` is absent and the immediate proxy connection is trusted. It updates `RequestHeader.connection.secure`, but it does not change `remoteNode`, `remoteIdentity`, or `remoteIpAddress`.

Only enable this when your trusted proxy overwrites or removes any incoming client-supplied `X-Forwarded-Proto` header before setting the correct value. Otherwise, clients may be able to spoof whether a request was secure.

### Trusting a single X-Forwarded-Port value

Play uses `X-Forwarded-Port` when it can match port values to `X-Forwarded-For` addresses. A single `X-Forwarded-Port` value is used automatically when there is a single `X-Forwarded-For` address. Multiple port values are paired with forwarded addresses by position when both headers contain the same number of values.

Some proxy chains append to `X-Forwarded-For`, but set a single `X-Forwarded-Port` value. In that case, Play cannot normally match each forwarded address to a port value, so it discards the port information.

If your trusted edge proxy is known to set `X-Forwarded-Port` to the port used by the original client request, you can enable:

```
play.http.forwarded.trustSingleXForwardedPort = true
```

This setting only applies when `play.http.forwarded.version = "x-forwarded"`. It associates a single `X-Forwarded-Port` value with the client address from `X-Forwarded-For`. It does not apply to RFC 7239 `Forwarded` headers, where the port is part of the `for` value, and it does not use `X-Forwarded-Port` when `X-Forwarded-For` is absent.

Only enable this when your trusted edge proxy overwrites or removes any incoming client-supplied `X-Forwarded-Port` header before setting the correct value. Otherwise, clients may be able to spoof the forwarded port.
