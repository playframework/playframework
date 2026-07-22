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

Play supports various forwarded headers used by proxies to indicate the incoming remote identity, IP address, receiving proxy node, port, protocol, and host of requests. Play uses this configuration to calculate `RequestHeader.remote`, plus the effective `RequestHeader.scheme` and `RequestHeader.authority`. `RequestHeader.secure` is derived from the scheme, and `RequestHeader.host` renders the authority.

It is trivial for an HTTP client, whether it's a browser or other client, to forge forwarded headers, thereby spoofing the remote identity, protocol, or authority that Play reports. Consequently, Play needs to know which proxies are trusted. Play provides configuration options to configure trusted proxies, and will validate the incoming forwarded headers to verify that they are trusted, taking the first untrusted remote identity that it finds as the reported user remote identity (or the first identity if all proxies are trusted.)

To configure the list of trusted proxies, you can configure `play.http.forwarded.trustedProxies`.  This takes a list of IP address or CIDR subnet ranges.  Both IPv4 and IPv6 are supported.  For example:

```
play.http.forwarded.trustedProxies=["192.168.0.0/24", "::1", "127.0.0.1"]
```

This says all IP addresses that start with `192.168.0`, as well as the IPv6 and IPv4 loopback addresses, are trusted.  By default, Play will just trust the loopback address, that is `::1` and `127.0.0.1`.

Trusted-proxy addresses must be ASCII numeric IPv4 or IPv6 literals; Play does not resolve hostnames. IPv6 zone identifiers such as `%1` or `%eth0`, non-ASCII digits, brackets, and endpoint ports are rejected. CIDR prefix lengths must contain only ASCII decimal digits and be within `0` through `32` for IPv4 or `0` through `128` for IPv6. Invalid trusted-proxy entries fail application startup rather than silently widening or changing the trust boundary.

### Securing the forwarding boundary

`trustedProxies` authorizes forwarding claims according to the peer that supplied them. It does not authenticate that peer, prevent clients from bypassing the proxy, sanitize fields at the network edge, or protect the fields while they travel between trusted systems.

A production deployment that uses forwarding metadata must therefore enforce all of the following outside Play:

* Prevent clients from connecting directly to Play. Use firewall rules, security groups, or equivalent controls so that application ports accept traffic only from the expected proxy tier. Otherwise clients can bypass TLS termination, header sanitization, access controls, or other policy enforced by the proxy.
* At the first trusted proxy that receives client traffic, remove every client-supplied `Forwarded` and `X-Forwarded-*` field before setting authoritative values. Later trusted proxies may extend only that sanitized chain according to the configured forwarding protocol. Otherwise a client can insert claims that appear to describe an earlier hop.
* Protect every trusted proxy-to-proxy and proxy-to-Play hop against interception and modification. Use authenticated TLS, such as mutually authenticated TLS where appropriate, or equivalently strong network isolation. IP-based trust alone does not protect header contents from an on-path attacker or a system that can impersonate a trusted source address.
* Configure only the addresses, CIDR ranges, and obfuscated identifiers actually controlled by the proxy infrastructure. A broader trust range makes every system in that range authoritative for forwarding metadata.

If these guarantees cannot be enforced, do not use forwarded metadata for security decisions. Play always retains the directly connected peer in `RequestHeader.transport.peer`, independently of the selected forwarded remote identity.

### Trusting all proxies

Many cloud providers, most notably AWS, provide no guarantees for which IP addresses their load balancer proxies will use.  Consequently, the only way to support forwarded headers with these services is to trust all IP addresses.  This can be done by configuring the trusted proxies like so:

```
play.http.forwarded.trustedProxies=["0.0.0.0/0", "::/0"]
```

Only trust all addresses when clients cannot connect directly to Play and every request passes through infrastructure that removes or overwrites client-supplied forwarding headers. Otherwise, every client is treated as a trusted proxy and can spoof forwarding information.

### Forwarded header version

Play supports two different versions of forwarded headers:

