<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Handling data streams reactively

## Enumerators

If an iteratee represents the consumer, or sink, of input, an `Enumerator` is the source that pushes input into a given iteratee. As the name suggests, it enumerates some input into the iteratee and eventually returns the new state of that iteratee. This can be easily seen looking at the `Enumerator`’s signature:

```scala
trait Enumerator[E] {

  /**
   * Apply this Enumerator to an Iteratee
   */
  def apply[A](i: Iteratee[E, A]): Future[Iteratee[E, A]]

}
```

An `Enumerator[E]` takes an `Iteratee[E,A]` which is any iteratee that consumes `Input[E]` and returns a `Future[Iteratee[E,A]]` which eventually gives the new state of the iteratee.

We can go ahead and manually implement `Enumerator` instances by consequently calling the iteratee’s fold method, or use one of the provided `Enumerator` creation methods. For instance we can create an `Enumerator[String]` that pushes a list of strings into an iteratee, like the following:

```scala
val enumerateUsers: Enumerator[String] = {
  Enumerator("Guillaume", "Sadek", "Peter", "Erwan")
}
```

Now we can apply it to the consume iteratee we created before:

```scala
val consume = Iteratee.consume[String]()
val newIteratee: Future[Iteratee[String,String]] = enumerateUsers(consume) 
```

To terminate the iteratee and extract the computed result we pass `Input.EOF`. An `Iteratee` carries a `run` method that does just this. It pushes an `Input.EOF` and returns a `Future[A]`, ignoring left input if any.

```scala
// We use flatMap since newIteratee is a promise, 
// and run itself return a promise
val eventuallyResult: Future[String] = newIteratee.flatMap(i => i.run)

//Eventually print the result
eventuallyResult.onSuccess { case x => println(x) }

// Prints "GuillaumeSadekPeterErwan"
```

You might notice here that an `Iteratee` will eventually produce a result (returning a promise when calling fold and passing appropriate calbacks), and a `Future` eventually produces a result. Then a `Future[Iteratee[E,A]]` can be viewed as `Iteratee[E,A]`. Indeed this is what `Iteratee.flatten` does, Let’s apply it to the previous example:

```scala
//Apply the enumerator and flatten then run the resulting iteratee
val newIteratee = Iteratee.flatten(enumerateUsers(consume))

val eventuallyResult: Future[String] = newIteratee.run
   
//Eventually print the result 
eventuallyResult.onSuccess { case x => println(x) }

// Prints "GuillaumeSadekPeterErwan"
```

An `Enumerator` has some symbolic methods that can act as operators, which can be useful in some contexts for saving some parentheses. For example, the `|>>` method works exactly like apply:

```scala
val eventuallyResult: Future[String] = {
  Iteratee.flatten(enumerateUsers |>> consume).run
}
```

Since an `Enumerator` pushes some input into an iteratee and eventually return a new state of the iteratee, we can go on pushing more input into the returned iteratee using another `Enumerator`. This can be done either by using the `flatMap` function on `Future`s or more simply by combining `Enumerator` instancess using the `andThen` method, as follows:

```scala
val colors = Enumerator("Red","Blue","Green")

val moreColors = Enumerator("Grey","Orange","Yellow")

val combinedEnumerator = colors.andThen(moreColors)

val eventuallyIteratee = combinedEnumerator(consume)
```

As for apply, there is a symbolic version of the `andThen` called `>>>` that can be used to save some parentheses when appropriate:

```scala
val eventuallyIteratee = {
  Enumerator("Red","Blue","Green") >>>
  Enumerator("Grey","Orange","Yellow") |>>
  consume    
}
```

We can also create `Enumerator`s for enumerating files contents:

```scala
val fileEnumerator: Enumerator[Array[Byte]] = {
  Enumerator.fromFile(new File("path/to/some/file"))
}
```

Or more generally enumerating a `java.io.InputStream` using `Enumerator.fromStream`. It is important to note that input won't be read until the iteratee this `Enumerator` is applied on is ready to take more input.

Actually both methods are based on the more generic `Enumerator.generateM` that has the following signature:

```scala
def generateM[E](e: => Future[Option[E]]) = {
  ... 
}
```

This method defined on the `Enumerator` object is one of the most important methods for creating `Enumerator`s from imperative logic. Looking closely at the signature, this method takes a callback function `e: => Future[Option[E]]` that will be called each time the iteratee this `Enumerator` is applied to is ready to take some input.

It can be easily used to create an `Enumerator` that represents a stream of time values every 100 millisecond using the opportunity that we can return a promise, like the following:

```scala
Enumerator.generateM {
  Promise.timeout(Some(new Date), 100 milliseconds)
}
```

In the same manner we can construct an `Enumerator` that would fetch a url every some time using the `WS` api which returns, not suprisingly a `Future`

Combining this, callback Enumerator, with an imperative `Iteratee.foreach` we can println a stream of time values periodically:

```scala
val timeStream = Enumerator.generateM {
  Promise.timeout(Some(new Date), 100 milliseconds)
}

val printlnSink = Iteratee.foreach[Date](date => println(date))

timeStream |>> printlnSink
```

Another, more imperative, way of creating an `Enumerator` is by using `Concurrent.unicast` which once it is ready will give a `Channel` interface on which defined methods `push` and `end`:

```scala
val enumerator = Concurrent.unicast[String](onStart = channel => {
  channel.push("Hello")
  channel.push("World")
})

enumerator |>> Iteratee.foreach(println)
```

The `onStart` function will be called each time the `Enumerator` is applied to an `Iteratee`. In some applications, a chatroom for instance, it makes sense to assign the `enumerator` to a synchronized global value (using STMs for example) that will contain a list of listeners. `Concurrent.unicast` accepts two other functions, `onComplete` and `onError`.

One more interesting method is the `interleave` or `>-` method which as the name says, itrerleaves two Enumerators. For reactive `Enumerator`s Input will be passed as it happens from any of the interleaved `Enumerator`s

## Enumerators à la carte

Now that we have several interesting ways of creating `Enumerator`s, we can use these together with composition methods `andThen` / `>>>` and `interleave` / `>-` to compose `Enumerator`s on demand.

Indeed one interesting way of organizing a streamful application is by creating primitive `Enumerator`s and then composing a collection of them. Let’s imagine doing an application for monitoring systems:

```scala
object AvailableStreams {

  val cpu: Enumerator[JsValue] = Enumerator.generateM(/* code here */)

  val memory: Enumerator[JsValue] = Enumerator.generateM(/* code here */)

  val threads: Enumerator[JsValue] = Enumerator.generateM(/* code here */)

  val heap: Enumerator[JsValue] = Enumerator.generateM(/* code here */)

}

val physicalMachine = AvailableStreams.cpu >- AvailableStreams.memory
val jvm = AvailableStreams.threads >- AvailableStreams.heap

def usersWidgetsComposition(prefs: Preferences) = {
  // do the composition dynamically
}
```

Now, it is time to adapt and transform `Enumerator`s and `Iteratee`s using ... `Enumeratee`s!
