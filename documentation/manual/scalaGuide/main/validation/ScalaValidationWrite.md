# Serializing data

## Introduction

To serialize data, the validation API provides the `Write` type. A `Write[I, O]` defines a way to transform data, from type `I` to type `O`. It's basically a function `I => O`, where `I` is the type of the input to serialize, and `O` is the expected output type.

## A simple example

Let's say you want to serialize a `Float` to `String`.
All you need to do is to define a `Write` from `Float` to `String`:

```scala
import play.api.data.mapping._
def floatToString: Write[Float, String] = ???
```

For now we'll not implement `floatToString`, actually the validation API comes with a number of built-in Writes, including `Writes.anyval[T]`.

All you have to do is import the default Writes.

```scala
scala> import play.api.data.mapping.Writes
import play.api.data.mapping.Writes

scala> :t Writes.anyval[Float]
play.api.data.mapping.Write[Float,String]
```

Let's now test it against different `Float` values:

```scala
scala> Writes.anyval.writes(12.8F)
res40: String = 12.8

scala> Writes.anyval.writes(12F)
res43: String = 12.0
```

## Defining your own `Write`

Creating a new `Write` is almost as simple as creating a new function.
This example creates a new `Write` serializing a Float with a custom format.

```scala
val currency = Write[Double, String]{ money =>
  import java.text.NumberFormat
  import java.util.Locale
  val f = NumberFormat.getCurrencyInstance(Locale.FRANCE)
  f.format(money)
}
```

Testing it:

```scala
scala> currency.writes(9.99)
res1: String = 9,99 €
```

## Composing Writes

Writes composition is very important in this API. `Write` composition means that given two writes `a: Write[I, J]` and `b: Write[J, O]`, we can create a new write `c: Write[I, O]`.

### Example

Let's see we're working working on a e-commerce website. We have defined a `Product` class.
Each product has a name and a price:

```scala
case class Product(name: String, price: Double)
```

Now we'd like to create a `Write[Product, String]` that serializes a product to a `String` of it price: `Product("demo", 123)` becomes `123,00 €`

We have already defined `currency: Write[Double, String]`, so we'd like to reuse that.
First, we'll create a `Write[Product, Double]` extracting the price of the product:

```scala
val productPrice = Write[Product, Double]{ _.price }
```

Now we just have to compose it with `currency`:

```scala
val productAsPrice: Write[Product,String] = productPrice compose currency
```

Let's test our new `Write`:

```scala
scala> productAsPrice.writes(Product("Awesome product", 9.99))
res1: String = 9,99 €
```

> **Next:** [[ Complex serialization with Writes combinators | ScalaValidationWriteCombinators]]