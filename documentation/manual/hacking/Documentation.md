<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Guidelines for writing Play documentation

The Play documentation is written in Markdown format, with code samples extracted from compiled, run and tested source files.

There are a few guidelines that must be adhered to when writing Play documentation.

## Markdown

All markdown files must have unique names across the entire documentation, regardless of what folders they are in.  Play uses a wiki style of linking and rendering documentation.

Newline characters in the middle of paragraphs are considered hard wraps, similar to GitHub flavored markdown, and are rendered as line breaks.  Paragraphs should therefore be contained on a single line.

### Links

Links to other pages in the documentation should be created using wiki markup syntax, for example:

    [[Optional description|ScalaRouting]]

Images should also use the above syntax.

> External links should not use the above syntax, but rather, should use the standard Markdown link syntax.

## Code samples

All supported code samples should be imported from external compiled files.  The syntax for doing this is:

    @[some-label](code/SomeFeature.scala)

The file should then delimit the lines that need to be extracted using `#some-label`, for example:

```scala
object SomeFeatureSpec extends Specification {
  "some feature" should {
    "do something" in {
      //#some-label
      val msg = Seq("Hello", "world").mkString(" ")
      //#some-label
      msg must_== "Hello world"
    }
  }
}
```

In the above case, the ``val msg = ...`` line will be extracted and rendered as code in the page.  All code samples should be checked to ensure they compile, run, and if it makes sense, ensure that it does what the documentation says it does.  It should not try to test the features themselves.

All code samples get run on the same classloader.  Consequently they must all be well namespaced, within a package that corresponds to the part of the documentation they are associated with.

In some cases, it may not be possible for the code that should appear in the documentation to exactly match the code that you can write given the above guidelines.  In particular, some code samples require the use of package names like `controllers`.  As a last resort if there are no other ways around this, there are a number of directives you can put in the code to instruct the code samples extractor to modify the sample.  These are:

* `###replace: foo` - Replace the next line with `foo`.  You may optionally terminate this command with `###`
* `###insert: foo` - Insert `foo` before the next line.  You may optionally terminate this command with `###`
* `###skip` - Skip the current line
* `###skip: n` - Skip the next n lines

For example:

```scala
//#controller
//###replace: package controllers
package foo.bar.controllers

import play.api.mvc._

object Application extends Controller {
  ...
}
//#controller
```

> These directives must only be used as a last resort, since the point of pulling code samples out into external files is that the very code that is in the documentation is also compiled and tested.  Directives break this.

It's also important to be aware of the current context of the code samples, to ensure that the appropriate import statements are documented.  However it doesn't make sense to necessarily include all import statements in every code sample, so discretion must be shown here.

Guidelines for specific types of code samples are below.

### Scala

All scala code samples should be tested using specs, and the code sample, if possible, should be inside the spec.  Local classes and method definitions are encouraged where appropriate.  Scoping import statements within blocks are also encouraged.

### Java

All Java code samples should be tested using JUnit.  Simple code samples are usually simple to include inside the JUnit test, but when the code sample is a method or a class, it gets harder.  Preference should be shown to use local and inner classes, but this may not be possible, for example, a static method can only appear on a static inner class, but that means adding the static modifier to the class, which would not appear if it was an outer class.  Consequently it may be necessary in some cases to pull Java code samples out into their own files.

### Scala Templates

Scala template code samples should be tested either with Specs in Scala or JUnit in Java.  Note that templates are compiled with different default imports, depending on whether they live in the Scala documentation or the Java documentation.  It is therefore also important to test them in the right context, if a template is relying on Java thread locals, they should be tested from a Java action.

Where possible, template code samples should be consoloditated in a single file, but this may not always be possible, for example if the code sample contains a parameter declaration.

### Routes files

Routes files should be tested either with Specs in Scala or JUnit in Java.  Routes files should be named with the full package name, for example, `scalaguide.http.routing.routes`, to ensure that they are isolated from other routes code samples.

The routes compiler used by the documentation runs in a special mode that generates the reverse router inside the namespace declared by that file.  This means that although a routes code sample may appear to use absolute references to classes, it is actually relative to the namespace of the router.  Thus in the above routes file, if you have a route called `controllers.Application`, it will actually refer to a controller called `scalaguide.http.routing.controllers.Application`.

### SBT code

At current, SBT code samples cannot be pulled out of the documentation, since compiling and running them will require a very custom SBT setup involving using completely different classloaders and classpaths.

### Other code

Other code may or may not be testable.  It may make sense to test Javascript code by running an integration test using HTMLUnit.  It may make sense to test configuration by loading it.  Common sense should be used here.

## Testing the docs

To build the docs, you'll first need to build and publish Play locally. You can do this by running `./build publish-local` from within the `framework` directory of the playframework repository.

To ensure that the docs render correctly, run `./build run` from within the `documentation` directory.  This will start a small Play server that does nothing but serve the documentation.

To ensure that the code samples compile, run and tests pass, run `./build test`.

To validate that the documentation is structurely sound, run `./build validate-docs`.  This checks that there are no broken wiki links, code references or resource links, ensures that all documentation markdown filenames are unique, and ensures that there are no orphaned pages.

## Code samples from external Play modules

To avoid circular dependencies, any documentation that documents a Play module that is not a core part of Play can't include its code samples along with the rest of the Play documentation.  To address this, the documentation for that module can place an entry into the `externalPlayModules` map in `project/Build.scala`, including all the extra settings (namely library dependencies) required to build the code snippets for that module.  For example:

```scala
val externalPlayModules: Map[String, Seq[Setting[_]]] = Map(
  "some-module" -> Seq(
    libraryDependencies += "com.example" %% "some-play-module" % "1.2.3" % "test"
  ),
  ...
)
```

Now place all code snippets that use that module in `code-some-module`.  Now to run any SBT commands, ensuring that that module is included, run `./build -Dexternal.modules=some-module test`, or to run the tests for all modules, run `./build -Dexternal-modules=all test`.
