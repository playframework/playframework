# OpenID Support in Play

OpenID is a protocol for users to access several services with a single account. As a web developer, you can use OpenID to offer users a way to login with an account they already have (their [Google account](https://developers.google.com/accounts/docs/OpenID) for example). In the enterprise, you can use OpenID to connect to a company's SSO server if it supports it.

                                                                                                      ## The OpenID flow in a nutshell

                                                                                                      1. The user gives you his OpenID (a URL)
                                                                                                      2. Your server inspect the content behind the URL to produce a URL where you need to redirect the user
3. The user validates the authorization on his OpenID provider, and gets redirected back to your server
4. Your server receives information from that redirect, and check with the provider that the information is correct

The step 1. may be omitted if all your users are using the same OpenID provider (for example if you decide to rely completely on Google accounts).

## OpenID in Play Framework

The OpenID API has two important functions:

* `OpenID.redirectURL` calculates the URL where you should redirect the user. It involves fetching the user's OpenID page, this is why it returns a `Promise<String>` rather than a `String`. If the OpenID is invalid, an exception will be thrown.
* `OpenID.verifiedId` inspects the current request to establish the user information, including his verified OpenID. It will do a call to the OpenID server to check the authenticity of the information, this is why it returns a `Promise<UserInfo>` rather than just `UserInfo`. If the information is not correct or if the server check is false (for example if the redirect URL has been forged), the returned `Promise` will be a `Thrown`.

In any case, you should catch exceptions and if one is thrown redirect back the user to the login page with relevant information.

## Extended Attributes

The OpenID of a user gives you his identity. The protocol also support getting [extended attributes](http://openid.net/specs/openid-attribute-exchange-1_0.html) such as the email address, the first name, the last name...

You may request from the OpenID server *optional* attributes and/or *required* attributes. Asking for required attributes means the user can not login to your service if he doesn't provides them.

Extended attributes are requested in the redirect URL:

```
Map<String, String> attributes = new HashMap<String, String>();
attributes.put("email", "http://schema.openid.net/contact/email");
OpenID.redirectURL(
  openid, 
  routes.Application.openIDCallback.absoluteURL(request()), 
  attributes
);
```

Attributes will then be available in the `UserInfo` provided by the OpenID server.
