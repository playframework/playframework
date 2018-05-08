<!--- Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com> -->
# Configuring Content Security Policy Headers

A good content security policy (CSP) is an essential part of securing a website.  Used properly, CSP can make XSS and injection much harder for attackers, although some attacks are [still possible](https://csp.withgoogle.com/docs/faq.html#caveats).

Play has a built in functionality for working with CSP, including rich support for CSP nonces and hashes.  There are two main approaches: a filter based approach that adds a CSP header to all responses, and an action based approach that only adds CSP when explicitly included.

> **Note**: The [[SecurityHeaders filter|SecurityHeaders]] has a `contentSecurityPolicy` property in configuration is deprecated.  Please see [[the deprecation section|CspFilter#Deprecation-of-SecurityHeaders.contentSecurityPolicy]].

## Enabling CSPFilter

The CSPFilter will set the content security policy header on all requests by default.

### Enabling Through Configuration

You can enable the new [`play.filters.csp.CSPFilter`](api/scala/play/filters/csp/CSPFilter.html) by adding it to `application.conf`:

```hocon
play.filters.enabled += play.filters.csp.CSPFilter
```

### Enabling Through Compile Time

The CSP components are available as compile time components, as described in [[Compile Time Default Filters|Filters#Compile-Time-Default-Filters]].

To add the filter in Scala compile time DI, include the [`play.filters.csp.CSPComponents`](api/scala/play/filters/csp/CSPComponents.html) trait.

To add the filter in Java compile time DI, include the [`play.filters.components.CSPComponents`](api/java/play/filters/components/CSPComponents.html).

Java
: @[java-csp-components](code/javaguide/detailed/filters/csp/MyComponents.java)

Scala
: @[scala-csp-components](code/scalaguide/detailed/filters/csp/MyComponents.scala)

### Selectively Disabling Filter with Route Modifier

Adding the filter will add a `Content-Security-Policy` header to every request.  There may be individual routes where you do not want the filter to apply, and the `nocsp` route modifier may be used here, using the [[route modifier syntax|ScalaRouting#The-routes-file-syntax]].

In your `conf/routes` file:

```
+ nocsp
GET     /my-nocsp-route         controllers.HomeController.myAction
```

This excludes the `GET /my-csp-route` route from the CSP filter.

## Enabling CSP on Specific Actions

If enabling CSP across all routes is not practical, CSP can be enabled on specific actions instead:

Java
: @[csp-action-controller](code/javaguide/detailed/filters/csp/CSPActionController.java)

Scala
: @[csp-action-controller](code/scalaguide/detailed/filters/csp/CSPActionController.scala)

## Configuring CSP

The CSP filter is driven primarily through configuration under the `play.filters.csp` section.

### Deprecation of SecurityHeaders.contentSecurityPolicy

The  [[SecurityHeaders filter|SecurityHeaders]] has a `contentSecurityPolicy` property in configuration is deprecated.  The functionality is still enabled, but `contentSecurityPolicy` property's default setting has been changed from `default-src ‘self’` to `null`.

If `play.filters.headers.contentSecurityPolicy` is not null, you will receive a warning.  It is technically possible to have `contentSecurityPolicy` and the new `CSPFilter` active at the same time, but this is not recommended.

> **Note:** You will want to review the Content Security Policy specified in the CSP filter closely to ensure it meets your needs, as it differs substantially from the previous `contentSecurityPolicy`.

### Configuring CSP Report Only

CSP has a feature which will place CSP violations into a "report only" mode, which results in the browser allowing the page to render and sending a CSP report to a given URL.

CSP reports are formatted as JSON.  For your convenience, Play provides a body parser that can parse a CSP report, useful when first adopting a CSP policy.  You can add a CSP report controller to send or store the CSP report at your convenience:

Java
: @[csp-report-controller](code/javaguide/detailed/filters/csp/CSPReportController.java)

Scala
: @[csp-report-controller](code/scalaguide/detailed/filters/csp/CSPReportController.scala)

To configure the controller, add it as a route in `conf/routes`:

```
+ nocsrf
POST     /report-to                 controllers.CSPReportController.report
```

Note that if you have the CSRF filter enabled, you may need `+ nocsrf` route modifier, or add `play.filters.csrf.contentType.whiteList += "application/csp-report"` to `application.conf` to whitelist CSP reports.

The report feature is enabled by setting the `reportOnly` flag, and configuring the `report-to` and `report-uri` CSP directives in `conf/application.conf`:

```hocon
play.filters.csp {
  reportOnly = true
  directives {
    report-to = "http://localhost:9000/report-to"
    report-uri = ${play.filters.csp.directives.report-to}
  }
}
```

CSP reports come in four different styles: "Blink", "Firefox", "Webkit", and "Old Webkit".  Zack Tollman has a good blog post [What to Expect When Expecting Content Security Policy Reports](https://www.tollmanz.com/content-security-policy-report-samples/) that discusses each style in detail.

### Configuring CSP Hashes

CSP allows inline scripts and styles to be whitelisted by [hashing the contents](https://www.w3.org/TR/CSP2/#script-src-hash-usage) and providing it as a directive.

Play provides an array of configured hashes that can be used to organize hashes via a referenced pattern.  In `application.conf`:

```hocon
play.filters.csp {
  hashes += {
    algorithm = "sha256"
    hash = "RpniQm4B6bHP0cNtv7w1p6pVcgpm5B/eu1DNEYyMFXc="
    pattern = "%CSP_MYSCRIPT_HASH%"
  }
  style-src = "%CSP_MYSCRIPT_HASH%"
}
```

Hashes can be calculated through an [online hash calculator](https://report-uri.com/home/hash), or generated internally using a utility class:

Java
: @[csp-hash-generator](code/javaguide/detailed/filters/csp/CSPHashGenerator.java)

Scala
: @[csp-hash-generator](code/scalaguide/detailed/filters/csp/CSPHashGenerator.scala)

### Configuring CSP Nonces

A CSP nonce is a "one time only" value (n=once) that is generated on every request and can be inserted into the body of inline content to whitelist content.

Play defines a nonce through [`play.filters.csp.DefaultCSPProcessor`](api/scala/play/filters/csp/DefaultCSPProcessor.html) if `play.filters.csp.nonce.enabled` is true.  If a request has the attribute [`play.api.mvc.request.RequestAttrKey.CSPNonce`](api/scala/play/api/mvc/request/RequestAttrKey$.html), then that nonce is used.  Otherwise, a nonce is generated from 16 bytes of [`java.security.SecureRandom`](https://docs.oracle.com/javase/8/docs/api/java/security/SecureRandom.html).

@[csp-nonce](/confs/filters-helpers/reference.conf)

Accessing the CSP nonce from a Twirl template is shown in [[Using CSP in Page Templates|CspFilter#Using-CSP-in-Page-Templates]].

### Configuring CSP Directives

CSP directives are configured through the `play.filters.csp.directives` section in `application.conf`.

#### Defining CSP Directives

Directives are configured one to one, with the configuration key matching the CSP directive name, i.e. for a CSP directive `default-src` with a value of  `'none'`, you would set the following:

```hocon
play.filters.csp.directives.default-src = "'none'"
```

Where no value is specified then `""` should be used, i.e. [`upgrade-insecure-requests`](https://www.w3.org/TR/upgrade-insecure-requests/#delivery) would be defined as follows:

```hocon
play.filters.csp.directives.upgrade-insecure-requests = ""
```

CSP directives are mostly defined in the [CSP3 Spec](https://www.w3.org/TR/CSP3/) except for the following exceptions:

* `require-sri-for` is described in [subresource-integrity](https://w3c.github.io/webappsec-subresource-integrity/#opt-in-require-sri-for)
* `upgrade-insecure-requests` in [Upgrade Insecure Requests W3C CR](https://www.w3.org/TR/upgrade-insecure-requests/#delivery)
* `block-all-mixed-content` in [Mixed Content W3C CR](https://www.w3.org/TR/mixed-content/#block-all-mixed-content)

The [CSP cheat sheet](https://scotthelme.co.uk/csp-cheat-sheet/) is a good reference for looking up CSP directives.

#### Default CSP Policy

The default policy defined in `CSPFilter` is based off Google's [Strict CSP Policy](https://csp.withgoogle.com/docs/strict-csp.html):

@[csp-directives](/confs/filters-helpers/reference.conf)

> **Note:** Google's Strict CSP policy is a good place to start, but it does not completely define a content security policy.  Please consult with a security team to determine the right policy for your site.  

## Using CSP in Page Templates

The CSP nonce is accessible from page templates using the [`views.html.helper.CSPNonce`](api/scala/views/html/helper/CSPNonce$.html) helper class.  This helper has a number of methods that render the nonce in different ways.

### CSPNonce Helper

* [`CSPNonce.apply`](api/scala/views/html/helper/CSPNonce$.html#apply\(\)\(implicitrequest:play.api.mvc.RequestHeader\):String) returns the nonce as a String or throws exception.
* [`CSPNonce.attr`](api/scala/views/html/helper/CSPNonce$.html#attr\(implicitrequest:play.api.mvc.RequestHeader\):play.twirl.api.Html) returns `nonce="$nonce"` as a Twirl `Html` or `Html.empty`
* [`CSPNonce.attrMap`](api/scala/views/html/helper/CSPNonce$.html#attrMap\(implicitrequest:play.api.mvc.RequestHeader\):play.twirl.api.Html) returns `Map("nonce" -> nonce)` or `Map.empty`
* [`CSPNonce.get`](api/scala/views/html/helper/CSPNonce$.html#get\(implicitrequest:play.api.mvc.RequestHeader\):Option[String]) returns `Some(nonce)` or `None`

> **Note:** You must have an implicit [`RequestHeader`](api/scala/play/api/mvc/RequestHeader.html) in scope for all the above methods, i.e. `@()(implicit request: RequestHeader)`

#### Adding CSPNonce to HTML

Adding the CSP nonce into page templates is most easily done by adding `@{CSPNonce.attr}` into an HTML element.

For example, to add a CSP nonce to a `link` element, you would do the following:

```twirl
@()(implicit request: RequestHeader)

<link rel="stylesheet" @{CSPNonce.attr}  media="screen" href="@routes.Assets.at("stylesheets/main.css")">
```

Using `CSPNonce.attrMap` is appropriate in cases where existing helpers take a map of attributes.  For example, the WebJars project will take attributes:

```twirl
@()(implicit request: RequestHeader, webJarsUtil: org.webjars.play.WebJarsUtil)

@webJarsUtil.locate("bootstrap.min.css").css(CSPNonce.attrMap)
@webJarsUtil.locate("bootstrap-theme.min.css").css(CSPNonce.attrMap)

@webJarsUtil.locate("jquery.min.js").script(CSPNonce.attrMap)
```

#### CSPNonce aware helpers

For ease of use, there are [`style`](api/scala/views/html/helper/style$.html), and [`script`](api/scala/views/html/helper/script$.html) helpers that will wrap existing inline blocks.  These are useful for adding simple bits of inline Javascript and CSS.

Because these helpers are generated from Twirl templates, Scaladoc does not provide the correct source reference for these helpers. The source code for these helpers can be seen on [Github](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/views/helper/) for a more complete view.

##### Style Helper

The `style` helper is a wrapper for the following:

```html
<style @{CSPNonce.attr} @toHtmlArgs(args.toMap)>@body</style>
```

And is used in pages like this:

```twirl
@()(implicit request: RequestHeader)

@views.html.helper.style('type -> "text/css") {
    html, body, pre {
        margin: 0;
        padding: 0;
        font-family: Monaco, 'Lucida Console', monospace;
        background: #ECECEC;
    }
}
```

##### Script Helper

The `script` helper is a wrapper for the script element:

```html
<script @{CSPNonce.attr} @toHtmlArgs(args.toMap)>@body</script>
```

and is used as follows:

```twirl
@()(implicit request: RequestHeader)

@views.html.helper.script(args = 'type -> "text/javascript") {
  alert("hello world");
}
```

## Enabling CSP Dynamically

In the examples given above, CSP is processed from configuration, and is done so statically.  If you need to change your CSP policy at runtime, or have several different policies, then it may make more sense to create and add a CSP header dynamically rather than use an action or a filter, and combine that with CSP's configured filter.

### Using CSPProcessor

Say that you have a number of assets, and you want to add CSP hashes to your header dynamically.  Here's how you would inject a dynamic list of CSP hashes using a custom action builder:

#### Scala

@[scala-csp-dynamic-action](code/scalaguide/detailed/filters/csp/DynamicCSPAction.scala)

#### Java

The same principal applies in Java, only extending the `AbstractCSPAction`:

@[java-csp-dynamic-action](code/javaguide/detailed/filters/csp/MyDynamicCSPAction.java)

@[java-asset-cache](code/javaguide/detailed/filters/csp/AssetCache.java)

@[java-csp-module](code/javaguide/detailed/filters/csp/CustomCSPActionModule.java)

And then call `@With(MyDynamicCSPAction.class)` on your action.

## CSP Gotchas

CSP is a powerful tool, but it also combines a number of disparate directives that do not always work together smoothly.

### Unintuitive Directives

Some directives are not covered by `default-src`, for example `form-action` is defined seperately.  An attack on a website omitting `form-action` was detailed in [I’m harvesting credit card numbers and passwords from your site. Here’s how](https://hackernoon.com/im-harvesting-credit-card-numbers-and-passwords-from-your-site-here-s-how-9a8cb347c5b5).

In particular, there are a number of subtle interactions with CSP that are unintuitive.  For example, if you are using websockets, you should enable the `connect-src` with the exact URL (i.e. `ws://localhost:9000 wss://localhost:9443`) as [declaring a CSP with connect-src ‘self’ will not allow websockets back to the same host/port, since they're not same origin](https://github.com/w3c/webappsec-csp/issues/7).  If you do not set `connect-src`, then you should check the `Origin` header to protect against [Cross-Site WebSocket Hijacking](http://www.christian-schneider.net/CrossSiteWebSocketHijacking.html).

### False CSP Reports

There can be a number of false positives produced from [browser extensions and plugins](https://jellyhive.com/activity/posts/2018/03/26/csp-implementations-are-broken/), and these can show as coming from `about:blank`.  It can take an extended period of time to resolve real issues and work out filters.  If you would rather configure a report-only policy externally, [Report URI](https://report-uri.com) is a hosted CSP service that will collect CSP reports and provide filters.

## Further Reading

Adopting a good CSP policy is a multi-stage process.  Google's [Adopting strict CSP](https://csp.withgoogle.com/docs/adopting-csp.html) guide is recommended, but is only a starting point, and there are some non-trivial aspects to CSP implementation.

Github's discussion on [implementing CSP](https://githubengineering.com/githubs-csp-journey/) and adding [additional protections](https://githubengineering.com/githubs-post-csp-journey/) is worth reading.

Dropbox has posts on [CSP reporting and filtering](https://blogs.dropbox.com/tech/2015/09/on-csp-reporting-and-filtering/) and [inline content and nonce deployment](https://blogs.dropbox.com/tech/2015/09/unsafe-inline-and-nonce-deployment/), and went through extended periods of reporting CSP before moving to an enforced CSP policy.

Square also wrote up [Content Security Policy for Single Page Web Apps](https://medium.com/square-corner-blog/content-security-policy-for-single-page-web-apps-78f2b2cf1757).
