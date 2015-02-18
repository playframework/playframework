<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Handling data streams reactively

## The realm of Enumeratees

‘Enumeratee’ is a very important component in the iteratees API. It provides a way to adapt and transform streams of data. An `Enumeratee` that might sound familiar is the `Enumeratee.map`.

Starting with a simple problem, consider the following `Iteratee`:

```scala
val sum: Iteratee[Int,Int] = Iteratee.fold[Int,Int](0){ (s,e) => s + e }
```

This `Iteratee` takes `Int` objects as input and computes their sum. Now if we have an `Enumerator` like the following:

```scala
val strings: Enumerator[String] = Enumerator("1","2","3","4")
```

Then obviously we can not apply the `strings:Enumerator[String]` to an `Iteratee[Int,Int]`. What we need is transform each `String` to the corresponding `Int` so that the source and the consumer can be fit together. This means we either have to adapt the `Iteratee[Int,Int]` to be `Iteratee[String,Int]`, or adapt the `Enumerator[String]` to be rather an `Enumerator[Int]`.
An `Enumeratee` is the right tool for doing that. We can create an `Enumeratee[String,Int]` and adapt our `Iteratee[Int,Int]` using it:

```scala
//create am Enumeratee using the map method on Enumeratee
val toInt: Enumeratee[String,Int] = Enumeratee.map[String]{ s => s.toInt } 

val adaptedIteratee: Iteratee[String,Int] = toInt.transform(sum)

//this works!
strings |>> adaptedIteratee
```
There is a symbolic alternative to the `transform` method, `&>>` which we can use in our previous example:

```scala
strings |>> toInt &>> sum 
```

The `map` method will create an 'Enumeratee' that uses a provided `From => To` function to map the input from the `From` type to the `To` type. We can also adapt the `Enumerator`:

```scala
val adaptedEnumerator: Enumerator[Int] = strings.through(toInt)

//this works!
adaptedEnumerator |>> sum
```

Here too, we can use a symbolic version of the `through` method:

```scala
strings &> toInt |>> sum
```

Let’s have a look at the `transform` signature defined in the `Enumeratee` trait:

```scala
trait Enumeratee[From, To] {
  def transform[A](inner: Iteratee[To, A]): Iteratee[From, A] = ...
}
```

This is a fairly simple signature, and is the same for `through` defined on an `Enumerator` :

```scala
trait Enumerator[E] {
  def through[To](enumeratee: Enumeratee[E, To]): Enumerator[To] 
}
```

The `transform` and `through` methods on an `Enumeratee` and `Enumerator`, respectively, both use the `apply` method on `Enumeratee`, which has a slightly more sophisticated signature:

```scala
trait Enumeratee[From, To] {
  def apply[A](inner: Iteratee[To, A]): Iteratee[From, Iteratee[To, A]] = ...
}
```

Indeed, an `Enumeratee` is more powerful than just transforming an `Iteratee` type. It really acts like an adapter in that you can get back your original `Iteratee` after pushing some different input through an `Enumeratee`. So in the previous example, we can get back the original `Iteratee[Int,Int]` to continue pushing some `Int` objects in:

```scala
val sum:Iteratee[Int,Int] = Iteratee.fold[Int,Int](0){ (s,e) => s + e }

//create am Enumeratee using the map method on Enumeratee
val toInt: Enumeratee[String,Int] = Enumeratee.map[String]{ s => s.toInt } 

val adaptedIteratee: Iteratee[String,Iteratee[Int,Int]] = toInt(sum)

// pushing some strings
val afterPushingStrings: Future[Iteratee[String,Iteratee[Int,Int]]] = {
   Enumerator("1","2","3","4") |>> adaptedIteratee
}

val flattenAndRun:Future[Iteratee[Int,Int]] = Iteratee.flatten(afterPushingStrings).run

val originalIteratee = Iteratee.flatten(flattenAndRun)

val moreInts: Future[Iteratee[Int,Int]] = Enumerator(5,6,7) |>> originalIteratee

val sumFuture:Future[Int] = Iteratee.flatten(moreInts).run

sumFuture onSuccess {
  case s => println(s)// eventually prints 28 
} 
```

