# Handling data streams reactively

Progressive Stream Processing and manipulation is an important task in modern Web Programming, starting from chunked upload/download to Live Data Streams consumption, creation, composition and publishing through different technologies including Comet and WebSockets.

Iteratees provide a paradigm and an api allowing this manipulation while focusing on several important aspects:

* Allowing user to create, consume and transform streams of data.
* Treating different data sources in the same manner (Files on disk, Websockets, Chunked Http, Data Upload, ...).
* Composable: use a rich set of adapters and transformers to change the shape of the source or the consumer; construct your own or start with primitives.
* Having control over when it is enough data sent and be informed when source is done sending data.
* Non blocking, reactive and allowing control over resource consumption (Thread, Memory)

## Iteratees

An Iteratee is a consumer, it describes the way input will be consumed to produce some value. Iteratee is a consumer that returns a value it computes after being fed enough input.

```scala
// an iteratee that consumes chunkes of String and produces an Int
Iteratee[String,Int] 
```

The Iteratee interface `Iteratee[E,A]` takes two type parameters, `E` representing the type of the Input it accepts and `A` the type of the calculated result.

An iteratee has one of three states, `Cont` meaning accepting more input, `Error` to indicate an error state and `Done` which carries the calculated result. These three states are defined by the `fold` method of an `Iteratee[E,A]` interface:

```scala
def fold[B](
  done: (A, Input[E]) => Promise[B],
  cont: (Input[E] => Iteratee[E, A]) => Promise[B],
  error: (String, Input[E]) => Promise[B]
): Promise[B]
```

The fold method defines an iteratee as one of the three mentioned states. It accepts three callback functions and will call the appropriate one depending on its state to eventually extract a required value. When calling `fold` on an iteratee you are basically saying:

- If the iteratee is the state `Done`, then I'd take the calculated result of type `A` and what is left from the last consumed chunk of input `Input[E]` and eventually produce a `B`
- If the iteratee is the state `Cont`, then I'd take the provided continuation (which is accepting an input) `Input[E] => Iteratee[E,A]` and eventually produce a `B`. Note that this state provides the only way to push input into the iteratee, and get a new iteratee state, using the provided continuation function. 
- If the iteratee is the state `Error`, then I'd take the error message of type `String` and the input that caused it and eventually produce a B.

Obviously, depending on the state of the iteratee, `fold` will produce the appropriate `B` using the corresponding passed function.

To sum up, iteratee consists of 3 states, and `fold` provides the means to do something useful with the state of the iteratee.

### Some important types in the `Iteratee` definition:

Before providing some concrete examples of iteratees, let's clarify two important types we mentioned above:

- `Input[E]` represents a chunk of input that can be either an `El[E]` containing some actual input, an `Empty` chunk or an `EOF` representing the end of the stream.
For example, `Input[String]` can be `El("Hello!")`, Empty, or EOF

- `Promise[A]` represents, as its name tells, a promise of value of type `A`. This means that it will eventually be redeemed with a value of type `A` and you can schedule a callback, among other things you can do, if you are interested in that value. A promise is a very nice primitive for synchronization and composing async calls, and is explained further at the [[PromiseScala | ScalaAsync]] section.

### Some primitive iteratees:

By implementing the iteratee, and more specifically its fold method, we can now create some primitive iteratees that we can use later on.

- An iteratee in the `Done` state producing an `1:Int` and returning `Empty` as left from last `Input[String]`

```scala
val doneIteratee = new Iteratee[String,Int] {
  def fold[B](
    done: (A, Input[E]) => Promise[B],
    cont: (Input[E] => Iteratee[E, A]) => Promise[B],
    error: (String, Input[E]) => Promise[B]): Promise[B] = done(1,Input.Empty)
}
```

As shown above, this is easily done by calling the appropriate callback function, in our case `done`, with the necessary information.

To use this iteratee we will make use of the `Promise.pure` that is a promise already in the Redeemed state.

