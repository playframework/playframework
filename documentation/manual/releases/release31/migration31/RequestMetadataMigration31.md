<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Request metadata and forwarded-header migration

This page collects the request metadata, forwarded-header, and affected filter changes for applications migrating to Play 3.1. For a conceptual overview of the new model, see [[Typed request and forwarded metadata|RequestMetadataHighlights31]].

## Remote connection APIs replaced by typed remote and transport metadata

Play now supports RFC 7239 `Forwarded` remote identifiers that are not IP addresses, such as `for=unknown` and obfuscated identifiers like `for=_hidden`. This change removes the legacy `RemoteConnection`, `connection`, `remoteAddress`, and request certificate-chain APIs because they mixed selected forwarding metadata with facts about the socket terminating at Play.

This is an intentional source- and binary-incompatible public API change. Applications and libraries compiled against the removed types or methods must be migrated and recompiled for this Play release.

Use `RequestHeader.remote` when you need the structured identity selected from trusted forwarded headers. Its `node` is a `RemoteNode`, `identity` renders that node, and `ipAddress` contains an address only when the selected node is an IP identity.

Use `RequestHeader.transport.peer` when you need the socket peer that connected directly to Play, including its source port. Actual proxy-to-Play TLS and peer certificates are available through `RequestHeader.transport.tls`. These transport values remain unchanged when trusted forwarding selects a different logical remote node.

Java request accessors migrate as follows:

| Removed API | Replacement |
| --- | --- |
| `Http.RequestHeader.remoteAddress()` | Use `remote().identity()` for the complete selected identity, which can also be `unknown` or obfuscated. Use `remote().ipAddress()` when the application specifically requires an IP address. |
| `Http.RequestHeader.clientCertificateChain()` | Use `transport().tls().map(Http.TransportTls::peerCertificates)` to preserve the old direct-connection semantics. Use `clientCertificate()` when the application intentionally needs Play's new effective certificate selection. |

Java test builders replace the former connection accessors and setters with the corresponding typed values:

| Removed API | Replacement |
| --- | --- |
| `RequestBuilder.remoteAddress()` and `remoteAddress(...)` | Use `remote()` and `remote(...)` with a `RemoteInfo` containing a typed `RemoteNode`. |
| `RequestBuilder.clientCertificateChain()` and `clientCertificateChain(...)` | Use `transport()` and `transport(...)` with certificates stored in `TransportTls` for direct TLS metadata. Set `clientCertificate(...)` separately when code under test reads the effective certificate. |

For example:

```java
InetAddress remoteAddress = InetAddress.getByName("192.0.2.10");
Http.RemoteInfo remote =
    new Http.RemoteInfo(
        new Http.RemoteNode.Ip(
            remoteAddress, Optional.of(new Http.NodePort.Numeric(53124))),
        Optional.empty());
Http.PeerEndpoint peer =
    new Http.PeerEndpoint(InetAddress.getByName("127.0.0.1"), Optional.of(44000));
Http.TransportConnection transport =
    new Http.TransportConnection(
        peer, Optional.of(new Http.TransportTls(certificates)));
Http.ClientCertificateInfo clientCertificate =
    new Http.ClientCertificateInfo(
        certificates.get(0),
        certificates.subList(1, certificates.size()),
        Http.ClientCertificateSource.DIRECT_TRANSPORT);

Http.RequestBuilder request =
    new Http.RequestBuilder()
        .remote(remote)
        .transport(transport)
        .clientCertificate(clientCertificate)
        .scheme(Http.Scheme.HTTPS);
```

The full Scala `FakeRequest.apply` overload likewise replaces `remoteAddress`, `secure`, and `clientCertificateChain` with separate `remote`, `scheme`, `transport`, and effective-certificate metadata. Code that previously changed the combined value through `withConnection` should now change only the relevant dimension with `withRemote`, `withScheme`, `withTransport`, or `withClientCertificate`:

```scala
val remote = RemoteInfo.ip(
  InetAddress.getByName("192.0.2.10"),
  Some(NodePort.Numeric(53124))
)
val peer      = PeerEndpoint(InetAddress.getByName("127.0.0.1"), Some(44000))
val transport = TransportConnection(peer, Some(TransportTls(certificates)))
val clientCertificate = certificates.headOption.map { certificate =>
  ClientCertificateInfo(
    certificate,
    certificates.drop(1).toVector,
    ClientCertificateSource.DirectTransport
  )
}

val request = FakeRequest(GET, "/")
  .withRemote(remote)
  .withTransport(transport)
  .withClientCertificate(clientCertificate)
  .withScheme(Scheme.Https)
```

