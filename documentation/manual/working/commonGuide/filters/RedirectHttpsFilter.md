<!--- Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com> -->
# Redirect HTTPS Filter

Play provides a filter which will redirect all HTTP requests to HTTPS automatically.

## Enabling the HTTPS filter

To enable the filter, add it to `play.filters.enabled`:

```
play.filters.enabled += play.filters.https.RedirectHttpsFilter
```

## Determining Secure Requests

The filter evaluates a request to be secure if `request.secure` is true.

This logic is set by the [[trusted proxies|HTTPServer#configuring-trusted-proxies]] configured for Play's HTTP engine.  Internally, `play.core.server.common.ForwardedHeaderHandler` and `play.api.mvc.request.RemoteConnection` determine between them whether an incoming request meets the criteria to be "secure", meaning that the request has gone through HTTPS at some point.

A request that is not secure is redirected according to the redirect code determined through configuration.

## Strict Transport Security

The [Strict Transport Security](https://en.wikipedia.org/wiki/HTTP_Strict_Transport_Security) header is used to indicate when HTTPS should always be used, and is added to a secure request.

This header is only enabled in production, since HSTS can cause problems in development by forcing the browser to always assume https://localhost:9000 for the application -- this is especially an issue when working with multiple projects.

The default is "max-age=31536000; includeSubDomains", and can be set explicitly by adding the following to `application.conf`:

```
play.filters.https.strictTransportSecurity="max-age=31536000; includeSubDomains"
```

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

then the URL in the `Location` header will include the port specifically, e.g. https://playframework.com:9443/some/url