```scala
val eventuallyMaybeResult: Promise[Option[Int]] = {
  doneIteratee.fold(
  
    // if done return the computed result
    (a,in) => Promise.pure(Some(a)),

    //if continue return None
    k => Promise.pure(None),

    //on error return None
    (msg,in) => Promise.pure(None) 
  ) 
}
```

of course to see what is inside the `Promise` when it is redeemed we use `onRedeem`

```scala
// will eventually print 1
eventuallyMaybeResult.onRedeem(i => println(i)) 
```

There is already a built-in way allowing us to create an iteratee in the `Done` state by providing a result and input, generalizing what is implemented above:

```scala
val doneIteratee = Done[Int,String](1, Input.Empty)
```

Creating a `Done` iteratee is simple, and sometimes useful, but it obviously does not consume any input. Let's create an iteratee that consumes one chunk and eventually returns it as the computed result:

```scala
val consumeOneInputAndEventuallyReturnIt = new Iteratee[String,Int] {
    
  def fold[B](
    done: (Int, Input[String]) => Promise[B],
    cont: (Input[String] => Iteratee[String, Int]) => Promise[B],
    error: (String, Input[String]) => Promise[B]
  ): Promise[B] = {
        
    cont(in => Done(in, Input.Empty))
      
  }
  
}
```

As for `Done` there is a built-in way to define an iteratee in the `Cont` state by providing a function that takes `Input[E]` and returns a state of `Iteratee[E,A]` :

```scala
val consumeOneInputAndEventuallyReturnIt = {
  Cont[String,Int](in => Done(in,Input.Empty))
}
```

In the same manner there is a built-in way to create an iteratee in the `Error` state by providing and error message and an `Input[E]`

Back to the `consumeOneInputAndEventuallyReturnIt`, it is possible to create a two step simple iteratee manually but it becomes harder and cumbersome to create any real world iteratee capable of consuming a lot of chunks before, possibly conditionally, it eventually returns a result. Luckily there are some built-in methods to create common iteratee shapes in the `Iteratee` object.

### Folding input:

One common task when using iteratees is maintaining some state and altering it each time input is pushed. This type of iteratee can be easily created using the `Iteratee.fold` which has the signature:

```scala
def fold[E, A](state: A)(f: (A, E) => A): Iteratee[E, A]
```

Reading the signature one can realize that this fold takes an initial state `A`, a function that takes the state and an input chunk `(A, E) => A` and returning an `Iteratee[E,A]` capable of consuming `E`s and eventually returning an `A`. The created iteratee will return `Done` with the computed `A` when an input `EOF` is pushed.

One example can be creating an iteratee that counts the number of bytes pushed in:

```scala
val inputLength: Iteratee[Array[Byte],A] = {
  Iteratee.fold[Array[Byte],Int](0) { (length, bytes) => length + bytes.size }
}
```
Another could be consuming all input and eventually returning it:

```scala
val consume: Iteratee[String,String] = {
  Iteratee.fold[String,String]("") { (result, chunk) => result ++ chunk }
}
```

There is actually already a method in `Iteratee` object that does exactly this for any scala `TraversableLike` called `consume`, so our example becomes:

```scala
val consume = Iteratee.consume[String]()
```

One common case is to create an iteratee that does some imperative operation for each chunk of input:

```scala
val printlnIteratee = Iteratee.foreach[String](s => println(s))
```

More interesting methods exist like `repeat`, `ignore` and `fold1` which is different from the preceeding `fold` in offering the opportunity to be asynchronous in treating input chunks.

Of course one should be worried now about how hard would it be to manually push input into an iteratee by folding over iteratee states over and over again. Indeed each time one has to push input into an iteratee, one has to use the `fold` function to check on its state, if it is a `Cont` then push the input and get the new state or otherwise return the computed result. That's when `Enumerator`s come in handy.

> **Next:** [[Enumerators | Enumerators]]