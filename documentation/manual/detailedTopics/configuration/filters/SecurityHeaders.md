<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Configuring Security Headers

Play provides a security headers filter that can be used to configure some default headers in the HTTP response to mitigate security issues and provide an extra level of defense for new applications.

## Enabling the security headers filter

To enable the security headers filter, add the Play filters project to your `libraryDependencies` in `build.sbt`:

@[content](code/filters.sbt)

Now add the security headers filter to your filters, which is typically done by creating a `Filters` class in the root of your project:

Scala
: @[filters](code/SecurityHeaders.scala)

Java
: @[filters](code/detailedtopics/configuration/headers/Filters.java)

The `Filters` class can either be in the root package, or if it has another name or is in another package, needs to be configured using `play.http.filters` in `application.conf`:

```
play.http.filters = "filters.MyFilters"
```

## Configuring the security headers

Scaladoc is available in the [play.filters.headers](api/scala/play/filters/headers/package.html) package.

The filter will set headers in the HTTP response automatically.  The settings can can be configured through the following settings in `application.conf`

* `play.filters.headers.frameOptions` - sets [X-Frame-Options](https://developer.mozilla.org/en-US/docs/HTTP/X-Frame-Options), "DENY" by default.
* `play.filters.headers.xssProtection` - sets [X-XSS-Protection](http://blogs.msdn.com/b/ie/archive/2008/07/02/ie8-security-part-iv-the-xss-filter.aspx), "1; mode=block" by default.
* `play.filters.headers.contentTypeOptions` - sets [X-Content-Type-Options](http://blogs.msdn.com/b/ie/archive/2008/09/02/ie8-security-part-vi-beta-2-update.aspx), "nosniff" by default.
* `play.filters.headers.permittedCrossDomainPolicies` - sets [X-Permitted-Cross-Domain-Policies](https://www.adobe.com/devnet/articles/crossdomain_policy_file_spec.html), "master-only" by default.
* `play.filters.headers.contentSecurityPolicy` - sets [Content-Security-Policy](http://www.html5rocks.com/en/tutorials/security/content-security-policy/), "default-src 'self'" by default.

Any of the headers can be disabled by setting a configuration value of `null`, for example:

    play.filters.headers.frameOptions = null

For a full listing of configuration options, see the Play filters [`reference.conf`](resources/confs/filters-helpers/reference.conf).
