<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Setting up a front end HTTP server

You can easily deploy your application as a stand-alone server by setting the application HTTP port to 80:

```
$ /path/to/bin/<project-name> -Dhttp.port=80
```

> Note that you probably need root permissions to bind a process on this port.

However, if you plan to host several applications in the same server or load balance several instances of your application for scalability or fault tolerance, you can use a front end HTTP server.

Note that using a front end HTTP server will rarely give you better performance than using Play server directly.  However, HTTP servers are very good at handling HTTPS, conditional GET requests and static assets, and many services assume a front end HTTP server is part of your architecture.

## Set up with lighttpd

This example shows you how to configure [lighttpd](http://www.lighttpd.net/) as a front end web server. Note that you can do the same with Apache, but if you only need virtual hosting or load balancing, lighttpd is a very good choice and much easier to configure!

The `/etc/lighttpd/lighttpd.conf` file should define things like this:

```
server.modules = (
      "mod_access",
      "mod_proxy",
      "mod_accesslog"
)
…
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

## Set up with nginx

This example shows you how to configure [nginx](http://wiki.nginx.org/Main) as a front end web server. Note that you can do the same with Apache, but if you only need virtual hosting or load balancing, nginx is a very good choice and much easier to configure!

The `/etc/nginx/nginx.conf` file should define things like this:

```
worker_processes  1;

events {
    worker_connections  1024;
}

http {
  include       mime.types;
  default_type  application/octet-stream;

  sendfile        on;
  keepalive_timeout  65;

  proxy_buffering    off;
  proxy_set_header   X-Real-IP $remote_addr;
  proxy_set_header   X-Scheme $scheme;
  proxy_set_header   X-Forwarded-For $proxy_add_x_forwarded_for;
  proxy_set_header   Host $http_host;
  proxy_http_version 1.1;

  upstream my-backend {
     server 127.0.0.1:9000;
  }

  server {
    listen       80;
    server_name www.mysite.com;
    location / {
       proxy_pass http://my-backend;
    }
  }

  #server {
  #  listen               443;
  #  ssl                  on;
  #
  #  # http://www.selfsignedcertificate.com/ is useful for development testing
  #  ssl_certificate      /etc/ssl/certs/my_ssl.crt;
  #  ssl_certificate_key  /etc/ssl/private/my_ssl.key;
  #
  #  # From https://bettercrypto.org/static/applied-crypto-hardening.pdf
  #  ssl_prefer_server_ciphers on;
  #  ssl_protocols TLSv1 TLSv1.1 TLSv1.2; # not possible to do exclusive
  #  ssl_ciphers 'EDH+CAMELLIA:EDH+aRSA:EECDH+aRSA+AESGCM:EECDH+aRSA+SHA384:EECDH+aRSA+SHA256:EECDH:+CAMELLIA256:+AES256:+CAMELLIA128:+AES128:+SSLv3:!aNULL:!eNULL:!LOW:!3DES:!MD5:!EXP:!PSK:!DSS:!RC4:!SEED:!ECDSA:CAMELLIA256-SHA:AES256-SHA:CAMELLIA128-SHA:AES128-SHA';
  #  add_header Strict-Transport-Security max-age=15768000; # six months
  #  # use this only if all subdomains support HTTPS!
  #  # add_header Strict-Transport-Security "max-age=15768000; includeSubDomains"
  #
  #  keepalive_timeout    70;
  #  server_name www.mysite.com;
  #  location / {
  #    proxy_pass  http://my-backend;
  #  }
  #}
}
```

> *Note* Make sure you are using version 1.2 or greater of Nginx otherwise chunked responses won't work properly.

## Set up with Apache

The example below shows a simple set up with [Apache httpd server](http://httpd.apache.org/) running in front of a standard Play configuration.

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

When using an HTTP frontal server, request addresses are seen as coming from the HTTP server. In a usual set-up, where you both have the Play app and the proxy running on the same machine, the Play app will see the requests coming from 127.0.0.1.

Proxy servers can add a specific header to the request to tell the proxied application where the request came from. Most web servers will add an X-Forwarded-For header with the remote client IP address as first argument. If the proxy server is running on localhost and connecting from 127.0.0.1, Play will trust its `X-Forwarded-For` header.  If you are running a reverse proxy on a different machine, you can set the `trustxforwarded` configuration item to true in the application configuration file, like so:

```
trustxforwarded=true
```

However, the host header is untouched, it’ll remain issued by the proxy. If you use Apache 2.x, you can add a directive like:

```
ProxyPreserveHost on
```

The host: header will be the original host request header issued by the client. By combining theses two techniques, your app will appear to be directly exposed.

If you don't want this play app to occupy the whole root, add an exclusion directive to the proxy config:

```
ProxyPass /excluded !
```

## Apache as a front proxy to allow transparent upgrade of your application

The basic idea is to run two Play instances of your web application and let the front-end proxy load-balance them. In case one is not available, it will forward all the requests to the available one.

Let’s start the same Play application two times: one on port 9999 and one on port 9998.

```
$ start -Dhttp.port=9998
$ start -Dhttp.port=9999
```

Now, let’s configure our Apache web server to have a load balancer.

In Apache, I have the following configuration:

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

The important part is `balancer://mycluster`. This declares a load balancer. The +H option means that the second Play application is on standby. But you can also instruct it to load balance.

Apache also provides a way to view the status of your cluster. Simply point your browser to `/balancer-manager` to view the current status of your clusters.

Because Play is completely stateless you don’t have to manage sessions between the 2 clusters. You can actually easily scale to more than 2 Play instances.

Note that [Apache does not support Websockets](https://issues.apache.org/bugzilla/show_bug.cgi?id=47485), so you may wish to use another front end proxy (such as haproxy or nginx) that does implement this functionality.

Note that [ProxyPassReverse might rewrite incorrectly headers](https://issues.apache.org/bugzilla/show_bug.cgi?id=51982) adding an extra / to the URIs, so you may wish to use this workaround:
```
ProxyPassReverse / http://localhost:9999
ProxyPassReverse / http://localhost:9998
```

## Configure trusted proxies

To determine the client IP address Play has to know which are the trusted proxies in your network.

Those can be configured with `play.http.forwarded.trustedProxies`. You can define a list of proxies
and/or subnet masks that Play recognizes as belonging to your network.

Default is `127.0.0.1` and `::FF`

There exists two possibilities how proxies are set in the HTTP-headers:

  * the legacy method with X-Forwarded headers
  * the RFC 7239 with Forwarded headers

The type of header to parse is set via `play.http.forwarded.version`. Valid values are `x-forwarded` or `rfc7239`.
The default is `x-forwarded`.

For more information, please read the [RFC 7239](http://tools.ietf.org/html/rfc7239).
