<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Play 3.1 Migration Guide

TBD

## How to migrate

Before starting `sbt`, make sure to make the following upgrades.

### Play upgrade

TBD

### sbt upgrade

TBD

### Minimum required Java and sbt version

TBD

### Java form binding no longer depends on Spring Framework libraries

Historically, Play's Java form binding used Spring Framework libraries, going back to the beginning of Play 2. Starting with this release, Play owns the form binding code it needs internally and registers the supported default conversions through Play's `Formatters` infrastructure.

This removes Spring from the application classpath for Java form binding and gives Play direct control over the binding behavior. The old integration inherited behavior that was useful for Spring bean configuration, but surprising for Play web forms: classpath scanning and factory lookup during JavaBean introspection, convention-based converter class loading (so called "editors" in Spring), resource location handling, class loading, and default converters that could resolve files, URLs, classpath resources, streams, or readers from submitted form values.

For Play applications, form binding should convert submitted request strings into application data values. It should not, by default, interpret user input as Spring resource expressions, open resources, inspect the classpath, or load classes. This avoids surprising resource access from user-submitted form data, such as opening streams or readers, resolving classpath resources, or loading classes during form binding.

As a result, the following types and Spring-specific behaviors are no longer bound by default:

* `java.io.File`
* `java.nio.file.Path`
* `java.io.InputStream`
* `java.io.Reader`
* `org.xml.sax.InputSource`
* `java.lang.Class`
* `java.lang.Class[]`
* Raw `java.lang.Enum` targets. Concrete enum types continue to bind by enum constant name.
* Spring resource types and resource patterns, if Spring is present in the application
* Spring-style resource locations such as `classpath:` URL/resource binding

Plain `URI` values are still parsed as URI values. For example, a `classpath:` URI is treated as ordinary URI text and is not resolved as a classpath resource by Play.

`URL` values are parsed as regular URLs only. Spring-style `classpath:` URL/resource binding is not supported.

This does not affect normal file uploads. Play file uploads use multipart form handling and `Http.MultipartFormData.FilePart`, not string-to-`File` form binding. See [[Handling file upload|JavaFileUpload]] for the Java file upload API.

If your application intentionally needs one of the removed bindings, register an explicit formatter or converter for that type in your application. See [[Register a custom DataBinder|JavaForms#Register-a-custom-DataBinder]] for the Java form formatter setup. If you think a removed binding should be supported by Play by default, please open an issue in the [Play issue tracker](https://github.com/playframework/playframework/issues).
