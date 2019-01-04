<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Allowed hosts filter

Play provides a filter that lets you configure which hosts can access your application. This is useful to prevent cache poisoning attacks. For a detailed description of how this attack works, see [this blog post](https://www.skeletonscribe.net/2013/05/practical-http-host-header-attacks.html). The filter introduces a whitelist of allowed hosts and sends a 400 (Bad Request) response to all requests with a host that do not match the whitelist.

This is an important filter to use even in development, because DNS rebinding attacks can be used against a developer's instance of Play: see [Rails Webconsole DNS Rebinding](https://benmmurphy.github.io/blog/2016/07/11/rails-webconsole-dns-rebinding/) for an example of how short lived DNS rebinding can attack a server running on localhost.

Note that if you are running a functional test against a Play application which has the AllowedHostsFilter, then `FakeRequest` and `Helpers.fakeRequest()` will create a request which already has `HOST` set to `localhost`.

## Enabling the allowed hosts filter

> **Note:** As of Play 2.6.x, the Allowed Hosts filter is included in Play's list of default filters that are applied automatically to projects.  See [[the Filters page|Filters]] for more information.

To enable the filter manually, add the allowed hosts filter to your filters in `application.conf`:

```
play.filters.enabled += play.filters.hosts.AllowedHostsFilter
```

## Configuring allowed hosts

You can configure which hosts the filter allows using `application.conf`. See the Play filters [`reference.conf`](resources/confs/filters-helpers/reference.conf) to see the defaults.

`play.filters.hosts.allowed` is a list of strings of the form `.example.com` or `example.com`. With a leading dot, the pattern will match example.com and all subdomains (`www.example.com`, `foo.example.com`, `foo.bar.example.com`, etc.). Without the leading dot it will just match the exact domain. If your application runs on a specific port, you can also include a port number, for instance `.example.com:8080`.

You can use the `.` pattern to match all hosts (not recommended in production). Note that the filter also strips the dot character from the end of the host, so the `example.com` pattern will match `example.com.`

An example configuration follows.

```
play.filters.hosts {
  # Allow requests to example.com, its subdomains, and localhost:9000.
  allowed = [".example.com", "localhost:9000"]
}
```

## Testing 

Because the AllowedHostsFilter filter is added automatically, functional tests need to have the Host HTTP header added.

If you are using `FakeRequest` or `Helpers.fakeRequest`, then the `Host` HTTP header is added for you automatically.  If you are using `play.mvc.Http.RequestBuilder`, then you may need to add your own line to add the header manually:

@[test-with-request-builder](code/javaguide/detailed/filters/FiltersTest.java)
