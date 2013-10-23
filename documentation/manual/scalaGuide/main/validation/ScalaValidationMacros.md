# Validation Inception

> This feature is still experimental because Scala Macros are still experimental in Scala 2.10.0. If you prefer not using an experimental feature from Scala, please use hand-written `Rule` / `Write` which are strictly equivalent.

## Introduction

The validation API provides macro-based helpers to generate `Rule` and `Write` for case classes (or any class with a companion object providing `apply` / and `unapply` methods).

The generated code:

- is completely typesafe
- is compiled
- does not rely on runtime introspection **at all**
- is strictly equivalent to a hand-written definition

## Example

Traditionnaly, for a given case class `Person`, we would define a `Rule` like this:

@[macro-person-def](code/ScalaValidationMacros.scala)

@[macro-manual](code/ScalaValidationMacros.scala)

Let's test it:

@[macro-manual-test](code/ScalaValidationMacros.scala)

That exact `Rule` can be generated using `Rule.gen`:

@[macro-macro](code/ScalaValidationMacros.scala)

The result is exactly the same:

@[macro-macro-test](code/ScalaValidationMacros.scala)

We can also generate a `Write`:

@[macro-write](code/ScalaValidationMacros.scala)

## Known limitations

 - **Don’t override the apply method of the companion object.** The macro inspects the `apply` method to generate `Rule`/`Write`. Overloading the `apply` method creates an ambiguity the compiler will complain about.
 - **Macros only work when `apply` and `unapply` have corresponding input/output types**: This is naturally the case for case classes. But if you want to the same with a trait, you must implement the same  `apply`/`unapply` you would have in a case class.
- **Validation Macros accept `Option`/`Seq`/`List`/`Set` & `Map[String, _]`**. For other generic types, test and if it's not working, define your `Rule`/`Write` manually.

> **Next:** - [[Supporting new types | ScalaValidationExtensions]]