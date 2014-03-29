<!--- Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com> -->
# Configuring Security Headers

Play provides a security headers filter that can be used to configure some default headers in the HTTP response to mitigate security issues and provide an extra level of defense for new applications..  It can be added to the applications filters using the `Global` object. To enable the security headers filter, add the Play filters helpers dependency to your project in `build.sbt`:

```scala
libraryDependencies += filters
```

## Enabling security headers in Scala

Scaladoc is available in the [play.filters.headers](api/scala/index.html#play.filters.headers.package) package.

The simplest way to enable the `SecurityHeaders` filter in a Scala project is to use the `WithFilters` helper:

@[global](code/SecurityHeaders.scala)

The filter will set headers in the HTTP response automatically.  The settings can can be configured through the following settings in `application.conf`

* `play.filters.headers.frameOptions` - sets <a href="https://developer.mozilla.org/en-US/docs/HTTP/X-Frame-Options">X-Frame-Options</a>, "DENY" by default.
* `play.filters.headers.xssProtection` - sets <a href="http://blogs.msdn.com/b/ie/archive/2008/09/02/ie8-security-part-vi-beta-2-update.aspx">X-Content-Type-Options</a>, "1; mode=block" by default.
* `play.filters.headers.contentTypeOptions` - sets <a href="http://blogs.msdn.com/b/ie/archive/2008/07/02/ie8-security-part-iv-the-xss-filter.aspx">X-XSS-Protection</a>, "nosniff" by default.
* `play.filters.headers.permittedCrossDomainPolicies` - sets <a href="http://www.adobe.com/devnet/articles/crossdomain_policy_file_spec.html">X-Permitted-Cross-Domain-Policies</a>, "master-only" by default.
* `play.filters.headers.contentSecurityPolicy` - sets <a href="http://www.html5rocks.com/en/tutorials/security/content-security-policy/">Content-Security-Policy</a>, "default-src: 'self'" by default.

> NOTE: Because these are security headers, they are "secure by default."  If the filter is applied, but these fields are NOT defined in Configuration, the defaults on the filter are NOT omitted, but are instead set to the strictest possible value.

The filter can also be configured on a custom basis in code:

@[custom-config](code/SecurityHeaders.scala)

## Enabling security headers in Java

To enable security headers in Java, add it to the list of filters in the `Global` object:

@[global](code/detailedtopics/configuration/headers/Global.java)
