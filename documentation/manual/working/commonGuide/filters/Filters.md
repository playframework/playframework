<!--- Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com> -->
# Built-in HTTP filters

Play provides several standard filters that can modify the HTTP behavior of your application. You can also write your own filters in either [[Java|JavaHttpFilters]] or [[Scala|ScalaHttpFilters]].

- [[Configuring gzip encoding|GzipEncoding]]
- [[Configuring security headers|SecurityHeaders]]
- [[Configuring CORS|CorsFilter]]
- [[Configuring allowed hosts|AllowedHostsFilter]]

## Default Filters

Play now comes with a default set of enabled filters, defined through configuration.  If the property `play.http.filters` is null, then the default is now `play.api.http.DefaultFilters`, which loads up the filters defined by fully qualified class name in the `play.filters.defaults` configuration property.

In Play itself, `play.filters.defaults` is an empty list.  However, the `PlayFilters` is automatically loaded in SBT as an AutoPlugin, and will append the following values to the `play.filters.defaults` property:

* `play.filters.csrf.CSRFFilter`
* `play.filters.headers.SecurityHeadersFilter`
* `play.filters.hosts.AllowedHostsFilter`

This means that on new projects, CSRF protection ([[ScalaCsrf]] / [[JavaCsrf]]), [[SecurityHeaders]] and [[AllowedHostsFilter]] are all defined automatically.

If you define your own list of filters (for example, by adding a `Filters` class to the root), then the default list is overridden.  You must inject `DefaultFilters` and compose the filters if you want to append to the defaults, or append to the defaults list:

```
play.filters.defaults+=MyFilter
```

### Disabling Default Filters

The simplest way to disable the default filters is to set the list of filters manually in `application.conf`:

```
play.filters.defaults=[]
```

This may be useful if you have functional tests that you do not want to go through the default filters.

If you want to remove all filter classes, you can disable it through the `disablePlugins` mechanism:

```
lazy val root = project.in(file(".")).enablePlugins(PlayScala).disablePlugins(PlayFilters)
```