For example, with a trusted direct proxy at `127.0.0.1`:

```http
Forwarded: for=_hidden;proto=https
```

Play reports:

```scala
request.remote.node      // RemoteNode.Obfuscated("_hidden", None)
request.remote.identity  // "_hidden"
request.remote.ipAddress // None
request.remote.nodePort  // None, or a numeric/obfuscated NodePort
request.secure           // true
```

## Effective client certificates are separate from direct transport TLS

`RequestHeader.transport.tls` always describes TLS on the connection directly terminating at Play. `RequestHeader.clientCertificate` is a separate effective value for application use, containing a leaf certificate, the remaining chain, and a `DirectTransport`, `Rfc9440`, or `XForwardedClientCert` source.

When forwarded certificate handling is off, or the directly connected peer is not trusted for certificate assertions, Play selects an observed direct peer certificate with the `DirectTransport` source. When a trusted RFC 9440 or XFCC mode is enabled, the configured header protocol describes the original client instead. If that trusted proxy supplies no client-certificate assertion, the effective value is empty rather than falling back to the proxy's own transport certificate. The immutable direct TLS information remains available through `transport.tls` in every case.

Custom Scala `RequestHeader` and `RequestFactory` implementations must provide and preserve both `clientCertificate` and the ordered `xForwardedClientCertificates` assertions. Test code sets these dimensions independently: use `FakeRequest.withClientCertificate` and `withXForwardedClientCertificates` in Scala, or `RequestBuilder.clientCertificate(...)` and `xForwardedClientCertificates(...)` in Java. Changing only `transport` deliberately does not rewrite either effective value.

