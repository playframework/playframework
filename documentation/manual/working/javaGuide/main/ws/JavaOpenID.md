<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# OpenID Support in Play

> **Warning**: Play's `openId` module implements the obsolete OpenID Authentication 2.0 protocol, with compatibility paths for OpenID 1.x providers. It does not implement OpenID Connect (OIDC). New authentication integrations should use an OIDC client library.
>
> Both initial discovery and callback verification issue outbound requests to locations derived from untrusted input. In particular, `OpenIdClient.verifiedId` performs discovery on the callback's `openid.claimed_id` and then contacts the discovered provider endpoint. This creates a server-side request forgery (SSRF) risk.
>
> Applications that retain this module must validate identifiers from both initiation and callback requests, restrict permitted provider endpoints, and enforce network-level egress controls that prevent connections to internal, loopback, link-local, and cloud metadata addresses, including after DNS resolution and redirects.

OpenID 2.0 is a protocol for users to access several services with a single account. In a legacy deployment, you can use it to let users log in with an existing OpenID identity or to connect to a company’s compatible SSO server.

## The OpenID flow in a nutshell

1. The user gives you his OpenID (a URL).
2. Your server inspects the content behind the URL to produce a URL where you need to redirect the user.
3. The user confirms the authorization on his OpenID provider, and gets redirected back to your server.
4. Your server receives information from that redirect, and checks with the provider that the information is correct.

Step 1 may be omitted if all your users are using the same trusted OpenID provider.

## Usage

To use OpenID, first add `openId` to your `build.sbt` file:

@[javaopenid-sbt-dependencies](code/javaopenid.sbt)

Now any controller or component that wants to use OpenID will have to declare a dependency on the [OpenIdClient](api/java/play/libs/openid/OpenIdClient.html).

## OpenID in Play

The OpenID API has two important functions:

* `OpenIdClient.redirectURL` calculates the URL where you should redirect the user. It involves fetching the user's OpenID page asynchronously, this is why it returns a `CompletionStage<String>`. If the OpenID is invalid, the returned `CompletionStage` will be completed with an exception.
* `OpenIdClient.verifiedId` inspects the current request to establish the user information, including his verified OpenID. It will do a call to the OpenID server asynchronously to check the authenticity of the information, returning a promise of [UserInfo](api/java/play/libs/openid/UserInfo.html). If the information is not correct or if the server check is false (for example if the redirect URL has been forged), the returned `CompletionStage` will be completed with an exception.
If the `CompletionStage` fails, you can define a fallback, which redirects back the user to the login page or return a `BadRequest`.

### Example

`conf/routes`:

@[ws-openid-routes](code/javaguide.ws.routes)

Controller:

@[ws-openid-controller](code/javaguide/ws/controllers/OpenIDController.java)


## Extended Attributes

The OpenID of a user gives you his identity. The protocol also supports getting [extended attributes](https://openid.net/specs/openid-attribute-exchange-1_0.html) such as the e-mail address, the first name, or the last name.

You may request *optional* attributes and/or *required* attributes from the OpenID server. Asking for required attributes means the user cannot login to your service if he doesn't provide them.

Extended attributes are requested in the redirect URL:

@[ws-openid-extended-attributes](code/javaguide/ws/controllers/OpenIDController.java)

Attributes will then be available in the `UserInfo` provided by the OpenID server.
