<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Handling data streams reactively

Progressive Stream Processing and manipulation is an important task in modern Web Programming, starting from chunked upload/download to Live Data Streams consumption, creation, composition and publishing through different technologies including Comet and WebSockets.

Iteratees provide a paradigm and an API allowing this manipulation, while focusing on several important aspects:

* Allowing the user to create, consume and transform streams of data.
* Treating different data sources in the same manner (Files on disk, Websockets, Chunked Http, Data Upload, ...).
* Composable: using a rich set of adapters and transformers to change the shape of the source or the consumer - construct your own or start with primitives.
* Being able to stop data being sent mid-way through, and being informed when source is done sending data.
* Non blocking, reactive and allowing control over resource consumption (Thread, Memory)

## Iteratees

An Iteratee is a consumer - it describes the way input will be consumed to produce some value. An Iteratee is a consumer that returns a value it computes after being fed enough input.

```scala
// an iteratee that consumes String chunks and produces an Int
Iteratee[String,Int]
```

The Iteratee interface `Iteratee[E,A]` takes two type parameters: `E`, representing the type of the Input it accepts, and `A`, the type of the calculated result.

An iteratee has one of three states: `Cont` meaning accepting more input, `Error` to indicate an error state, and `Done` which carries the calculated result. These three states are defined by the `fold` method of an `Iteratee[E,A]` interface:

```scala
def fold[B](folder: Step[E, A] => Future[B]): Future[B]
```

where the `Step` object has 3 states :

```scala
object Step {
  case class Done[+A, E](a: A, remaining: Input[E]) extends Step[E, A]
  case class Cont[E, +A](k: Input[E] => Iteratee[E, A]) extends Step[E, A]
  case class Error[E](msg: String, input: Input[E]) extends Step[E, Nothing]
}
```

The fold method defines an iteratee as one of the three mentioned states. It accepts three callback functions and will call the appropriate one depending on its state to eventually extract a required value. When calling `fold` on an iteratee you are basically saying:

- If the iteratee is in the state `Done`, then I'll take the calculated result of type `A` and what is left from the last consumed chunk of input `Input[E]` and eventually produce a `B`
- If the iteratee is in the state `Cont`, then I'll take the provided continuation (which is accepting an input) `Input[E] => Iteratee[E,A]` and eventually produce a `B`. Note that this state provides the only way to push input into the iteratee, and get a new iteratee state, using the provided continuation function. 
- If the iteratee is in the state `Error`, then I'll take the error message of type `String` and the input that caused it and eventually produce a B.

Depending on the state of the iteratee, `fold` will produce the appropriate `B` using the corresponding passed-in function.

To sum up, an iteratee consists of 3 states, and `fold` provides the means to do something useful with the state of the iteratee.

### Some important types in the `Iteratee` definition:

Before providing some concrete examples of iteratees, let's clarify two important types we mentioned above:

- `Input[E]` represents a chunk of input that can be either an `El[E]` containing some actual input, an `Empty` chunk or an `EOF` representing the end of the stream.
For example, `Input[String]` can be `El("Hello!")`, Empty, or EOF

- `Future[A]` represents, as its name indicates, a future value of type `A`. This means that it is initially empty and will eventually be filled in ("redeemed") with a value of type `A`, and you can schedule a callback, among other things you can do, if you are interested in that value. A Future is a very nice primitive for synchronization and composing async calls, and is explained further at the [[ScalaAsync]] section.

### Some primitive iteratees:

By implementing the iteratee, and more specifically its fold method, we can now create some primitive iteratees that we can use later on.

- An iteratee in the `Done` state producing a `1:Int` and returning `Empty` as the remaining value from the last `Input[String]`

```scala
val doneIteratee = new Iteratee[String,Int] {
  def fold[B](folder: Step[String,Int] => Future[B])(implicit ec: ExecutionContext) : Future[B] = 
    folder(Step.Done(1, Input.Empty))
}
```

As shown above, this is easily done by calling the appropriate `apply` function, in our case that of `Done`, with the necessary information.

To use this iteratee we will make use of the `Future` that holds a promised value.

