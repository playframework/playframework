# Serializing data

## Introduction

To serialize data, the validation API provides the `Write` type. A `Write[I, O]` defines a way to transform data, from type `I` to type `O`. It's basically a function `I => O`, where `I` is the type of the input to serialize, and `O` is the expected output type.

## A simple example

Let's say you want to serialize a `Float` to `String`.
All you need to do is to define a `Write` from `Float` to `String`:

@[write-first-ex](code/ScalaValidationWrite.scala)

For now we'll not implement `floatToString`, actually the validation API comes with a number of built-in Writes, including `Writes.anyval[T]`.

All you have to do is import the default Writes.

```scala
scala> import play.api.data.mapping.Writes
import play.api.data.mapping.Writes

scala> :t Writes.anyval[Float]
play.api.data.mapping.Write[Float,String]
```

Let's now test it against different `Float` values:

@[write-first-defaults](code/ScalaValidationWrite.scala)

## Defining your own `Write`

Creating a new `Write` is almost as simple as creating a new function.
This example creates a new `Write` serializing a Float with a custom format.

@[write-custom](code/ScalaValidationWrite.scala)

Testing it:

@[write-custom-test](code/ScalaValidationWrite.scala)

## Composing Writes

Writes composition is very important in this API. `Write` composition means that given two writes `a: Write[I, J]` and `b: Write[J, O]`, we can create a new write `c: Write[I, O]`.

### Example

Let's see we're working working on a e-commerce website. We have defined a `Product` class.
Each product has a name and a price:

@[write-product](code/ScalaValidationWrite.scala)

Now we'd like to create a `Write[Product, String]` that serializes a product to a `String` of it price: `Product("demo", 123)` becomes `123,00 €`

We have already defined `currency: Write[Double, String]`, so we'd like to reuse that.
First, we'll create a `Write[Product, Double]` extracting the price of the product:

@[write-product-price](code/ScalaValidationWrite.scala)

Now we just have to compose it with `currency`:

@[write-product-asprice](code/ScalaValidationWrite.scala)

Let's test our new `Write`:

@[write-product-asprice-test](code/ScalaValidationWrite.scala)

> **Next:** [[ Complex serialization with Writes combinators | ScalaValidationWriteCombinators]]