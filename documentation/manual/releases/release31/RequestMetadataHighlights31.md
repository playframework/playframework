<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Typed request and forwarded metadata

Play now models the selected remote identity, direct transport connection, and effective request scheme and authority as separate typed values. This page describes those additions and Play's forwarded-header handling. For upgrade instructions, see [[Request metadata and forwarded-header migration|RequestMetadataMigration31]].

## RFC 7239 Remote Identities

Play exposes selected remote metadata through `RequestHeader.remote` in Scala and `Http.RequestHeader.remote()` in Java. Its typed `node` can be an IP address, `unknown`, or an obfuscated identifier; `identity` renders that node, and `ipAddress` is present only for IP nodes.

This supports [RFC 7239](https://tools.ietf.org/html/rfc7239) `Forwarded` identifiers that are not IP addresses, such as `for=unknown` and obfuscated identifiers like `for=_hidden`, without inventing a fallback IP address.

For RFC 7239 `Forwarded` headers, Play also exposes the selected element's `by` parameter through `RequestHeader.remote.byNode` in Scala and `Http.RequestHeader.remote().byNode()` in Java. This identifies the proxy interface that received the request represented by `node`.

`RequestHeader.remote.forwarding` retains whether the selected endpoint came from RFC 7239 or X-Forwarded metadata and the accepted intermediate endpoints Play traversed within the configured trust boundary. `RequestHeader.remote.path` exposes the selected endpoint followed by those intermediates in client-to-Play order. For a direct request, that selected endpoint is the direct socket peer. For a forwarded request, the path excludes the direct socket peer, which remains available through `RequestHeader.transport.peer`. It always excludes uninspected header values.

Play can also use the selected trusted RFC 7239 `host` parameter for `RequestHeader.host`, allowing applications behind trusted proxies to reconstruct the original public host from standards-based forwarding information. This behavior is disabled by default and can be enabled with `play.http.forwarded.trustForwardedHost = true`. A grammatically valid authority whose host component is empty cannot replace the effective request authority; Play keeps the last verified authority while applying the element's other valid metadata.

## Identity-aware IP filtering

The IP filter now matches the typed selected remote identity. Existing `whiteList` and `blackList` entries can be numeric IPv4/IPv6 literals, CIDR networks, `unknown`, or exact obfuscated identities such as `_edge`. Whitelists remain fail-closed; blacklists deny only matching identities, so an unrelated IP entry no longer rejects every non-IP remote identity.

Matcher configuration is parsed strictly without DNS resolution. Hostnames, endpoint ports, malformed identifiers, and invalid CIDR prefixes fail application startup instead of producing ambiguous access rules.

## Direct Transport Metadata

`RequestHeader.transport` in Scala and `Http.RequestHeader.transport()` in Java retain the immutable network connection directly terminating at Play. `transport.peer` contains the socket peer address and source port, while `transport.tls` describes actual peer-to-Play TLS and peer certificates. Selecting a forwarded remote identity, scheme, or authority never replaces these transport facts.

## Source-aware client certificates

`RequestHeader.clientCertificate` in Scala and `Http.RequestHeader.clientCertificate()` in Java expose the effective X.509 client certificate selected for application use. The value separates the leaf certificate from the rest of its chain and records whether Play obtained it from direct transport TLS, the standardized RFC 9440 `Client-Cert` fields, or `X-Forwarded-Client-Cert`.

Selecting a trusted forwarded certificate never replaces `RequestHeader.transport.tls`, which continues to describe the physical connection terminating at Play. For `X-Forwarded-Client-Cert`, `RequestHeader.xForwardedClientCertificates` also retains the accepted assertion metadata, including identities that do not carry a certificate.

Forwarded certificate interpretation is disabled by default and uses a dedicated trusted-proxy list, parser limits, and protocol mode. See [[Forwarded client certificates|HTTPServer#forwarded-client-certificates]] for the supported RFC 9440 and sanitized Envoy XFCC profiles and their deployment requirements.

## First-class Request Scheme and Authority

The effective request scheme and authority are now first-class immutable values through `RequestHeader.scheme` and `RequestHeader.authority`, with corresponding Java APIs. Schemes use RFC 3986 syntax, and authorities distinguish registered names, IPv4, IPv6, and IPvFuture while preserving arbitrary-size decimal URI ports. `request.secure`, `request.host`, `request.domain`, and the canonical exposed `Host` field are derived from these values.

For origin-form, asterisk-form, and CONNECT requests, the initial scheme comes from direct transport TLS, and accepted trusted forwarding metadata can select the effective public scheme. An absolute-form request target supplies its own scheme and, for HTTP and HTTPS, a required non-empty authority. A trusted gateway may preserve the public absolute target or rewrite it to its backend scheme; Play accepts either when the target scheme matches the effective forwarded scheme or the direct transport scheme. The trusted forwarded scheme becomes the application-facing scheme while `RequestHeader.uri` retains the raw target. Play rejects an absolute scheme that describes neither side of the trusted hop. Trusted forwarding metadata can also replace the effective public authority. When a rejected request is reported to a custom `HttpErrorHandler`, the handler receives a best-effort `RequestHeader` that can retain both the raw target and independently valid forwarding metadata. Ordinary request copies preserve the effective values; applications use `withScheme` or `withAuthority` when an explicit change is intended. Scala `FakeRequest` and Java `RequestBuilder` additionally treat one supplied `Host` value as a test-helper convenience that replaces and canonicalizes the effective authority; duplicate or invalid values are rejected.

HTTP/1.1 requests must contain exactly one syntactically valid `Host` field even when an absolute-form or CONNECT target supplies the effective authority. Origin-form and asterisk-form requests require that field to contain a non-empty host. For absolute-form and CONNECT, Play follows request-target precedence: an empty `Host` field is accepted and the canonical application-facing `Host` is derived from the target authority. HTTP and HTTPS absolute targets require a non-empty authority; other absolute schemes are rejected as soon as their scheme is recognized without parsing the remaining URI. CONNECT accepts only authority-form targets with a non-empty host and a usable destination port from `1` through `65535`. The Netty backend passes this absolute-form traffic to Play's validation. Pekko HTTP currently rejects an empty or different `Host` while establishing its effective URI, before Play request conversion runs.

Known RFC 7239 obfuscated proxy identifiers can also be trusted explicitly:

```hocon
play.http.forwarded.trustedProxyIdentifiers = ["_edge"]
```

This allows Play to continue scanning through configured obfuscated proxy identifiers. The setting only applies to RFC 7239 `Forwarded` headers and does not make the `unknown` identifier trusted.

Play also validates RFC 7239 field syntax, including tokens, quoted strings, quoted-pair escapes, HTTP lists, and duplicate parameters. Before applying an element, Play atomically validates every recognized `for`, `by`, `host`, and `proto` value. If any is invalid, none of that element changes the selected remote, scheme, or authority, and scanning stops at the last verified state. A `host` value with an empty host component conforms to the referenced grammar but is unusable as an effective HTTP authority, so Play ignores that authority without discarding the element's other valid metadata. Syntactically valid extension parameters remain ignorable. Existing support for unquoted `for` node values containing IPv6 addresses or ports remains available for Play 3.0 compatibility. Play applies the same allowance to `by` values for consistent node parsing. For the same compatibility reason, an optional sequence of spaces and tabs is accepted immediately after a semicolon between parameters. Proxies should emit quoted node values and the compact RFC syntax without parameter-separator whitespace.

## Remote Node Port

Play exposes a selected node's numeric or obfuscated port through `RequestHeader.remote.nodePort` in Scala and `Http.RequestHeader.remote().nodePort()` in Java. `port` is the numeric convenience projection. RFC 7239 node ports are preserved on IP, `unknown`, and obfuscated nodes. Numeric values are validated as network endpoint ports from `0` through `65535`; syntactically five-digit values above that range are rejected.

The Netty and Pekko HTTP server backends populate this value from the raw socket connection. A selected forwarded remote identity can instead provide its endpoint port through an RFC 7239 `for` node or a port embedded in `X-Forwarded-For`. `X-Forwarded-Port` describes the original destination port and is not exposed as a remote endpoint port.

Applications using `X-Forwarded-*` headers can opt in to reconstructing the effective request authority from one trusted `X-Forwarded-Host` value with a non-empty host component using `play.http.forwarded.trustXForwardedHost`, and from one trusted destination port using `play.http.forwarded.trustXForwardedPort`. A port-only forwarded host is ignored; an independently valid and trusted `X-Forwarded-Port` can still update the retained authority. This affects request authority consumers such as absolute URL generation without changing `request.remote.port`.

## Trusting Single X-Forwarded-Proto Values

Play can now be configured to trust a single `X-Forwarded-Proto` value when `X-Forwarded-For` contains multiple addresses:

```hocon
play.http.forwarded.trustSingleXForwardedProto = true
```

This helps deployments where trusted proxy chains append to `X-Forwarded-For`, but the edge proxy sets one `X-Forwarded-Proto` value for the original client request. The setting is disabled by default and only applies to `play.http.forwarded.version = "x-forwarded"`.

Only enable it when the trusted edge proxy overwrites or strips any incoming client-supplied `X-Forwarded-Proto` header before setting the correct value.

Play can also be configured to trust a single `X-Forwarded-Proto` value when `X-Forwarded-For` is absent:

```hocon
play.http.forwarded.trustXForwardedProtoWithoutXForwardedFor = true
```

This setting updates the effective request scheme, but does not change the selected remote identity.

For compatibility with older proxies, Play can also trust one `X-Forwarded-Ssl: on` or `X-Forwarded-Ssl: off` value when `X-Forwarded-Proto` is absent:

```hocon
play.http.forwarded.trustXForwardedSsl = true
```

This setting works with or without `X-Forwarded-For`, remains subject to trusted-proxy validation, and is disabled by default.