See [[Forwarded client certificates|HTTPServer#forwarded-client-certificates]] for protocol configuration, trust-boundary requirements, parser limits, and the distinction between certificate parsing and authentication or authorization.

## IP filter entries match typed remote identities

The IP filter now evaluates `RequestHeader.remote.node` rather than only its optional IP projection. The existing `play.filters.ip.whiteList` and `blackList` keys accept numeric IPv4/IPv6 literals, CIDR networks, `unknown`, and exact RFC 7239 obfuscated identifiers such as `_edge`.

A nonempty whitelist remains fail-closed and allows only matching nodes. When the whitelist is empty, a blacklist now denies only matching nodes. Consequently, an unrelated IP blacklist entry no longer implicitly rejects an `unknown` or obfuscated selected remote; list that identity explicitly or use a whitelist when unlisted identities must be denied. If both lists are nonempty, the existing whitelist precedence is unchanged.

List parsing is now strict, accepts only printable ASCII, and performs no DNS lookup. IPv4 entries must use canonical four-part dotted-decimal notation, with no leading zero unless an octet is exactly `0`.

The former `InetAddress.getByName` parser accepted more than numeric IP literals. Replace DNS names such as `localhost`, shorthand IPv4 such as `127.1`, integer IPv4 such as `2130706433`, and leading-zero IPv4 such as `001.002.003.004` with canonical numeric literals. Remove brackets from IPv6 literals, and remove or replace IPv6 zone identifiers; acceptance of scoped addresses depended on the local interfaces. An empty entry previously resolved to the loopback address and must now be written explicitly. Values such as `unknown` and `_edge` were formerly passed to name resolution and could match a resolved IP; they now mean the RFC 7239 unknown identity and an exact obfuscated identity, respectively.

Whitespace-padded entries and endpoint notation such as `192.0.2.1:443` were already invalid under the former parser and remain invalid. Non-ASCII values, malformed identities, and invalid CIDR prefixes are also rejected at startup. Matching uses only the selected node identity: its port, RFC `by` node, and direct transport peer do not affect the result.

`play.http.forwarded.trustedProxies` now likewise accepts only ASCII numeric IPv4/IPv6 literals and CIDR networks. Scoped IPv6 spellings such as `fe80::1%1` and addresses written with non-ASCII decimal digits now fail application startup instead of silently discarding a scope or normalizing the digits.

## Request scheme and authority are first-class values

`RequestHeader.scheme` and `RequestHeader.authority` now carry the effective destination metadata independently from the remote client and direct transport. `secure`, `host`, `domain`, and the exposed `Host` header are derived from those values. Custom Scala `RequestHeader` implementations must provide both fields, and custom `RequestFactory` implementations must accept and preserve them.

Immutable core request-copy operations no longer infer authority from a changed target or let generic header replacement create contradictory host state. Use `withScheme` and `withAuthority` in Scala when changing these values deliberately. Core `RequestHeader.withHeaders` may omit the canonical `Host`, but a conflicting or duplicate `Host` is rejected.

The request-building helpers retain a convenient Host mutation path. Scala `FakeRequest.withHeaders` and Java `RequestBuilder.headers(...)` or `header(...)` treat exactly one case-insensitive `Host` value as an authority replacement, parse and canonicalize it, and keep the typed authority and exposed header synchronized. Omitting `Host` preserves the existing authority; duplicate or invalid values throw `IllegalArgumentException`. Java builders can also use `scheme(...)` and `authority(...)` directly.

Java `RequestBuilder.uri(...)` now changes only the synthetic request target. Code that previously relied on it to change the effective scheme or host must set those values explicitly.

For requests received by a Play server, an absolute-form target supplies its scheme and any authority it contains. A trusted gateway may preserve the public absolute target or rewrite it to the scheme of its backend connection; Play accepts either when the target scheme matches the final effective forwarded scheme or the direct transport scheme. The trusted forwarded scheme becomes the application-facing `RequestHeader.scheme`, while `RequestHeader.uri` retains the raw target. An accepted HTTP or HTTPS absolute-form target with an empty path now exposes `/` through `RequestHeader.path`, while its raw target and query remain unchanged. Play rejects an absolute scheme that describes neither side of the trusted hop. Consequently, a client cannot make a plaintext connection secure merely by writing `https://` without accepted scheme-changing forwarding metadata. When a rejected request is reported to a custom `HttpErrorHandler`, the best-effort `RequestHeader` supplied for error handling can retain the raw absolute target alongside independently valid forwarding metadata.

Play now rejects an HTTP/1.1 request with a missing, duplicate, or syntactically invalid `Host` field before applying absolute-form or CONNECT authority precedence. Origin-form and asterisk-form requests require a non-empty Host authority. Absolute-form and CONNECT requests may carry an empty `Host` field because their request target supplies the effective authority; Play exposes that target authority as the canonical `Host`. HTTP and HTTPS absolute targets always require a non-empty authority. Other absolute schemes are rejected as soon as their scheme is recognized without parsing the remaining URI. HTTP/1.0 requests retain missing-Host compatibility when the selected server backend accepts them. CONNECT now accepts only authority-form targets with a non-empty host and a destination port from `1` through `65535`; port zero, oversized ports, and absolute-form CONNECT targets are rejected. Netty passes this absolute-form traffic through to Play. Pekko HTTP currently rejects an empty or different `Host` while establishing its effective URI, before Play can apply target precedence.

## Missing, invalid, or ambiguous X-Forwarded-Proto retains the last verified scheme

When Play accepts an `X-Forwarded-For` identity but cannot associate it with a valid `X-Forwarded-Proto` value, Play now retains the last verified effective scheme. This applies when the proto value is missing or invalid, and when the `X-Forwarded-For` and `X-Forwarded-Proto` lists cannot be paired according to the configured single-proto policy. The selected remote identity can still change; unverified protocol metadata no longer forces that identity to be insecure.

For example, suppose a trusted proxy connects to Play over TLS and sends:

```http
X-Forwarded-For: 203.0.113.43
```

with no `X-Forwarded-Proto`. Play 3.0 selected the forwarded client address but projected the request as insecure, so `request.secure` was `false`. Play now selects the same remote identity while retaining the HTTPS scheme verified from the proxy-to-Play transport, so `request.scheme == Scheme.Https` and `request.secure == true`.

This can change redirect and HSTS behavior, absolute and WebSocket URL generation, CORS same-origin decisions, and application policy that reads `request.secure` or `request.scheme`. If the public client scheme can differ from the proxy-to-Play transport scheme, configure the trusted proxy to emit a valid, correctly aligned `X-Forwarded-Proto` value; otherwise Play can only retain the last scheme it has verified.

## CORS same-origin checks use effective request metadata

The CORS filter now compares the `Origin` header with the request's normalized effective scheme, host, and port from `RequestHeader.scheme` and `RequestHeader.authority`. An omitted port is equivalent to port `80` for HTTP or `443` for HTTPS. For example, `http://www.example.com` and `http://www.example.com:80` are now the same origin, as are the corresponding HTTPS forms with port `443`.

Accepted trusted forwarding metadata can therefore change the request origin used by CORS. A trusted forwarded scheme, host, or port can make the public origin match even when the direct proxy-to-Play scheme or internal `Host` differs. The CORS filter does not read `Forwarded` or `X-Forwarded-*` fields directly: only metadata accepted from a configured trusted proxy through enabled forwarding options affects the comparison. Untrusted, disabled, or invalid forwarding metadata has no CORS effect. Review CORS behavior during upgrade if your deployment exposes different public and internal origins, and see [[configuring trusted proxies|HTTPServer#configuring-trusted-proxies]].

## RFC 7239 Forwarded header syntax is validated

Play now validates RFC 7239 `Forwarded` field values before using them. Parameter names must be valid tokens, values must be tokens or quoted strings, and a parameter cannot occur more than once in one forwarded element. Empty HTTP list elements remain accepted and are ignored.

RFC 7239 requires IPv6 addresses and node identifiers with ports to be quoted because they contain `:`:

```http
Forwarded: for="[2001:db8:cafe::17]:4711"
Forwarded: for="192.0.2.43:4711"
```

For compatibility with Play 3.0, Play continues to accept these values without quotes in the `for` parameter. Play applies the same allowance to `by` for consistent node parsing. Play also accepts an optional sequence of spaces and tabs immediately after the semicolon between parameters, as in `for=192.0.2.43; proto=https`. Whitespace before the semicolon or around `=` remains invalid. These are bounded compatibility allowances; proxy configurations should emit quoted node values and the compact RFC syntax without parameter-separator whitespace. Other parameters do not receive the unquoted-value allowance.

Play also validates every recognized value in an otherwise syntactically valid element: `for` and `by` must be valid node identifiers, `host` must conform to request-authority syntax, and `proto` must be a valid RFC 3986 scheme. This validation is atomic and is performed even when a feature such as forwarded-host application is disabled. If any recognized value is invalid, Play applies none of that element, stops scanning older elements, and keeps the last verified remote, scheme, and authority. Syntactically valid unknown extension parameters remain ignored.

The referenced `Host` grammar permits a registered name with zero characters, including a port-only value such as `host=":8080"`. Such a value remains grammatically valid but cannot establish Play's effective HTTP authority. When forwarded-host application is enabled, Play ignores that authority as a unit, including its embedded port, retains the last verified authority, and still applies the element's other valid metadata. The same non-empty-host requirement applies to `X-Forwarded-Host`. In `x-forwarded` mode, a separately trusted `X-Forwarded-Port` remains independent and can update a retained non-empty authority.

When Play encounters a malformed `Forwarded` field while scanning a trusted proxy chain, it likewise stops at that field and keeps the last verified request information. Check that each trusted proxy emits valid RFC 7239 syntax and recognized values before upgrading.

## Redirect HTTPS filter only reads X-Forwarded-Proto when enabled

The Redirect HTTPS filter previously treated `X-Forwarded-Proto: https` as a secure request even when `play.filters.https.xForwardedProtoEnabled` was `false`. The filter now ignores `X-Forwarded-Proto` unless that legacy option is explicitly enabled. Deployments that relied on this implicit header handling may otherwise repeatedly redirect a public HTTPS request when a proxy terminates HTTPS but Play still sees the proxy connection as insecure.

Applications behind a trusted proxy should normally configure [[trusted proxies|HTTPServer#configuring-trusted-proxies]] so Play derives `request.secure` after validating the forwarded proxy chain. If a trusted proxy sends a single `X-Forwarded-Proto` value without `X-Forwarded-For`, enable:

```hocon
play.http.forwarded.trustXForwardedProtoWithoutXForwardedFor = true
```

The immediate proxy must also be included in `play.http.forwarded.trustedProxies`. Applications that intentionally rely on the filter reading `X-Forwarded-Proto` directly can instead set:

```hocon
play.filters.https.xForwardedProtoEnabled = true
```

This legacy option changes more than the secure-request check: the filter redirects only requests with `X-Forwarded-Proto: http`. For an otherwise insecure request, a missing or unexpected value causes the filter to pass the request to the application without an HTTPS redirect or HSTS header, and the option does not update `request.secure`. Only enable it when bypassing redirects for requests without the header is intentional, or when a trusted proxy removes or overwrites every client-supplied value and reliably sends either `http` or `https`.
