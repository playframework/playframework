<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# JSON automated mapping

If the JSON maps directly to a class, we provide a handy shortcut so that you don't have to write the `Reads[T]`, `Writes[T]`, or `Format[T]` manually. Given the following case class :

@[model](code/ScalaJsonAutomatedSpec.scala)

The following code will create a `Reads[Resident]` based on its structure and the name of its fields :

@[auto-reads](code/ScalaJsonAutomatedSpec.scala)

It is equivalent to the more verbose syntax :

@[manual-reads](code/ScalaJsonAutomatedSpec.scala)


The same shortcut exists for a `Writes[T]` or a `Format[T]` :

@[auto-writes](code/ScalaJsonAutomatedSpec.scala)
@[auto-format](code/ScalaJsonAutomatedSpec.scala)

> These shortcuts are actually macros. The given class is inspected
and the corresponding code is injected, just as if it had been written manually.
This is done **at compile-time**, so you don't lose any type safety or performance.

### Known limitations

These macros work only with structures having `apply/unapply` methods with
corresponding input/output types (like case classes). If you want to use it
with a Trait for instance, you must implement the same `apply/unapply` you would
have in a case class.
