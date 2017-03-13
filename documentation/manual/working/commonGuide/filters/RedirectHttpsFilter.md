<!--- Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com> -->
# Redirect HTTPS Filter

Play provides a filter which will redirect HTTP requests to HTTP requests.

## Enabling the HTTPS filter

To enable the filter, add it to `play.filters.enabled`:

```
play.filters.enabled += play.filters.https.RedirectHttpsFilter
```

## Trusted Proxies

The filter evaluates a secure request to be secure if `request.secure` is true.  This logic is set by the [[trusted proxies|HTTPServer#configuring-trusted-proxies]] configured for Play's HTTP engine.

## Strict Transport Security 

The [Strict Transport Security](https://en.wikipedia.org/wiki/HTTP_Strict_Transport_Security) header is used to indicate when HTTPS should always be used, and is provided on redirect if set.  The default is null, since HSTS can interfere with development if you are working on multiple projects.  To set HSTS, add the following to `application.conf`:

```
play.filters.https.strictTransportSecurity="max-age=31536000; includeSubDomains"
```

## Redirect code

This filter redirects using HTTP code 308, which is a permanent redirect that does not change the HTTP method according to [RFC 7238](https://tools.ietf.org/html/rfc7238).  This will work with the vast majority of browsers, but you can change the redirect code if working with older browsers:

```
play.filters.https.redirectCode = 301
``` 