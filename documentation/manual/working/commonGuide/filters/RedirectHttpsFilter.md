<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Redirect HTTPS Filter

Play provides a filter which will redirect all HTTP requests to HTTPS automatically.

## Enabling the HTTPS filter

To enable the filter, add it to `play.filters.enabled`:

```
play.filters.enabled += play.filters.https.RedirectHttpsFilter
```

By default, the redirect only happens in Prod mode. To override this, set `play.filters.https.redirectEnabled = true`.

## Determining Secure Requests

The filter evaluates a request to be secure if `request.secure` is true.

This logic depends on the [[trusted proxies|HTTPServer#configuring-trusted-proxies]] configured for Play's HTTP engine. Internally, `play.core.server.common.ForwardedHeaderHandler` and `play.api.mvc.request.RemoteConnection` determine between them whether an incoming request meets the criteria to be "secure", meaning that the request has gone through HTTPS at some point.

When the filter is enabled, any request that is not secure is redirected.

## Strict Transport Security

The [Strict Transport Security](https://en.wikipedia.org/wiki/HTTP_Strict_Transport_Security) header is used to indicate when HTTPS should always be used, and is added to a secure request. The HSTS header is only added if the redirect is enabled.

The default is "max-age=31536000; includeSubDomains", and can be set explicitly by adding the following to `application.conf`:

```
play.filters.https.strictTransportSecurity="max-age=31536000; includeSubDomains"
```

It is also possible to set `play.filters.https.strictTransportSecurity = null` to disable HSTS.

Note that the `Strict-Transport-Security` header tells the browser to prefer HTTPS for *all requests to that hostname*, so if you enable the filter in dev mode, the header will affect other apps being developed with that hostname (e.g. `localhost:9000`). If you want to avoid this, either use a different host for each app in development (`app1:9000`, `app2:9000`, etc.) or disable HSTS completely in dev mode.

## Redirect code

The filter redirects using HTTP code 308, which is a permanent redirect that does not change the HTTP method according to [RFC 7238](https://tools.ietf.org/html/rfc7238).  This will work with the vast majority of browsers, but you can change the redirect code if working with older browsers:

```
play.filters.https.redirectStatusCode = 301
```

## Custom HTTPS Port

If the HTTPS server is on a custom port, then the redirect URL needs to be aware of it.  If the port is specified:

```
play.filters.https.port = 9443
```

then the URL in the `Location` header will include the port specifically, e.g. `https://playframework.com:9443/some/url`.

