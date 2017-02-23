<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Hostname Verification

Hostname verification is a little known part of HTTPS that involves a [server identity check](https://tools.ietf.org/search/rfc2818#section-3.1) to ensure that the client is talking to the correct server and has not been redirected by a man in the middle attack.

The check involves looking at the certificate sent by the server, and verifying that the `dnsName` in the `subjectAltName` field of the certificate matches the host portion of the URL used to make the request.

WS SSL does hostname verification automatically.

## Debugging

Hostname Verification can be tested using `dnschef`.  A complete guide is out of scope of documentation, but an example can be found in [Testing Hostname Verification](https://tersesystems.com/2014/03/31/testing-hostname-verification/).
