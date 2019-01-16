<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Configuring Security Headers

Play provides a security headers filter that can be used to configure some default headers in the HTTP response to mitigate security issues and provide an extra level of defense for new applications.

## Enabling the security headers filter

> **Note:** As of Play 2.6.x, the Security Headers filter is included in Play's list of default filters that are applied automatically to projects.  See [[the Filters page|Filters]] for more information.

To enable the security headers filter manually, add the security headers filter to your filters in `application.conf`:

```
play.filters.enabled += "play.filters.headers.SecurityHeadersFilter"
```

## Configuring the security headers

Scaladoc is available in the [play.filters.headers](api/scala/play/filters/headers/index.html) package.

The filter will set headers in the HTTP response automatically.  The settings can be configured through the following settings in `application.conf`

* `play.filters.headers.frameOptions` - sets [X-Frame-Options](https://developer.mozilla.org/en-US/docs/HTTP/X-Frame-Options), "DENY" by default.
* `play.filters.headers.xssProtection` - sets [X-XSS-Protection](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-XSS-Protection), "1; mode=block" by default.
* `play.filters.headers.contentTypeOptions` - sets [X-Content-Type-Options](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Content-Type-Options), "nosniff" by default.
* `play.filters.headers.permittedCrossDomainPolicies` - sets [X-Permitted-Cross-Domain-Policies](https://www.adobe.com/devnet/articles/crossdomain_policy_file_spec.html), "master-only" by default.
* `play.filters.headers.referrerPolicy` - sets [Referrer Policy](https://www.w3.org/TR/referrer-policy/),  "origin-when-cross-origin, strict-origin-when-cross-origin" by default.
* `play.filters.headers.contentSecurityPolicy` - sets [Content-Security-Policy](https://developers.google.com/web/fundamentals/security/csp/), "default-src 'self'" by default.

Any of the headers can be disabled by setting a configuration value of `null`, for example:

```
play.filters.headers.frameOptions = null
```

For a full listing of configuration options, see the Play filters [`reference.conf`](resources/confs/filters-helpers/reference.conf).

## Action-specific overrides

Security headers may be overridden in specific actions using `withHeaders` on the result:

@[allowActionSpecificHeaders](code/SecurityHeaders.scala)

Any security headers not mentioned in `withHeaders` will use the usual configured values
(if present) or the defaults.  Action-specific security headers are ignored unless
`play.filters.headers.allowActionSpecificHeaders` is set to `true` in the configuration.
