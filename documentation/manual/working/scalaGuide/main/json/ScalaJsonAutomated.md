<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# JSON automated mapping

If the JSON maps directly to a class, we provide a handy macro so that you don't have to write the `Reads[T]`, `Writes[T]`, or `Format[T]` manually. Given the following case class :

@[model](code/ScalaJsonAutomatedSpec.scala)

The following macro will create a `Reads[Resident]` based on its structure and the name of its fields :

@[auto-reads](code/ScalaJsonAutomatedSpec.scala)

When compiling, the macro will inspect the given class and
inject the following code, exactly as if you had written it manually :

@[manual-reads](code/ScalaJsonAutomatedSpec.scala)

This is done **at compile-time**, so you don't lose any type safety or performance.
Similar macros exists for a `Writes[T]` or a `Format[T]` :

@[auto-writes](code/ScalaJsonAutomatedSpec.scala)
@[auto-format](code/ScalaJsonAutomatedSpec.scala)

### Requirements

These macros rely on a few assumptions about the type they're working with :
- It must have a companion object having `apply` and `unapply` methods
- The return types of the `unapply` must match the argument types of the `apply` method.
- The parameter names of the `apply` method must be the same as the property names desired in the JSON.

Case classes natively meet these requirements. For more custom classes or traits, you might
have to implement them.