```scala
def folder(step: Step[String,Int]):Future[Option[Int]] = step match {
  case Step.Done(a, e) => future(Some(a))
  case Step.Cont(k) => future(None)
  case Step.Error(msg,e) => future(None)
} 

val eventuallyMaybeResult: Future[Option[Int]] = doneIteratee.fold(folder)

eventuallyMaybeResult.onComplete(i => println(i))
```

of course to see what is inside the `Future` when it is redeemed we use `onComplete`

```scala
// will eventually print 1
eventuallyMaybeResult.onComplete(i => println(i))
```

There is already a built-in way allowing us to create an iteratee in the `Done` state by providing a result and input, generalizing what is implemented above:

```scala
val doneIteratee = Done[String,Int](1, Input.Empty)
```

Creating a `Done` iteratee is simple, and sometimes useful, but it does not consume any input. Let's create an iteratee that consumes one chunk and eventually returns it as the computed result:

```scala
val consumeOneInputAndEventuallyReturnIt = new Iteratee[String,Int] {
    
def fold[B](folder: Step[String,Int] => Future[B])(implicit ec: ExecutionContext): Future[B] = {
     folder(Step.Cont {
       case Input.EOF => Done(0, Input.EOF) //Assuming 0 for default value
       case Input.Empty => this
       case Input.El(e) => Done(e.toInt,Input.EOF) 
     })
  }
}

def folder(step: Step[String,Int]):Future[Int] = step match {
  case Step.Done(a, _) => future(a)
  case Step.Cont(k) => k(Input.EOF).fold({
    case Step.Done(a1, _) => Future.successful(a1)
    case _ => throw new Exception("Erroneous or diverging iteratee")
  })
  case _ => throw new Exception("Erroneous iteratee")
} 

```

As for `Done`, there is a built-in way to define an iteratee in the `Cont` state by providing a function that takes `Input[E]` and returns a state of `Iteratee[E,A]` :

```scala
val consumeOneInputAndEventuallyReturnIt = {
  Cont[String,Int](in => Done(100,Input.Empty))
}
```

In the same manner there is a built-in way to create an iteratee in the `Error` state by providing an error message and an `Input[E]`

Back to the `consumeOneInputAndEventuallyReturnIt`, it is possible to create a two-step simple iteratee manually, but it becomes harder and cumbersome to create any real-world iteratee capable of consuming a lot of chunks before, possibly conditionally, it eventually returns a result. Luckily there are some built-in methods to create common iteratee shapes in the `Iteratee` object.

### Folding input:

One common task when using iteratees is maintaining some state and altering it each time input is pushed. This type of iteratee can be easily created using the `Iteratee.fold` which has the signature:

```scala
def fold[E, A](state: A)(f: (A, E) => A): Iteratee[E, A]
```

Reading the signature one can realize that this fold takes an initial state `A`, a function that takes the state and an input chunk `(A, E) => A` and returns an `Iteratee[E,A]` capable of consuming `E`s and eventually returning an `A`. The created iteratee will return `Done` with the computed `A` when an input `EOF` is pushed.

One example would be creating an iteratee that counts the number of bytes pushed in:

```scala
val inputLength: Iteratee[Array[Byte],Int] = {
  Iteratee.fold[Array[Byte],Int](0) { (length, bytes) => length + bytes.size }
}
```
Another would be consuming all input and eventually returning it:

```scala
val consume: Iteratee[String,String] = {
  Iteratee.fold[String,String]("") { (result, chunk) => result ++ chunk }
}
```

There is actually already a method in the `Iteratee` object that does exactly this for any scala `TraversableLike`, called `consume`, so our example becomes:

```scala
val consume = Iteratee.consume[String]()
```

One common case is to create an iteratee that does some imperative operation for each chunk of input:

```scala
val printlnIteratee = Iteratee.foreach[String](s => println(s))
```

More interesting methods exist like `repeat`, `ignore`, and `fold1` - which is different from the preceding `fold` in that it gives one the opportunity to treat input chunks asychronously.

Of course one should be worried now about how hard it would be to manually push input into an iteratee by folding over iteratee states over and over again. Indeed each time one has to push input into an iteratee, one has to use the `fold` function to check on its state, if it is a `Cont` then push the input and get the new state, or otherwise return the computed result. That's when `Enumerator`s come in handy.