* the legacy method with X-Forwarded headers
* the [RFC 7239](https://tools.ietf.org/html/rfc7239) with Forwarded headers

This is configured using `play.http.forwarded.version`, with valid values being `x-forwarded` or `rfc7239`. The default is `x-forwarded`.

`x-forwarded` uses the de facto standard `X-Forwarded-*` headers to determine remote connection information and, when explicitly enabled, effective request authority information. These headers are widely used, however, they have some serious limitations, for example, if you have multiple proxies, and only one of them adds the `X-Forwarded-Proto` header, it's impossible to reliably determine which proxy added it and therefore whether the request from the client was made using https or http. `rfc7239` uses the new `Forwarded` header standard, and solves many of the limitations of the `X-Forwarded-*` headers.

For more information, please read the [RFC 7239](https://tools.ietf.org/html/rfc7239) specification.

### Choosing the right request metadata

Forwarding separates the client selected from trusted headers from the network connection that directly reached Play. Use the value that answers the application's actual question:

| If you need | Scala API | Java API | Meaning |
| --- | --- | --- | --- |
| Selected client identity | `request.remote` | `request.remote()` | May be an IP address, `unknown`, or an RFC 7239 obfuscated identifier. |
| Selected client IP address | `request.remote.ipAddress` | `request.remote().ipAddress()` | Empty when the selected identity is not an IP address. |
| Directly connected proxy or client | `request.transport.peer` | `request.transport().peer()` | The socket peer observed by Play, including its source port. Forwarding never changes it. |
| TLS and peer certificates on the connection to Play | `request.transport.tls` | `request.transport().tls()` | Describes only the connection terminating at Play, not the original client-to-proxy connection. |
| Effective public request scheme | `request.scheme` or `request.secure` | `request.scheme()` or `request.secure()` | Can come from direct TLS or trusted forwarding metadata. |
| Effective public host and destination port | `request.authority` or `request.host` | `request.authority()` or `request.host()` | Used by request-aware URL generation, CORS, and the allowed hosts filter. |
| Accepted forwarding path | `request.remote.path` and `request.remote.forwarding` | `request.remote().path()` and `request.remote().forwarding()` | Contains only the forwarding elements accepted up to the configured trust boundary. |
| Exact received forwarding text | `request.headers` | `request.getHeaders()` | Use only for auditing or protocol diagnostics, not instead of the normalized trusted values above. |

### Minimal forwarded-header configurations

Every recipe below assumes that clients cannot connect directly to Play and that the first trusted proxy removes all client-supplied `Forwarded` and `X-Forwarded-*` fields before writing authoritative values.

#### One trusted proxy using X-Forwarded-For and X-Forwarded-Proto

Use this when the proxy preserves the public `Host` field and sends one `X-Forwarded-For` and matching `X-Forwarded-Proto` value:

```hocon
play.http.forwarded {
  version = "x-forwarded"
  trustedProxies = ["10.0.0.10"]
}
```

The proxy at `10.0.0.10` must be the only client able to reach Play on the application port. Play selects the client from `X-Forwarded-For`, the effective scheme from the corresponding `X-Forwarded-Proto`, and the authority from the preserved `Host` field.

#### One trusted proxy that rewrites the Host field

Use this when the proxy sends Play an internal `Host` field and supplies the public destination separately:

```hocon
play.http.forwarded {
  version = "x-forwarded"
  trustedProxies = ["10.0.0.10"]
  trustXForwardedHost = true
  trustXForwardedPort = true
}
```

The proxy must set exactly one unambiguous `X-Forwarded-Host` and `X-Forwarded-Port`, in addition to its sanitized client and scheme headers. If `X-Forwarded-Host` already contains the intended public port, omit `trustXForwardedPort` and the separate port header. Configure the allowed hosts filter with the resulting public authority.

#### Multiple trusted proxies using RFC 7239

Use RFC 7239 when multiple controlled proxies need to keep each hop's identity, scheme, and optional host together:

```hocon
play.http.forwarded {
  version = "rfc7239"
  trustedProxies = ["10.0.0.10", "10.0.0.11"]
  trustForwardedHost = true
}
```

The first proxy must remove incoming forwarding fields, and each trusted proxy must append its own `Forwarded` element without changing older sanitized elements. Omit `trustForwardedHost` when the proxy chain preserves the public `Host` field or the application does not need forwarded authority. Protect every proxy-to-proxy and proxy-to-Play hop as described in [Securing the forwarding boundary](#securing-the-forwarding-boundary).

### Forwarded header examples

The examples below assume that the immediate TCP connection to Play comes from `127.0.0.1`, which is trusted by default. Scala API names are shown; the corresponding Java values are available through `request.remote()`, `request.scheme()`, and `request.authority()`.

#### Direct request

Without forwarding headers, Play reports the raw socket peer:

```http
GET /products/42 HTTP/1.1
Host: play.internal:9000
```

The resulting values include:

```scala
request.remote.identity          // "127.0.0.1"
request.remote.ipAddress         // Some(127.0.0.1)
request.remote.port              // the raw peer source port
request.transport.peer.address     // 127.0.0.1
request.transport.peer.port        // the same raw peer source port
request.transport.tls              // Some(...) when the socket terminating at Play uses TLS
request.scheme                     // Scheme.Http, or Scheme.Https for direct TLS
request.authority                  // Some(RequestAuthority(...))
request.host                       // "play.internal:9000"
```

The raw peer source port is normally an ephemeral port assigned to the client connection. It is not Play's listener port, such as `9000`.

#### One trusted proxy

With the default `x-forwarded` version, a trusted proxy can identify the client and the protocol used by that client connection:

```http
X-Forwarded-For: 203.0.113.43:53124
X-Forwarded-Proto: https
```

Play selects the forwarded endpoint:

```scala
request.remote.identity // "203.0.113.43"
request.remote.port     // Some(53124), the client source port
request.scheme                    // Scheme.Https
request.secure                    // true
request.transport.peer            // unchanged directly connected proxy endpoint
request.transport.tls             // actual proxy-to-Play TLS metadata
```

Forwarded metadata never replaces `request.transport`. This keeps the direct proxy address and source port available for correlating Play requests with transport-level server logs. A trusted forwarded `proto=https` can make the effective request secure while `request.transport.tls` remains empty when the proxy-to-Play hop uses plaintext.

#### Multiple trusted proxies

For a chain containing an additional proxy at `192.168.1.10`, configure both proxy hops as trusted:

```hocon
play.http.forwarded.trustedProxies = ["127.0.0.1", "192.168.1.0/24"]
```

The nearest proxy appears at the end of `X-Forwarded-For`. Play scans from right to left and selects the first untrusted identity:

```http
X-Forwarded-For: 203.0.113.43, 192.168.1.10
X-Forwarded-Proto: https, http
```

The two lists have equal lengths, so Play pairs their entries by position:

```scala
request.remote.identity // "203.0.113.43"
request.remote.port     // None, no client source port was forwarded
request.scheme                    // Scheme.Https
request.secure                    // true
```

When a proxy chain sends only one `X-Forwarded-Proto` value, see [Trusting a single X-Forwarded-Proto value](#trusting-a-single-x-forwarded-proto-value).

#### Forwarded request authority

`X-Forwarded-Host` and `X-Forwarded-Port` describe the public destination rather than the remote client endpoint. Their independently trusted settings can be used without `X-Forwarded-For`:

```hocon
play.http.forwarded.trustXForwardedHost = true
play.http.forwarded.trustXForwardedPort = true
```

For example:

```http
Host: play.internal:9000
X-Forwarded-Host: www.example.com:7000
X-Forwarded-Port: 8443
```

Play uses the forwarded host and lets the forwarded destination port replace its existing port:

```scala
request.host                  // "www.example.com:8443"
request.authority             // typed host plus AuthorityPort(8443)
request.remote.port // still the raw peer source port
```

Request-aware absolute and WebSocket URL generation and the allowed hosts filter use the resulting authority.

#### RFC 7239 Forwarded

RFC 7239 keeps metadata for each hop together in one `Forwarded` element. With `127.0.0.1` and `192.168.1.0/24` trusted and forwarded-host handling enabled:

```hocon
play.http.forwarded.version = "rfc7239"
play.http.forwarded.trustForwardedHost = true
```

A two-proxy request can be represented as:

```http
Forwarded: for="203.0.113.43:53124";by=192.168.1.10;proto=https;host="www.example.com:8443", for=192.168.1.10;by=127.0.0.1;proto=http
```

Play selects the first untrusted element and preserves its associated metadata:

```scala
request.remote.identity // "203.0.113.43"
request.remote.port     // Some(53124)
request.remote.byNode   // Some(RemoteNode.Ip(192.168.1.10, None))
request.remote.isForwarded // true
request.remote.forwarding.map(_.source) // Some(ForwardingSource.Rfc7239)
request.remote.path.map(_.node)
// Vector(RemoteNode.Ip(203.0.113.43, ...), RemoteNode.Ip(192.168.1.10, ...))
request.scheme                    // Scheme.Https
request.secure                    // true
request.authority                 // typed www.example.com:8443 authority
request.host                      // "www.example.com:8443"
```

RFC 7239 also permits non-IP identities:

```http
Forwarded: for=_anonymous;by=127.0.0.1;proto=https
```

For this request, `request.remote.identity` is `_anonymous` and `request.remote.ipAddress` is empty. Play stops scanning at an untrusted obfuscated or `unknown` identity because it cannot verify that node using an IP/CIDR trusted-proxy rule.

RFC 7239 parameters are independent, so a trusted proxy can send `Forwarded: proto=https` without a `for` parameter. Play then changes the effective request scheme while retaining its remote identity. Because the element does not identify the preceding node, Play stops there and does not scan any earlier elements. Every valid RFC 3986 scheme is retained; `request.secure` is true only when the normalized scheme is `https`.

All examples rely on the trusted edge proxy removing or overwriting forwarding headers supplied by clients before adding authoritative values. Never enable a forwarding option merely because a header is present.

### Effective request scheme and authority

`RequestHeader.scheme` and `RequestHeader.authority` are first-class immutable request values. For origin-form, asterisk-form, and CONNECT requests, the initial scheme comes from TLS on the transport terminating at Play, and accepted trusted forwarding metadata can select the effective public scheme. An absolute-form request target supplies its own scheme and any authority it contains. A trusted gateway may preserve the public absolute target or rewrite it to the scheme of its backend connection; Play accepts either when the target scheme matches the final effective scheme or the direct transport scheme. The trusted forwarded scheme becomes `RequestHeader.scheme`, while `RequestHeader.uri` retains the raw target. For an accepted HTTP or HTTPS absolute-form target with an empty path, Play exposes `/` as `RequestHeader.path` while retaining the raw target and query. Play rejects an absolute scheme that describes neither side of the trusted hop, including a direct mismatch without accepted scheme-changing forwarding metadata. For an accepted request, `RequestHeader.secure` is true exactly when the resulting normalized effective scheme is `https`.

For request-conversion failures reported to `HttpErrorHandler`, Play supplies a best-effort `RequestHeader`. Its `uri` retains the raw request target while independently valid trusted forwarding metadata can still determine `scheme`, `authority`, and `remote`. An error handler can therefore observe an absolute target such as `http://example.com/path` together with `request.scheme == https` when a trusted gateway rewrote the target to its backend scheme. Error-handler requests still contain best-effort metadata and do not necessarily satisfy every invariant required for application routing.

The authority initially comes from an absolute-form or `CONNECT` request target when present, and otherwise from the single `Host` field. Every HTTP/1.1 request must still contain exactly one syntactically valid `Host` field before target precedence is applied. Origin-form and asterisk-form require that field to contain a non-empty host. For absolute-form and CONNECT, the request-target authority takes precedence, so an empty `Host` field is accepted and replaced in the application-facing headers by the canonical target authority. HTTP and HTTPS absolute targets always require a non-empty authority, and even a non-empty `Host` field does not supply a missing target authority. CONNECT accepts only authority-form with a non-empty host and a destination port from `1` through `65535`; generic URI authorities continue to preserve arbitrary-size decimal ports. HTTP request targets containing a fragment are rejected. The authority is represented as a `RequestAuthority` with a typed registered-name, IPv4, IPv6, or IPvFuture host and an optional arbitrary-precision URI port. `RequestHeader.host`, `RequestHeader.domain`, and the exposed `Host` field are derived canonical views of this value. Netty passes this absolute-form traffic through to Play's validation. Pekko HTTP currently rejects an empty or different `Host` while establishing its effective URI, before Play request conversion runs.

For compatibility with requests accepted by Play 3.0, Play extracts and validates the scheme and authority of an HTTP(S) absolute-form target without requiring the entire raw path and query to parse as an RFC 3986 URI. For example, if the selected server backend accepts `https://example.com/search?model=GTI|V8`, Play preserves the `|` in `RequestHeader.uri` while still validating `https` and `example.com`. Backend request-line restrictions still apply and can reject other characters before Play constructs a request. Other absolute schemes are rejected as soon as their scheme is recognized; Play neither parses their remaining URI nor treats their rootless paths as application route paths.

Ordinary immutable core request copies, including `withTarget`, `withHeaders`, and `withAttrs`, preserve the effective scheme and authority. Core header replacement may omit `Host`, in which case Play restores its canonical value, but it cannot add, duplicate, or change the authority. Use `withScheme` and `withAuthority` for explicit changes.

Scala `FakeRequest.withHeaders` and Java `RequestBuilder.headers(...)` or `header(...)` are request-building conveniences: exactly one supplied `Host` value replaces and canonicalizes the effective authority, while omitting `Host` preserves the existing authority. Duplicate or invalid Host values are rejected. The Java request builder also provides explicit `scheme(...)` and `authority(...)` operations.

### RFC 7239 syntax validation

Play validates `Forwarded` field values using the RFC 7239 token, quoted-string, parameter, and HTTP list syntax. Parameter names cannot be repeated within one forwarded element. Empty HTTP list elements are ignored.

RFC 7239 requires IPv6 addresses and node identifiers containing a port to be quoted because `:` is not valid in an unquoted token:

```http
Forwarded: for="[2001:db8:cafe::17]:4711"
Forwarded: for="192.0.2.43:4711"
```

For compatibility with Play 3.0, Play also accepts these node values without quotes in the `for` parameter. Play applies the same allowance to `by` for consistent node parsing. Play also accepts an optional sequence of spaces and tabs immediately after the semicolon between parameters, as in `for=192.0.2.43; proto=https`. Whitespace before the semicolon or around `=` remains invalid. New and updated proxy configurations should emit quoted node values and the compact RFC 7239 syntax without parameter-separator whitespace. These compatibility allowances do not permit non-token characters in other parameter values.

Before applying an otherwise syntactically valid element, Play validates every recognized parameter that is present: `for` and `by` must be valid node identifiers, `host` must conform to request-authority syntax, and `proto` must be a valid RFC 3986 scheme. Validation is atomic and is not conditional on whether Play is configured to apply a parameter such as `host`. If any recognized value is invalid, none of that element changes `request.remote`, `request.scheme`, or `request.authority`.

The request-authority grammar permits a registered name with zero characters, so `host=""` and `host=":8080"` are grammatical values rather than atomic validation failures. An empty host is not usable as an effective HTTP authority, however. Play ignores that authority as a unit, including any embedded port, keeps the last verified authority, and still applies the element's other valid metadata.

A malformed field or invalid recognized value is an unverifiable proxy boundary. Trusted-proxy scanning stops there, keeps the last verified request information, and never skips past it to trust an older element. Syntactically valid unknown extension parameters are ignored and do not prevent recognized values in the same element from applying.

### RFC 7239 remote identities

RFC 7239 `Forwarded` headers can identify the remote client with an IP address, the `unknown` identifier, or an obfuscated identifier such as `_hidden`. Play exposes this value through `RequestHeader.remote.node`.

Use `RequestHeader.remote.identity` when you need the selected remote identity as a string. When the selected remote node is an IP address, `RequestHeader.remote.ipAddress` contains that address. When the selected remote node is `unknown` or obfuscated, `ipAddress` is empty; Play does not invent a fallback IP address.

When an RFC 7239 `Forwarded` element contains a `by` parameter, Play exposes it through `RequestHeader.remote.byNode`. This identifies the proxy interface that received the request represented by `node`; it is not the selected remote client identity.

Play also retains the part of the remote endpoint path that it actually accepted while walking the configured trust boundary. `RequestHeader.remote.forwarding` is empty for a direct request and contains a `ForwardingInfo` for a remote selected from forwarding headers. Its `source` distinguishes RFC 7239 from the `X-Forwarded-*` family. `RequestHeader.remote.path` starts with the selected endpoint and continues with the accepted intermediate proxy endpoints in client-to-Play order. Every endpoint preserves its own RFC 7239 `by` node; X-Forwarded endpoints have no `by` node.

For a direct request, the path contains only the selected endpoint, which is the direct socket peer. For a forwarded request, it excludes that socket peer because the independently observed endpoint is already available as `RequestHeader.transport.peer`. The path also excludes malformed elements, elements beyond the first untrusted boundary, and any older values Play did not inspect. Consequently, this is an accepted, configuration-derived path rather than proof that every asserted hop is authentic. The original fields remain available through `RequestHeader.headers` when an application needs their exact textual representation.

If Play selects an `unknown` or untrusted obfuscated remote node while scanning a trusted proxy chain, it stops scanning at that node because it cannot determine whether the non-IP identifier represents a trusted proxy.

### RFC 7239 forwarded hosts

RFC 7239 `Forwarded` headers can include a `host` parameter that identifies the original `Host` value received by the proxy. Host forwarding is disabled by default because the effective host affects request routing, URL generation, and cache keys. Enable it explicitly:

```hocon
play.http.forwarded.version = "rfc7239"
play.http.forwarded.trustForwardedHost = true
```

Play then parses the `host` parameter from the selected trusted `Forwarded` element as `RequestHeader.authority`. `RequestHeader.host` and the exposed `Host` header are canonical renderings of that typed value.

For example:

```http
Host: play.internal
Forwarded: for=203.0.113.43;proto=https;host=www.example.com
```

When the proxy that sent this header is trusted, `request.host` is `www.example.com`. A host containing a port must be quoted because `:` is not valid in an RFC 7239 token:

```http
Forwarded: for=203.0.113.43;proto=https;host="www.example.com:8443"
```

IPv6 literals must also be quoted because their brackets are not token characters. As required by [RFC 7239](https://www.rfc-editor.org/rfc/rfc7239.html#section-5.3), the value must conform to the HTTP [`Host` field grammar](https://www.rfc-editor.org/rfc/rfc9110.html#section-7.2), including brackets around IPv6 addresses. The parsed host component must also be non-empty before Play can use the value as the effective authority. If the proxy is not trusted, the selected `Forwarded` element has no usable `host` parameter, or host forwarding is disabled, Play keeps the previous verified authority.

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
```

Only add identifiers that are generated by trusted infrastructure and cannot be supplied by clients. The setting matches identifiers exactly and only applies to RFC 7239 `Forwarded` headers.

### Trusting a single X-Forwarded-Proto value

Some proxy chains append to `X-Forwarded-For`, but set a single `X-Forwarded-Proto` value. In that case, Play cannot normally match each forwarded address to a protocol value, so it discards the ambiguous protocol metadata and retains the previously verified effective request scheme.

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

This setting only applies when `play.http.forwarded.version = "x-forwarded"`. It uses a single `X-Forwarded-Proto` value only when `X-Forwarded-For` is absent and the immediate proxy connection is trusted. It updates `RequestHeader.scheme` and therefore `RequestHeader.secure`, but it does not change `RequestHeader.remote`.

Only enable this when your trusted proxy overwrites or removes any incoming client-supplied `X-Forwarded-Proto` header before setting the correct value. Otherwise, clients may be able to spoof whether a request was secure.

### Trusting X-Forwarded-Ssl

Some older proxies send `X-Forwarded-Ssl: on` or `X-Forwarded-Ssl: off` instead of `X-Forwarded-Proto`. Play can use this header as a compatibility fallback. This behavior is disabled by default. Enable it with:

```
play.http.forwarded.trustXForwardedSsl = true
```

This setting only applies when `play.http.forwarded.version = "x-forwarded"`. The directly connected proxy must match `play.http.forwarded.trustedProxies`. Play accepts exactly one value, interpreting `on` as HTTPS and `off` as HTTP, case-insensitively. Ambiguous comma-separated or repeated values and any other value are ignored.

When `X-Forwarded-For` is present, the value describes the protocol used by the original client and is associated with the first client entry. Play only reaches that entry if every intervening proxy is trusted. When `X-Forwarded-For` is absent, the value updates `RequestHeader.scheme` without changing the selected remote identity.

`X-Forwarded-Ssl` is considered only when the `X-Forwarded-Proto` header is entirely absent. If `X-Forwarded-Proto` is present, its existing processing rules take precedence even when its values are invalid, ambiguous, or cannot be matched to `X-Forwarded-For`. `X-Forwarded-Ssl` is a non-standard compatibility header; prefer `X-Forwarded-Proto` or RFC 7239 `Forwarded: proto=https` when the proxy can emit them.

Only enable this setting when the trusted proxy overwrites or removes incoming client-supplied `X-Forwarded-Ssl` and `X-Forwarded-Proto` headers before setting the correct value. Otherwise, clients may be able to spoof whether a request was secure.

### Trusting X-Forwarded-Host as the request authority

`X-Forwarded-Host` identifies the original `Host` value received by a proxy. Play can use one `X-Forwarded-Host` value as the effective request host. This behavior is disabled by default. Enable it with:

```
play.http.forwarded.trustXForwardedHost = true
```

This setting only applies when `play.http.forwarded.version = "x-forwarded"`. The directly connected proxy must match `play.http.forwarded.trustedProxies`. Because the host describes the request destination rather than a remote node, `X-Forwarded-For` is not required and the host is not paired with its entries.

Play accepts exactly one value that conforms to the HTTP [`Host` field grammar](https://www.rfc-editor.org/rfc/rfc9110.html#section-7.2) and has a non-empty parsed host component. For example, given `Host: play.internal:9000` and `X-Forwarded-Host: public.example:8443`, `request.host` is `public.example:8443`. IPv6 literals must use brackets, as in `[2001:db8::1]:8443`. Empty-host authorities such as `:8080`, as well as empty, ambiguous comma-separated, repeated, or malformed values, are ignored.

The effective `Host` header, typed Scala and Java authority APIs, request-aware absolute and WebSocket URL generation, and the allowed hosts filter all observe the forwarded authority. If `play.http.forwarded.trustXForwardedPort` is also enabled and both headers are usable, `X-Forwarded-Port` replaces any port included in `X-Forwarded-Host`. If the forwarded host is unusable but the forwarded port is independently valid and trusted, that port can still replace the port of the retained effective authority.

Only enable this setting when the trusted proxy removes or overwrites any incoming client-supplied `X-Forwarded-Host` header before setting the correct value. Otherwise, clients may be able to spoof the effective request authority.

### Forwarded remote ports

`RequestHeader.remote.nodePort` contains the numeric or obfuscated port identifier attached to the selected remote node. RFC 7239 permits either form on IP, `unknown`, and obfuscated nodes. `RequestHeader.remote.port` is its numeric convenience projection; it is empty when the selected node has no port or has an obfuscated port.

Play's typed remote endpoint model accepts numeric node ports from `0` through `65535`. RFC 7239's syntax permits any one-to-five-digit token, but values above the network-port range are not meaningful endpoint ports in Play and are rejected. An invalid port causes Play to stop at the last verified forwarding state.

For a direct request, the numeric port is the raw socket peer port, usually an ephemeral port assigned by the peer's operating system rather than Play's listener port. When Play selects a forwarded identity, an RFC 7239 `for` node port or a port embedded in `X-Forwarded-For` supplies that endpoint's source port. If the selected forwarded identity does not include a port, its node port is empty rather than retaining the port of the nearer proxy.

`X-Forwarded-Port` has different semantics: it normally identifies the original destination or public listener port that the client connected to. Play therefore does not expose `X-Forwarded-Port` as `request.remote.port`. Applications must not use the selected remote port to reconstruct the original public request authority.

### Trusting X-Forwarded-Port as the request authority port

Play can use one `X-Forwarded-Port` value as the destination port in the effective request host. This behavior is disabled by default. Enable it with:

```
play.http.forwarded.trustXForwardedPort = true
```

This setting only applies when `play.http.forwarded.version = "x-forwarded"`. The directly connected proxy must match `play.http.forwarded.trustedProxies`. Because the port describes the request destination rather than a remote node, `X-Forwarded-For` is not required and the port is not paired with its entries.

Play accepts exactly one non-empty, non-negative decimal URI port and preserves it as an arbitrary-precision `AuthorityPort`. When the value is valid and trusted, it replaces any port in the existing effective request authority. For example, `Host: public.example:9000` together with `X-Forwarded-Port: 8443` produces `request.host == "public.example:8443"`. This also applies to the authority of an absolute request target and preserves bracketed IPv6 syntax. `AuthorityPort.tcpPort` is available when an application specifically needs the `0` through `65535` network-port projection.

The effective `Host` header, Scala and Java request APIs, request-aware absolute and WebSocket URL generation, and the allowed hosts filter all observe the resulting authority. When trusted `X-Forwarded-Host` is also available, the port replaces any port in that forwarded authority. The HTTPS redirect filter continues to select its destination port from `play.filters.https.port`; an incoming destination port is not necessarily the correct HTTPS redirect port.

Only enable this setting when the trusted proxy removes or overwrites any incoming client-supplied `X-Forwarded-Port` header before setting the correct value. Otherwise, clients may be able to spoof the effective request authority.

### Forwarded client certificates

A reverse proxy that terminates mutually authenticated TLS can forward the certificate presented by the original client to Play. This has a separate trust boundary and configuration from the remote address, scheme, and authority forwarding described above.

Play exposes three deliberately different views of certificate information:

* `request.transport.tls.map(_.peerCertificates)` is physical transport metadata. It contains the certificate sequence observed on the TLS connection that terminates at Play. Behind a TLS proxy this is the proxy's certificate sequence, not the original client's.
* `request.clientCertificate` is the effective client certificate selected for application use. Its `certificate` is the leaf, its `chain` contains the remaining effective chain certificates in leaf-to-root order without repeating the leaf, and its `source` is `DirectTransport`, `Rfc9440`, or `XForwardedClientCert`.
* `request.xForwardedClientCertificates` is the ordered sequence of accepted `X-Forwarded-Client-Cert` assertions. An assertion can contain `By`, `Hash`, `Subject`, `URI`, and `DNS` metadata without containing a certificate, so this sequence can be non-empty while `request.clientCertificate` is empty.

The corresponding Java accessors are `request.transport()`, `request.clientCertificate()`, and `request.xForwardedClientCertificates()`.

All three accessors are available in every mode. `request.clientCertificate` is the common effective-certificate API; inspect its `source` when the application needs to distinguish direct TLS from either forwarding protocol:

| Mode and direct-peer trust | `request.clientCertificate` | `request.xForwardedClientCertificates` | `request.transport.tls` |
| --- | --- | --- | --- |
| `off`, or peer not trusted | Direct TLS leaf and chain with source `DirectTransport`, or empty | Empty | Direct TLS metadata, if present |
| `rfc9440` with trusted peer | `Client-Cert` leaf and `Client-Cert-Chain` with source `Rfc9440`, or empty | Empty | Direct proxy-to-Play TLS metadata, if present |
| `x-forwarded-client-cert` with trusted peer | Certificate from the accepted XFCC assertion with source `XForwardedClientCert`, or empty | Accepted XFCC assertion, including metadata-only assertions | Direct proxy-to-Play TLS metadata, if present |

For example, Scala applications can consume the selected certificate independently of its source while retaining access to the physical connection:

```scala
request.clientCertificate.foreach { info =>
  val leafAndChain = info.certificates
  val source = info.source
}

val directPeerCertificates =
  request.transport.tls.toSeq.flatMap(_.peerCertificates)
```

The equivalent Java APIs are:

```java
request.clientCertificate().ifPresent(info -> {
  List<X509Certificate> leafAndChain = info.certificates();
  Http.ClientCertificateSource source = info.source();
});

List<X509Certificate> directPeerCertificates =
    request.transport().tls()
        .map(Http.TransportTls::peerCertificates)
        .orElseGet(List::of);
```

Forwarded client certificates are disabled by default. Configure one of the two supported protocols and a dedicated set of directly connected trusted proxies:

```hocon
play.http.forwarded.clientCertificates {
  # off, rfc9440, or x-forwarded-client-cert
  mode = "rfc9440"

  # Independent from play.http.forwarded.trustedProxies
  trustedProxies = ["10.0.0.0/24", "2001:db8:1234::/48"]

  limits {
    maxHeaderBytes = 64k
    maxDecodedBytes = 64k
    maxCertificateBytes = 16k
    maxChainLength = 8
  }

  xForwardedClientCert {
    policy = "sanitized-single"
    format = "text"
  }
}
```

The limits bound the aggregate encoded header size, aggregate decoded certificate data, each decoded certificate, and the number of chain certificates excluding the leaf, respectively. These are application-level parser limits and do not increase the HTTP server's header-size limit.

In `off` mode, or when the directly connected peer is not in `clientCertificates.trustedProxies`, Play does not interpret forwarded certificate fields. The original `Client-Cert`, `Client-Cert-Chain`, and `X-Forwarded-Client-Cert` fields remain available through `request.headers`, but the typed forwarded-certificate APIs ignore them. A certificate observed directly by Play remains available through `request.transport.tls` and is selected in `request.clientCertificate` with the `DirectTransport` source.

When a trusted proxy and a forwarded-certificate mode are configured, the selected protocol describes the original client rather than the proxy-to-Play connection. If the trusted proxy supplies no client-certificate assertion, `request.clientCertificate` is empty; Play does not misidentify the proxy's own transport certificate as the original client's certificate. `request.transport.tls` still retains that physical connection information.

#### RFC 9440 Client-Cert

Set `mode = "rfc9440"` for the standardized [`Client-Cert` and `Client-Cert-Chain` fields](https://www.rfc-editor.org/rfc/rfc9440.html). `Client-Cert` contains exactly one DER X.509 end-entity certificate encoded as an RFC 8941 Byte Sequence. The optional `Client-Cert-Chain` is a list of Byte Sequences in TLS order, excludes the leaf, and may include the trust anchor:

```http
Client-Cert: :<base64-DER-leaf>:
Client-Cert-Chain: :<base64-DER-chain-certificate-1>:, :<base64-DER-chain-certificate-2>:
```

Play exposes the result through `request.clientCertificate` with the `Rfc9440` source. `Client-Cert` is a singleton; `Client-Cert-Chain` can be split across repeated fields as permitted by RFC 9440. Syntactically valid Structured Fields parameters on either certificate item are accepted for forward compatibility and ignored.

Following RFC 8941, Play treats a field that fails Structured Fields parsing as absent. A duplicated or malformed `Client-Cert`, including invalid Base64, DER, or trailing certificate data, therefore produces no effective client certificate. `Client-Cert-Chain` is handled independently: if its syntax or certificate data is invalid, Play ignores the chain but retains a valid `Client-Cert` leaf. A chain without a valid leaf has no effect. Exceeding a configured parser limit still rejects the request with HTTP 400.

#### X-Forwarded-Client-Cert

Set `mode = "x-forwarded-client-cert"` for the [Envoy-compatible text form](https://www.envoyproxy.io/docs/envoy/latest/configuration/http/http_conn_man/headers.html#x-forwarded-client-cert) of `X-Forwarded-Client-Cert` (XFCC). The currently supported `sanitized-single` policy accepts no field or exactly one physical field containing exactly one assertion; it does not accept repeated fields or an appended comma-separated multi-proxy history. The trusted proxy must therefore replace the field rather than append to it. With Envoy, use its `SANITIZE_SET` behavior. `format = "text"` selects the semicolon-delimited `Key=Value` representation; JSON and other vendor-specific encodings are not accepted.

This requires explicit attention in multi-proxy service meshes. [Istio ingress gateways default to `SANITIZE_SET`, while sidecars default to `APPEND_FORWARD`](https://istio.io/latest/docs/ops/configuration/traffic-management/network-topologies/#configuring-x-forwarded-client-cert-headers). A sidecar can therefore append a workload assertion to the ingress assertion and produce a multi-entry history that Play rejects. Configure the proxy chain so the proxy directly connected to Play delivers exactly one assertion sanitized by a trusted component; Play does not guess an effective client certificate from an appended history.

Play recognizes repeatable `By`, `URI`, and `DNS` values and singleton `Hash`, `Cert`, `Chain`, and `Subject` values. Their order is retained in `request.xForwardedClientCertificates`. An assertion containing only identity metadata, for example a SPIFFE `URI`, is valid and does not invent an X.509 certificate. When present, `Hash` is the hexadecimal SHA-256 digest of the leaf certificate.

Envoy's text form emits `By` and `URI` values without escaping its comma and semicolon delimiters. Those characters therefore cannot be recovered unambiguously: identities used with this mode must not contain raw `,` or `;` in `By` or URI SAN values. Percent-encode them when issuing URI identities. An empty `URI=` marker, which Envoy emits when URI forwarding is enabled but the certificate has no URI SAN, is accepted without adding an empty identity.

`Cert` is a percent-encoded PEM leaf certificate. Envoy's `Chain` value is a percent-encoded PEM sequence that includes the leaf; Play normalizes it so both `XForwardedClientCert.chain` and `ClientCertificateInfo.chain` contain only the remaining chain certificates. This is the chain presented by the client and is not necessarily the chain that the proxy validated. If both `Cert` and `Chain` are present, their leaf certificates must match. When either value supplies a certificate, `request.clientCertificate` uses the `XForwardedClientCert` source. The accepted assertion remains available separately so applications can inspect its other metadata.

Malformed XFCC input from a configured, trusted proxy is rejected with HTTP 400 rather than being partly accepted. Exceeding a configured parser limit in either forwarded-certificate mode also rejects the request. Input from an untrusted peer is ignored by the typed APIs without being parsed. These rules apply independently of `play.http.forwarded.version`, which selects only remote address, scheme, and authority forwarding.

#### Certificate trust and deployment requirements

Parsing a forwarded certificate proves only that the field has the expected syntax and contains a parseable X.509 object. Play does not perform PKIX path validation, check certificate validity or revocation, prove possession of the private key, map a certificate to an application principal, or authorize the request. An XFCC hash consistency check likewise does not authenticate the assertion. The TLS-terminating proxy must authenticate the client certificate according to deployment policy, and the application must perform any required identity mapping and authorization.

Before enabling either mode, enforce all of the following:

* Prevent clients from connecting directly to Play, so they cannot bypass the proxy and supply certificate fields themselves.
* On every request, have the trusted edge remove all incoming `Client-Cert`, `Client-Cert-Chain`, and `X-Forwarded-Client-Cert` fields before setting the one configured representation. It must also remove them when client-certificate authentication did not occur.
* Protect and authenticate the proxy-to-Play path with mutually authenticated TLS or equivalently strong network isolation. An IP address in `trustedProxies` does not itself protect the assertion from interception, modification, or source-address impersonation.
* Put only the proxy addresses that are authoritative for client-certificate assertions in `clientCertificates.trustedProxies`; do not copy a broader forwarding trust range without reviewing it.

Certificate fields can exceed Play's default `play.server.max-header-size` of `8k`, especially when a chain is included. Increase that server setting when required, while keeping the client-certificate parser limits as small as the deployment permits:

```hocon
play.server.max-header-size = 64k
```

Finally, do not let a shared cache reuse certificate-dependent content across clients. When `Client-Cert` influences the response, RFC 9440 requires either an uncacheable response such as `Cache-Control: no-store` or `Vary: Client-Cert`; it also recommends that the TLS-terminating proxy turn that `Vary` value into `*` before returning the response to the user agent. `Cache-Control: no-store` is the safest default for certificate-dependent responses, including those based on XFCC metadata. Play does not add these response fields automatically.
