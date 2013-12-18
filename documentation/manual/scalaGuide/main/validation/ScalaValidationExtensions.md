# Extensions: Supporting new types

The validation API is designed to be easily extensible. Supporting new types is just a matter of providing the appropriate set of Rules and Writes.

In this documentation, we'll study the implementation of the Json support. All extensions are to be defined in a similar fashion. The total amount of code needed is rather small, but there's best practices you need to follow.

## Rules

The first step is to define what we call primitive rules. Primitive rules is a set of rules on which you could build any complex validation.

The base of all Rules is the capacity to extract a subset of some input data.

For the type `JsValue`, we need to be able to extract a `JsValue` at a given `Path`:

@[extensions-rules](code/ScalaValidationExtensions.scala)

Now we are able to do this:

@[extensions-rules-jsvalue](code/ScalaValidationExtensions.scala)

Which is nice, but is would be much more convenient if we could extract that value as an `Int`.

One solution is to write the following method:

```scala
implicit def pickIntInJson[O](p: Path): Rule[JsValue, JsValue] = ???
```

But we would end up copying 90% of the code we already wrote.
Instead of doing so, we're going to make `pickInJson` a bit smarter by adding an implicit parameter:

@[extensions-rules-gen](code/ScalaValidationExtensions.scala)

The now all we have to do is to write a `Rule[JsValue, O]`, and we automatically get the ` Path => Rule[JsValue, O]` we're interested in. The rest is just a matter of defining all the prmitives rules, for example:

@[extensions-rules-primitives](code/ScalaValidationExtensions.scala)

The types you generally want to support natively are:

- String
- Boolean
- Int
- Short
- Long
- Float
- Double
- java BigDecimal
- scala BigDecimal

## Higher order Rules

Supporting primitives is nice, but not enough. Users are going to deal with `Seq` and `Option`. We need to support those types too.

### Option

What we want to do is to implement a function that takes a `Path => Rule[JsValue, O]`, an lift it into a `Path => Rule[JsValue, Option[O]]` for any type `O`. The reason we're working on the fully defined `Path => Rule[JsValue, O]` and not just `Rule[JsValue, O]` is because a non existent `Path` must be validated as a `Success(None)`. If we were to use `pickInJson` on a `Rule[JsValue, Option[O]]`, we would end up with a `Failure` in the case of non-existing `Path`.

The `play.api.data.mapping.DefaultRules[I]` traits provides a helper for building the desired method. It's signature is:

```scala
protected def opt[J, O](r: => Rule[J, O], noneValues: Rule[I, I]*)(implicit pick: Path => Rule[I, I], coerce: Rule[I, J]): Path = Rule[I, O]
```

- `noneValues` is a List of all the values we should consider to be `None`. For Json that would be `JsNull`.
- `pick` is a extractor. It's going to extract a subtree.
- `coerce` is a type conversion `Rule`
- `r` is a `Rule` to be applied on the data if they are found

What you do is to use this method to implement a specialized version for your type.
For example it's defined this way for Json:

```scala
def option[J, O](r: => Rule[J, O], noneValues: Rule[JsValue, JsValue]*)(implicit pick: Path => Rule[JsValue, JsValue], coerce: Rule[JsValue, J]): Path => Rule[JsValue, Option[O]]
    = super.opt[J, O](r, (jsNull.fmap(n => n: JsValue) +: noneValues):_*)
```
Basically it's just the same, but we are now only supporting `JsValue`. We are also adding JsNull is the list of None-ish values.

Despite the type signature funkiness, this function is actually **really** simple to use:

@[extensions-rules-opt](code/ScalaValidationExtensions.scala)

Alright, so now we can explicitly define rules for optional data.

But what if we write `(__ \ "age").read[Option[Int]]` ? It does not compile !
We need to define an implicit rule for that:

```scala
implicit def option[O](p: Path)(implicit pick: Path => Rule[JsValue, JsValue], coerce: Rule[JsValue, O]): Rule[JsValue, Option[O]] =
    option(Rule.zero[O])(pick, coerce)(p)
```

@[extensions-rules-opt-int](code/ScalaValidationExtensions.scala)

### Lazyness

It's very important that every Rule is completely lazily evaluated . The reason for that is that you may be validating recursive types:

@[extensions-rules-recursive](code/ScalaValidationExtensions.scala)

## Writes

Writes are implemented in a similar fashion, but a generally easier to implement. You start by defining a function for writing at a given path:

@[extensions-writes](code/ScalaValidationExtensions.scala)

And you then defines all the primitive writes:

@[extensions-writes-anyval](code/ScalaValidationExtensions.scala)

### Monoid

In order to be able to use writes combinators, you also need to create an implementation of `Monoid` for your output type. For example, to create a complex write of `JsObject`, we had to implement a `Monoid[JsObject]`:

@[extensions-writes-monoid](code/ScalaValidationExtensions.scala)

from there you're able to create complex writes like:

@[write-combine](code/ScalaValidationWriteCombinators.scala)

## Testing

We highly recommend you to test your rules as much as possible. There's a few tricky cases you need to handle properly. You should port the tests in `RulesSpec.scala` and use them on your rules.

> **Next:** - [[Cookbook | ScalaValidationCookbook]]