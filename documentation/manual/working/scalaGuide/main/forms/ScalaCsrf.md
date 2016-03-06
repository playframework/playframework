<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Protecting against Cross Site Request Forgery

Cross Site Request Forgery (CSRF) is a security exploit where an attacker tricks a victims browser into making a request using the victims session.  Since the session token is sent with every request, if an attacker can coerce the victims browser to make a request on their behalf, the attacker can make requests on the users behalf.

It is recommended that you familiarise yourself with CSRF, what the attack vectors are, and what the attack vectors are not.  We recommend starting with [this information from OWASP](https://www.owasp.org/index.php/Cross-Site_Request_Forgery_%28CSRF%29).

There is no simple answer to what requests are safe and what are vulnerable to CSRF requests, the reason for this is that there is no clear specification as to what is allowable from plugins and future extensions to specifications.  Historically, browser plugins and extensions have relaxed the rules that frameworks previously thought could be trusted, introducing CSRF vulnerabilities to many applications, and the onus has been on the frameworks to fix them.  For this reason, Play takes a conservative approach in its defaults, but allows you to configure exactly when a check is done.  By default, Play will require a CSRF check when all of the following are true:

* The request method is not `GET`, `HEAD` or `OPTIONS`.
* The request has one or more `Cookie` or `Authorization` headers.
* The CORS filter is not configured to trust the request's origin.

> **Note:** If you use browser-based authentication other than using cookies or HTTP authentication, such as NTLM or client certificate based authentication, then you **must** set `play.filters.csrf.header.protectHeaders = null`, or include the headers used in authentication in `protectHeaders`.

### Play's CSRF protection

Play supports multiple methods for verifying that a request is not a CSRF request.  The primary mechanism is a CSRF token.  This token gets placed either in the query string or body of every form submitted, and also gets placed in the users session.  Play then verifies that both tokens are present and match.

To allow simple protection for non browser requests, Play only checks requests with cookies in the header.  If you are making requests with AJAX, you can place the CSRF token in the HTML page, and then add it to the request using the `Csrf-Token` header.

Alternatively, you can set `play.filters.csrf.header.bypassHeaders` to match common headers: A common configuration would be:

* If an `X-Requested-With` header is present, Play will consider the request safe.  `X-Requested-With` is added to requests by many popular Javascript libraries, such as jQuery.
* If a `Csrf-Token` header with value `nocheck` is present, or with a valid CSRF token, Play will consider the request safe.

This configuration would look like:

```
play.filters.csrf.headers.bypassHeaders {
  X-Requested-With = "*"
  Csrf-Token = "nocheck"
}
```

Caution should be taken when using this configuration option, as historically browser plugins have undermined this type of CSRF defence.

### Trusting CORS requests

By default, if you have a CORS filter before your CSRF filter, the CSRF filter will let through CORS requests from trusted origins. To disable this check, set the config option `play.filters.csrf.bypassCorsTrustedOrigins = false`.

## Applying a global CSRF filter

Play provides a global CSRF filter that can be applied to all requests.  This is the simplest way to add CSRF protection to an application.  To enable the global filter, add the Play filters helpers dependency to your project in `build.sbt`:

```scala
libraryDependencies += filters
```

Now add them to your `Filters` class as described in [[HTTP filters|ScalaHttpFilters]]:

@[http-filters](code/ScalaCsrf.scala)

The `Filters` class can either be in the root package, or if it has another name or is in another package, needs to be configured using `play.http.filters` in `application.conf`:

```
play.http.filters = "filters.MyFilters"
```

### Getting the current token

The current CSRF token can be accessed using the `CSRF.getToken` method.  It takes an implicit `RequestHeader`, so ensure that one is in scope.

@[get-token](code/ScalaCsrf.scala)

If you are not using the CSRF filter, you also should inject the `CSRFAddToken` and `CSRFCheck` action wrappers to force adding a token or a CSRF check on a specific action. Otherwise the token will not be available.

@[csrf-controller](code/ScalaCsrf.scala)

To help in adding CSRF tokens to forms, Play provides some template helpers.  The first one adds it to the query string of the action URL:

@[csrf-call](code/scalaguide/forms/csrf.scala.html)

This might render a form that looks like this:

```html
<form method="POST" action="/items?csrfToken=1234567890abcdef">
   ...
</form>
```

If it is undesirable to have the token in the query string, Play also provides a helper for adding the CSRF token as hidden field in the form:

@[csrf-input](code/scalaguide/forms/csrf.scala.html)

This might render a form that looks like this:

```html
<form method="POST" action="/items">
   <input type="hidden" name="csrfToken" value="1234567890abcdef"/>
   ...
</form>
```

The form helper methods all require an implicit `RequestHeader` to be available in scope. This will typically be provided by adding an implicit `RequestHeader` parameter to your template, if it doesn't have one already.

### Adding a CSRF token to the session

To ensure that a CSRF token is available to be rendered in forms, and sent back to the client, the global filter will generate a new token for all GET requests that accept HTML, if a token isn't already available in the incoming request.

## Applying CSRF filtering on a per action basis

Sometimes global CSRF filtering may not be appropriate, for example in situations where an application might want to allow some cross origin form posts.  Some non session based standards, such as OpenID 2.0, require the use of cross site form posting, or use form submission in server to server RPC communications.

In these cases, Play provides two actions that can be composed with your applications actions.

The first action is the `CSRFCheck` action, and it performs the check.  It should be added to all actions that accept session authenticated POST form submissions:

@[csrf-check](code/ScalaCsrf.scala)

The second action is the `CSRFAddToken` action, it generates a CSRF token if not already present on the incoming request.  It should be added to all actions that render forms:

@[csrf-add-token](code/ScalaCsrf.scala)

A more convenient way to apply these actions is to use them in combination with Play's [[action composition|ScalaActionsComposition]]:

@[csrf-action-builder](code/ScalaCsrf.scala)

Then you can minimise the boiler plate code necessary to write actions:

@[csrf-actions](code/ScalaCsrf.scala)

## CSRF configuration options

The full range of CSRF configuration options can be found in the filters [reference.conf](resources/confs/filters-helpers/reference.conf).  Some examples include:

* `play.filters.csrf.token.name` - The name of the token to use both in the session and in the request body/query string. Defaults to `csrfToken`.
* `play.filters.csrf.cookie.name` - If configured, Play will store the CSRF token in a cookie with the given name, instead of in the session.
* `play.filters.csrf.cookie.secure` - If `play.filters.csrf.cookie.name` is set, whether the CSRF cookie should have the secure flag set.  Defaults to the same value as `play.http.session.secure`.
* `play.filters.csrf.body.bufferSize` - In order to read tokens out of the body, Play must first buffer the body and potentially parse it.  This sets the maximum buffer size that will be used to buffer the body.  Defaults to 100k.
* `play.filters.csrf.token.sign` - Whether Play should use signed CSRF tokens.  Signed CSRF tokens ensure that the token value is randomised per request, thus defeating BREACH style attacks.
