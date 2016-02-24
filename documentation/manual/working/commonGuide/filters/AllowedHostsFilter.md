<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Allowed hosts filter

Play provides a filter that lets you configure which hosts can access your application. This is useful to prevent cache poisoning attacks. For a detailed description of how this attack works, see [this blog post](http://www.skeletonscribe.net/2013/05/practical-http-host-header-attacks.html). The filter introduces a whitelist of allowed hosts and sends a 400 (Bad Request) response to all requests with a host that do not match the whitelist.

## Enabling the allowed hosts filter

To enable the filter, first add the Play filters project to your `libraryDependencies` in `build.sbt`:

@[content](code/filters.sbt)

Now add the allowed hosts filter to your filters, which is typically done by creating a `Filters` class in the root of your project:

Scala
: @[filters](code/AllowedHostsFilter.scala)

Java
: @[filters](code/detailedtopics/configuration/hosts/Filters.java)

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
