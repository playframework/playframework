<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Built-in HTTP filters

Play provides several standard filters that can modify the HTTP behavior of your application. You can also write your own filters in either [[Java|JavaHttpFilters]] or [[Scala|ScalaHttpFilters]].

- [[Configuring gzip encoding|GzipEncoding]]
- [[Configuring security headers|SecurityHeaders]]
- [[Configuring CORS|CorsFilter]]
- [[Configuring allowed hosts|AllowedHostsFilter]]
- [[Configuring Redirect HTTPS filter|RedirectHttpsFilter]]

## Default Filters

Play now comes with a default set of enabled filters, defined through configuration.  If the property `play.http.filters` is null, then the default is now `play.api.http.EnabledFilters`, which loads up the filters defined by fully qualified class name in the `play.filters.enabled` configuration property.

In Play itself, `play.filters.enabled` is an empty list.  However, the filters library is automatically loaded in sbt as an AutoPlugin called `PlayFilters`, and will append the following values to the `play.filters.enabled` property:

* `play.filters.csrf.CSRFFilter`
* `play.filters.headers.SecurityHeadersFilter`
* `play.filters.hosts.AllowedHostsFilter`

This means that on new projects, CSRF protection ([[ScalaCsrf]] / [[JavaCsrf]]), [[SecurityHeaders]] and [[AllowedHostsFilter]] are all defined automatically.

To append to the defaults list, use the `+=`:

```
play.filters.enabled+=MyFilter
```

If you have previously defined your own filters by extending `play.api.http.DefaultHttpFilters`, then you can also combine `EnabledFilters` with your own filters in code:

Java
: @[filters-combine-enabled-filters](code/javaguide/detailed/filters/Filters.java)

Scala
: @[filters-combine-enabled-filters](code/scalaguide/detailed/filters/ScalaFilters.scala)

Otherwise, if you have a `Filters` class in the root or have `play.http.filters` defined explicitly, it will take precedence over the `EnabledFilters` functionality described below.

### Testing Default Filters

Because there are several filters enabled, functional tests may need to change slightly to ensure that all the tests pass and requests are valid.  For example, a request that does not have a `Host` HTTP header set to `localhost` will not pass the AllowedHostsFilter and will return a 400 Forbidden response instead.

#### Testing with AllowedHostsFilter

Because the AllowedHostsFilter filter is added automatically, functional tests need to have the Host HTTP header added.

If you are using `FakeRequest` or `Helpers.fakeRequest`, then the `Host` HTTP header is added for you automatically.  If you are using `play.mvc.Http.RequestBuilder`, then you may need to add your own line to add the header manually:

@[test-with-request-builder](code/javaguide/detailed/filters/FiltersTest.java)

#### Testing with CSRFFilter

Because the CSRFFilter filter is added automatically, tests that render a Twirl template that includes `CSRF.formField`, i.e.

```html
@(userForm: Form[UserData])(implicit request: RequestHeader, m: Messages)

<h1>user form</h1>

@request.flash.get("success").getOrElse("")

@helper.form(action = routes.UserController.userPost()) {
  @helper.CSRF.formField
  @helper.inputText(userForm("name"))
  @helper.inputText(userForm("age"))
  <input type="submit" value="submit"/>
}
```

must contain a CSRF token in the request.  In the Scala API, this is done by importing `play.api.test.CSRFTokenHelper._`, which enriches `play.api.test.FakeRequest` with the `withCSRFToken` method:

@[test-with-withCSRFToken](code/scalaguide/detailed/filters/UserControllerSpec.scala)

In the Java API, this is done by calling `CSRFTokenHelper.addCSRFToken` on a `play.mvc.Http.RequestBuilder` instance:

@[test-with-addCSRFToken](code/javaguide/detailed/filters/FiltersTest.java)

### Disabling Default Filters

The simplest way to disable a filter is to add it to the `play.filters.disabled` list in `application.conf`:

```
play.filters.disabled+=play.filters.hosts.AllowedHostsFilter
```

This may be useful if you have functional tests that you do not want to go through the default filters.

To remove the default filters, you can set the entire list manually:

```
play.filters.enabled=[]
```

If you want to remove all filter classes, you can disable it through the `disablePlugins` mechanism:

```
lazy val root = project.in(file(".")).enablePlugins(PlayScala).disablePlugins(PlayFilters)
```

If you are writing functional tests involving `GuiceApplicationBuilder`, then you can disable all filters in a test by calling `configure`:

@[test-disabling-filters](code/scalaguide/detailed/filters/UserControllerSpec.scala)

## Compile Time Default Filters

If you are using compile time dependency injection, then the default filters are resolved at compile time, rather than through runtime.

This means that the [`play.api.BuiltInComponents`](api/scala/play/api/BuiltInComponents.html) trait (for Scala) and [`play.BuiltInComponents`](api/java/play/BuiltInComponents.html) interface (for Java) now contains an `httpFilters` method which is left abstract. The default list of filters is defined in [`play.filters.HttpFiltersComponents`](api/scala/play/filters/HttpFiltersComponents.html) for Scala and [`play.filters.components.HttpFiltersComponents`](api/java/play/filters/components/HttpFiltersComponents.html) for Java. So, for most cases you will want to mixin `HttpFiltersComponents` and append your own filters:

Java
: @[appending-filters-compile-time-di](code/javaguide/detailed/filters/add/MyAppComponents.java)

Scala
: @[appending-filters-compile-time-di](code/scalaguide/detailed/filters/FiltersComponents.scala)

If you want to filter elements out of the list, you can do the following:

Java
: @[removing-filters-compile-time-di](code/javaguide/detailed/filters/remove/MyAppComponents.java)

Scala
: @[removing-filters-compile-time-di](code/scalaguide/detailed/filters/FiltersComponents.scala)

### Disabling Compile Time Default Filters

To disable the default filters, mix in [`play.api.NoHttpFiltersComponents`](api/scala/play/api/NoHttpFiltersComponents.html) for Scala and [`play.filters.components.NoHttpFiltersComponents`](api/java/play/filters/components/NoHttpFiltersComponents.html) for Java:

Java
: @[remove-all-filters-compile-time-di](code/javaguide/detailed/filters/removeAll/MyAppComponents.java)

Scala
: @[remove-all-filters-compile-time-di](code/scalaguide/detailed/filters/FiltersComponents.scala)

Both Scala [`play.api.NoHttpFiltersComponents`](api/scala/play/api/NoHttpFiltersComponents.html)  and [`play.filters.components.NoHttpFiltersComponents`](api/java/play/filters/components/NoHttpFiltersComponents.html) have `httpFilters` method which returns an empty list of filters.