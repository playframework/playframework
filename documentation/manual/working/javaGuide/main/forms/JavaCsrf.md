<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Protecting against Cross Site Request Forgery

Cross Site Request Forgery (CSRF) is a security exploit where an attacker tricks a victim's browser into making a request using the victim's session.  Since the session token is sent with every request, if an attacker can coerce the victim's browser to make a request on their behalf, the attacker can make requests on the user's behalf.

It is recommended that you familiarize yourself with CSRF, what the attack vectors are, and what the attack vectors are not.  We recommend starting with [this information from OWASP](https://www.owasp.org/index.php/Cross-Site_Request_Forgery_%28CSRF%29).

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
play.filters.csrf.header.bypassHeaders {
  X-Requested-With = "*"
  Csrf-Token = "nocheck"
}
```

Caution should be taken when using this configuration option, as historically browser plugins have undermined this type of CSRF defence.

### Trusting CORS requests

By default, if you have a CORS filter before your CSRF filter, the CSRF filter will let through CORS requests from trusted origins. To disable this check, set the config option `play.filters.csrf.bypassCorsTrustedOrigins = false`.

## Applying a global CSRF filter

> **Note:** As of Play 2.6.x, the CSRF filter is included in Play's list of default filters that are applied automatically to projects.  See [[the Filters page|Filters]] for more information.

Play provides a global CSRF filter that can be applied to all requests.  This is the simplest way to add CSRF protection to an application.  To add the filter manually, add it to `application.conf`:

```
play.filters.enabled += "play.filters.csrf.CSRFFilter"
```

It is also possible to disable the CSRF filter for a specific route in the routes file. To do this, add the `nocsrf` modifier tag before your route:

@[nocsrf](../http/code/javaguide.http.routing.routes)

### Getting the current token

The current CSRF token can be accessed using the `CSRF.getToken` method.  It takes a [`RequestHeader`](api/java/play/mvc/Http.RequestHeader.html), which can be obtained from [`Http.Context.current()`](api/java/play/mvc/Http.Context.html#current--) with [`context.request()`](api/java/play/mvc/Http.Context.html#request--):

@[get-token](code/javaguide/forms/JavaCsrf.java)

> **Note**: If the CSRF filter is installed, Play will try to avoid generating the token as long as the cookie being used is HttpOnly (meaning it cannot be accessed from JavaScript). When sending a response with a strict body, Play skips adding the token to the response unless `CSRF.getToken` has already been called. This results in a significant performance improvement for responses that don't need a CSRF token. If the cookie is not configured to be HttpOnly, Play will assume you wish to access it from JavaScript and generate it regardless.

> **Note**: if you are accessing the template from a `CompletionStage` and get an `There is no HTTP Context` error, then you will need to add  [`HttpExecutionContext.current()`](api/java/play/libs/concurrent/HttpExecutionContext.html) -- see [[JavaAsync]] for details.

To help in adding CSRF tokens to forms, Play provides some template helpers.  The first one adds it to the query string of the action URL:

@[csrf-call](code/javaguide/forms/csrf.scala.html)

This might render a form that looks like this:

```html
<form method="POST" action="/items?csrfToken=1234567890abcdef">
   ...
</form>
```

If it is undesirable to have the token in the query string, Play also provides a helper for adding the CSRF token as hidden field in the form:

@[csrf-input](code/javaguide/forms/csrf.scala.html)

This might render a form that looks like this:

```html
<form method="POST" action="/items">
   <input type="hidden" name="csrfToken" value="1234567890abcdef"/>
   ...
</form>
```

### Adding a CSRF token to the session

To ensure that a CSRF token is available to be rendered in forms, and sent back to the client, the global filter will generate a new token for all GET requests that accept HTML, if a token isn't already available in the incoming request.

## Applying CSRF filtering on a per action basis

Sometimes global CSRF filtering may not be appropriate, for example in situations where an application might want to allow some cross origin form posts.  Some non session based standards, such as OpenID 2.0, require the use of cross site form posting, or use form submission in server to server RPC communications.

In these cases, Play provides two actions that can be composed with your applications actions.

The first action is the [`play.filters.csrf.RequireCSRFCheck`](api/java/play/filters/csrf/RequireCSRFCheck.html) action which performs the CSRF check. It should be added to all actions that accept session authenticated POST form submissions:

@[csrf-check](code/javaguide/forms/JavaCsrf.java)

The second action is the [`play.filters.csrf.AddCSRFToken`](api/java/play/filters/csrf/AddCSRFToken.html) action, it generates a CSRF token if not already present on the incoming request. It should be added to all actions that render forms:

@[csrf-add-token](code/javaguide/forms/JavaCsrf.java)

## CSRF configuration options

The full range of CSRF configuration options can be found in the filters [reference.conf](resources/confs/filters-helpers/reference.conf).  Some examples include:

* `play.filters.csrf.token.name` - The name of the token to use both in the session and in the request body/query string. Defaults to `csrfToken`.
* `play.filters.csrf.cookie.name` - If configured, Play will store the CSRF token in a cookie with the given name, instead of in the session.
* `play.filters.csrf.cookie.secure` - If `play.filters.csrf.cookie.name` is set, whether the CSRF cookie should have the secure flag set.  Defaults to the same value as `play.http.session.secure`.
* `play.filters.csrf.body.bufferSize` - In order to read tokens out of the body, Play must first buffer the body and potentially parse it.  This sets the maximum buffer size that will be used to buffer the body.  Defaults to 100k.
* `play.filters.csrf.token.sign` - Whether Play should use signed CSRF tokens.  Signed CSRF tokens ensure that the token value is randomised per request, thus defeating BREACH style attacks.

## Testing CSRF


In a functional test, if you are rendering a Twirl template with a CSRF token, you need to have a CSRF token available.  You can do this by calling `play.api.test.CSRFTokenHelper.addCSRFToken` on a `play.mvc.Http.RequestBuilder` instance:

@[test-with-addCSRFToken](../../../commonGuide/filters/code/javaguide/detailed/filters/FiltersTest.java)

