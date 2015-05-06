# Play enhancer

The [Play enhancer](https://github.com/playframework/play-enhancer) is an sbt plugin that generates getters and setters for Java beans, and rewrites the code that accesses those fields to use the getters and setters.

## Motivation

One common criticism of Java is that simple things require a lot of boilerplate code.  One of the biggest examples of this is encapsulating fields - it is considered good practice to encapsulate the access and mutation of fields in methods, as this allows future changes such as validation and generation of the data. In Java, this means making all your fields private, and then writing getters and setters for each field, a typical overhead of 6 lines of code per field.

Furthermore, many libraries, particularly libraries that use reflection to access properties of objects such as ORMs, require classes to be implemented in this way, with getters and setters for each field.

The Play enhancer provides a convenient alternative to manually implementing getters and setters. It implements some post processing on the compiled byte code for your classes, this is commonly referred to as byte code enhancement. For every public field in your classes, Play will automatically generate a getter and setter, and then will rewrite the code that uses these fields to use the getters and setters instead.

### Drawbacks

Using byte code enhancement to generating getters and setters is not without its drawbacks however.  Here are a few:

* Byte code enhancement is opaque, you can't see the getters and setters that are generated, so when things go wrong, it can be hard to debug and understand what is happening. Byte code enhancement is ofter described as being "magic" for this reason.
* Byte code enhancement can interfere with the operation of some tooling, such as IDEs, as they will be unaware of the eventual byte code that gets used. This can cause problems such as tests failing when run in an IDE because they depend on byte code enhancement, but the IDE isn't running the byte code enhancer when it compiles your source files.
* Existing Java developers that are new to your codebase will not expect getters and setters to be generated, this can cause confusion.

Whether you use the Play enhancer or not in your projects is up to you, if you do decide to use it the most important thing is that you understand what the enhancer does, and what the drawbacks may be.

## Setting up

To enable the byte code enhancer, simply add the following line to your `project/plugins.sbt` file:

@[plugins.sbt](code/enhancer.sbt)

The Play enhancer should be enabled for all your projects.  If you want to disable the Play enhancer for a particular project, you can do that like so in your `build.sbt` file:

@[disable-project](code/enhancer.sbt)

In some situations, it may not be possible to disable the enhancer plugin, an example of this is using Play's ebean plugin, which requires the enhancer to ensure that getters and setters are generated before it does its byte code enhancement.  If you don't want to generate getters and setters in that case, you can use the `playEnhancerEnabled` setting:

@[disable-enhancement](code/enhancer.sbt)

## Operation

The enhancer looks for all fields on Java classes that:

* are public
* are non static
* are non final

For each of those fields, it will generate a getter and a setter if they don't already exist.  If you wish to provide a custom getter or setter for a field, this can be done by just writing it, the Play enhancer will simply skip the generation of the getter or setter if it already exists.

## Configuration

If you want to control exactly which files get byte code enhanced, this can be done by configuring the `sources` task scoped to the `playEnhancerGenerateAccessors` and `playEnhancerRewriteAccessors` tasks.  For example, to only enhance the java sources in the models package, you might do this:

@[select-generate](code/enhancer.sbt)
