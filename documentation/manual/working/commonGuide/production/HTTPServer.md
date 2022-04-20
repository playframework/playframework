<!--- Copyright (C) Lightbend Inc. <https://www.lightbend.com> -->

# Setting up a front end HTTP server

You can easily deploy your application as a stand-alone server by setting the application HTTP port to 80:

```bash
$ /path/to/bin/<project-name> -Dhttp.port=80
```

> **Note**: you probably need root permissions to bind a process on this port.

However, if you plan to host several applications in the same server or load balance several instances of your application for scalability or fault tolerance, you can use a front end HTTP server.

Note that using a front end HTTP server will rarely give you better performance than using Play server directly.  However, HTTP servers are very good at handling HTTPS, conditional GET requests and static assets, and many services assume a front end HTTP server is part of your architecture.

## Set up with lighttpd

This example shows you how to configure [lighttpd](http://www.lighttpd.net/) as a front end web server. Note that you can do the same with Apache, but if you only need virtual hosting or load balancing, lighttpd is a very good choice and much easier to configure.

The `/etc/lighttpd/lighttpd.conf` file should define configuration like this:

```
server.modules = (
      "mod_access",
      "mod_proxy",
      "mod_accesslog"
)

$HTTP["host"] =~ "www.myapp.com" {
    proxy.balance = "round-robin" proxy.server = ( "/" =>
        ( ( "host" => "127.0.0.1", "port" => 9000 ) ) )
}

$HTTP["host"] =~ "www.loadbalancedapp.com" {
    proxy.balance = "round-robin" proxy.server = ( "/" => (
          ( "host" => "127.0.0.1", "port" => 9001 ),
          ( "host" => "127.0.0.1", "port" => 9002 ) )
    )
}
```

See [lighttpd's documentation](https://redmine.lighttpd.net/projects/lighttpd/wiki/Docs_ModProxy) for more details about how to configure `mod_proxy`.

## Set up with nginx

This example shows you how to configure [nginx](https://www.nginx.com/resources/wiki/start/) as a front end web server. Note that you can do the same with Apache, but if you only need virtual hosting or load balancing, nginx is a very good choice and much easier to configure.

> **Note**: nginx has extensive documentation about how to configure it as a load balancer. See the [HTTP Load Balance Guide](https://docs.nginx.com/nginx/admin-guide/load-balancer/http-load-balancer/) for detailed information.

The `/etc/nginx/nginx.conf` file should define `upstream` and `server` block like this:

```
upstream playapp {
  server 127.0.0.1:9000;
}

server {
  listen 80;
  server_name www.domain.com;
  location / {
    proxy_pass http://playapp;
  }
}
```

For more details, see a [full example configuration](https://www.nginx.com/resources/wiki/start/topics/examples/full/), and if you want to use nginx to do SSL termination, see the [documentation here](https://docs.nginx.com/nginx/admin-guide/security-controls/terminating-ssl-http/).

> **Note**: make sure you are using version 1.2 or greater of Nginx otherwise chunked responses won't work properly.

## Set up with Apache

The example below shows a simple set up with [Apache httpd server](https://httpd.apache.org/) running in front of a standard Play configuration.

```
LoadModule proxy_module modules/mod_proxy.so
…
<VirtualHost *:80>
  ProxyPreserveHost On
  ServerName www.loadbalancedapp.com
  ProxyPass  /excluded !
  ProxyPass / http://127.0.0.1:9000/
  ProxyPassReverse / http://127.0.0.1:9000/
</VirtualHost>
```

## Advanced proxy settings

When using an HTTP frontal server, request addresses are seen as coming from the HTTP server. In a usual set-up, where you both have the Play app and the proxy running on the same machine, the Play app will see the requests coming from `127.0.0.1`.

Proxy servers can add a specific header to the request to tell the proxied application where the request came from. Most web servers will add an `X-Forwarded-For` header with the remote client IP address as first argument. If the proxy server is running on `localhost` and connecting from `127.0.0.1`, Play will trust its `X-Forwarded-For` header.

However, the host header is untouched, it’ll remain issued by the proxy. If you use Apache 2.x, you can add a directive like:

```
ProxyPreserveHost on
```

The `Host` header will be the original host request header issued by the client. By combining theses two techniques, your app will appear to be directly exposed.

If you don't want this play app to occupy the whole root, add an exclusion directive to the proxy config:

```
ProxyPass /excluded !
```

## Apache as a front proxy to allow transparent upgrade of your application

The basic idea is to run two Play instances of your web application and let the front-end proxy load-balance them. In case one is not available, it will forward all the requests to the available one.

Let’s start the same Play application two times: one on port `9999` and one on port `9998`.

```bash
start -Dhttp.port=9998
start -Dhttp.port=9999
```

Now, let’s configure our Apache web server to have a load balancer. In Apache, add the following configuration:

```
<VirtualHost mysuperwebapp.com:80>
  ServerName mysuperwebapp.com
  <Location /balancer-manager>
    SetHandler balancer-manager
    Order Deny,Allow
    Deny from all
    Allow from .mysuperwebapp.com
  </Location>
  <Proxy balancer://mycluster>
    BalancerMember http://localhost:9999
    BalancerMember http://localhost:9998 status=+H
  </Proxy>
  <Proxy *>
    Order Allow,Deny
    Allow From All
  </Proxy>
  ProxyPreserveHost On
  ProxyPass /balancer-manager !
  ProxyPass / balancer://mycluster/
  ProxyPassReverse / balancer://mycluster/
</VirtualHost>
```

The important part is `balancer://mycluster`. This declares a load balancer. The `+H` option means that the second Play application is on standby. But you can also instruct it to load balance.

Apache also provides a way to view the status of your cluster. Simply point your browser to `/balancer-manager` to view the current status of your clusters.

Because Play is completely stateless you don’t have to manage sessions between the 2 clusters. You can actually easily scale to more than 2 Play instances.

To use WebSockets, you must use [mod_proxy_wstunnel](http://httpd.apache.org/docs/2.4/mod/mod_proxy_wstunnel.html), which was introduced in Apache 2.4.

Note that [ProxyPassReverse might rewrite incorrectly headers](https://bz.apache.org/bugzilla/show_bug.cgi?id=51982) adding an extra / to the URIs, so you may wish to use this workaround:

```
ProxyPassReverse / http://localhost:9999
ProxyPassReverse / http://localhost:9998
```

## Configuring trusted proxies

Play supports various forwarded headers used by proxies to indicate the incoming IP address and protocol of requests. Play uses this configuration to calculate the correct value for the `remoteAddress` and `secure` fields of `RequestHeader`.

It is trivial for an HTTP client, whether it's a browser or other client, to forge forwarded headers, thereby spoofing the IP address and protocol that Play reports, consequently, Play needs to know which proxies are trusted. Play provides a configuration option to configure a list of trusted proxies, and will validate the incoming forwarded headers to verify that they are trusted, taking the first untrusted IP address that it finds as the reported user remote address (or the last IP address if all proxies are trusted.)

To configure the list of trusted proxies, you can configure `play.http.forwarded.trustedProxies`.  This takes a list of IP address or CIDR subnet ranges.  Both IPv4 and IPv6 are supported.  For example:

```
play.http.forwarded.trustedProxies=["192.168.0.0/24", "::1", "127.0.0.1"]
```

This says all IP addresses that start with `192.168.0`, as well as the IPv6 and IPv4 loopback addresses, are trusted.  By default, Play will just trust the loopback address, that is `::1` and `127.0.0.1`.

### Trusting all proxies

Many cloud providers, most notably AWS, provide no guarantees for which IP addresses their load balancer proxies will use.  Consequently, the only way to support forwarded headers with these services is to trust all IP addresses.  This can be done by configuring the trusted proxies like so:

```
play.http.forwarded.trustedProxies=["0.0.0.0/0", "::/0"]
```

### Forwarded header version

Play supports two different versions of forwarded headers:

* the legacy method with X-Forwarded headers
* the [RFC 7239](https://tools.ietf.org/html/rfc7239) with Forwarded headers

This is configured using `play.http.forwarded.version`, with valid values being `x-forwarded` or `rfc7239`. The default is `x-forwarded`.

`x-forwarded` uses the de facto standard `X-Forwarded-For` and `X-Forwarded-Proto` headers to determine the correct remote address and protocol for the request. These headers are widely used, however, they have some serious limitations, for example, if you have multiple proxies, and only one of them adds the `X-Forwarded-Proto` header, it's impossible to reliably determine which proxy added it and therefore whether the request from the client was made using https or http. `rfc7239` uses the new `Forwarded` header standard, and solves many of the limitations of the `X-Forwarded-*` headers.

For more information, please read the [RFC 7239](https://tools.ietf.org/html/rfc7239) specification.