That’s why we call the adapted (original) `Iteratee` ‘inner’ and the resulting `Iteratee` ‘outer’.

Now that the `Enumeratee` picture is clear, it is important to know that `transform` drops the left input of the inner `Iteratee` when it is `Done`. This means that if we use `Enumeratee.map` to transform input, if the inner `Iteratee` is `Done` with some left transformed input, the `transform` method will just ignore it.

That might have seemed like a bit too much detail, but it is useful for grasping the model.

Back to our example on `Enumeratee.map`, there is a more general method `Enumeratee.mapInput` which, for example, gives the opportunity to return an `EOF` on some signal:

```scala
val toIntOrEnd: Enumeratee[String,Int ] = Enumeratee.mapInput[String] {
  case Input.El("end") => Input.EOF
  case other => other.map(e => e.toInt)
}
```

`Enumeratee.map` and `Enumeratee.mapImput` are pretty straight forward, they operate on a per chunk basis and they convert them. Another useful `Enumeratee` is the `Enumeratee.filter` :

```scala
def filter[E](predicate: E => Boolean): Enumeratee[E, E]
```

The signature is pretty obvious, `Enumeratee.filter` creates an `Enumeratee[E,E]` and it will test each chunk of input using the provided `predicate: E => Boolean` and it passes it along to the inner (adapted) iteratee if it statisfies the predicate:

```scala
val numbers = Enumerator(1,2,3,4,5,6,7,8,9,10)

val onlyOdds = Enumeratee.filter[Int](i => i % 2 != 0)

numbers.through(onlyOdds) |>> sum
```

There are methods, such as `Enumeratee.collect`, `Enumeratee.drop`, `Enumeratee.dropWhile`, `Enumeratee.take`, `Enumeratee.takeWhile`, which work on the same principle.
Let try to use the `Enumeratee.take` on an Input of chunks of bytes:

```scala
// computes the size in bytes
val fillInMemory: Iteratee[Array[Byte],Array[Byte]] = {
  Iteratee.consume[Array[Byte]]()
}

val limitTo100: Enumeratee[Array[Byte],Array[Byte]] = {
  Enumeratee.take[Array[Byte]](100)
}

val limitedFillInMemory: Iteratee[Array[Byte],Array[Byte]] = {
  limitTo100 &>> fillInMemory
}
```

It looks good, but how many bytes are we taking? What would ideally limit the size, in bytes, of loaded input. What we do above is to limit the number of chunks instead, whatever the size of each chunk is. It seems that the `Enumeratee.take` is not enough here since it has no information about the type of input (in our case an `Array[Byte]`) and this is why it can’t count what’s inside.

Luckily there is a `Traversable` object that offers a set of methods for creating `Enumeratee` instances for Input types that are `TraversableLike`. An `Array[Byte]` is `TraversableLike` and so we can use`Traversable.take`:

```scala
val fillInMemory: Iteratee[Array[Byte],Array[Byte]] = {
  Iteratee.consume[Array[Byte]]()
}

val limitTo100: Enumeratee[Array[Byte],Array[Byte]] = {
  Traversable.take[Array[Byte]](100)
}

// We are sure not to get more than 100 bytes loaded into memory
val limitedFillInMemory: Iteratee[Array[Byte],Array[Byte]] = {
  limitTo100 &>> fillInMemory
}
```

Other `Traversable` methods exist including `Traversable.takeUpTo`, `Traversable.drop`.

Finally, you can compose different `Enumeratee` instances using the `compose` method, which has the symbolic equivalent `><>`. Note that any left input on the `Done` of the composed `Enumeratee` instances will be dropped. However, if you use `composeConcat` aliased `>+>`, any left input will be concatenated. 
