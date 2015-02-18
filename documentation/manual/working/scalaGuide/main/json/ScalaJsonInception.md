<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# JSON Macro Inception

> Please note this documentation was initially published as an article by Pascal Voitot ([@mandubian](https://github.com/mandubian)) on [mandubian.com](http://mandubian.com/2012/11/11/JSON-inception/)


> **This feature is still experimental because Scala Macros are still experimental in Scala 2.10.0. If you prefer not using an experimental feature from Scala, please use hand-written Reads/Writes/Format which are strictly equivalent.**

## <a name="wtf-inception-boring">Writing a default case class Reads/Writes/Format is so boring!</a>

Remember how you write a `Reads[T]` for a case class.

```
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Person(name: String, age: Int, lovesChocolate: Boolean)

implicit val personReads = (
  (__ \ 'name).read[String] and
  (__ \ 'age).read[Int] and
  (__ \ 'lovesChocolate).read[Boolean]
)(Person)
```

So you write 4 lines for this case class.  
You know what?  
We have had a few complaints from some people who think it's not cool to write a `Reads[TheirClass]` because usually Java JSON frameworks like Jackson or Gson do it behind the curtain without writing anything.  
We argued that Play2.1 JSON serializers/deserializers are:

- completely typesafe, 
- fully compiled,
- nothing was performed using introspection/reflection at runtime.  

But for some, this didn’t justify the extra lines of code for case classes.

We believe this is a really good approach so we persisted and proposed:

- JSON simplified syntax
- JSON combinators
- JSON transformers

Added power, but nothing changed for the additional 4 lines.

## <a name="wtf-inception-minimalist">Let's be minimalist</a>
As we are perfectionist, now we propose a new way of writing the same code:

```
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Person(name: String, age: Int, lovesChocolate: Boolean)

implicit val personReads = Json.reads[Person]
```

1 line only.  
Questions you may ask immediately:

> Does it use runtime bytecode enhancement? -> NO

> Does it use runtime introspection? -> NO

> Does it break type-safety? -> NO

**So what?**

> After creating buzzword **JSON coast-to-coast design**, let's call it **JSON INCEPTION**.

<br/>
<br/>
# <a name="json-incept">JSON Inception</a>

## <a name="json-incept-eq">Code Equivalence</a>
As explained just before:

```
import play.api.libs.json._
// please note we don't import functional.syntax._ as it is managed by the macro itself

implicit val personReads = Json.reads[Person]

// IS STRICTLY EQUIVALENT TO writing

implicit val personReads = (
  (__ \ 'name).read[String] and
  (__ \ 'age).read[Int] and
  (__ \ 'lovesChocolate).read[Boolean]
)(Person)
```	

## <a name="json-incept">Inception equation</a>

Here is the equation describing the windy _Inception_ concept:

```
(Case Class INSPECTION) + (Code INJECTION) + (COMPILE Time) = INCEPTION
```

<br/>
####Case Class Inspection
As you may deduce by yourself, in order to ensure preceding code equivalence, we need :

- to inspect `Person` case class, 
- to extract the 3 fields `name`, `age`, `lovesChocolate` and their types,
- to resolve typeclasses implicits,
- to find `Person.apply`.


<br/>
####INJECTION?
No I stop you immediately…  

>**Code injection is not dependency injection…**  
>No Spring behind inception… No IOC, No DI… No No No ;)  
  
I used this term on purpose because I know that injection is now linked immediately to IOC and Spring. But I'd like to re-establish this word with its real meaning.  
Here code injection just means that **we inject code at compile-time into the compiled scala AST** (Abstract Syntax Tree).

So `Json.reads[Person]` is compiled and replaced in the compile AST by:

```
(
  (__ \ 'name).read[String] and
  (__ \ 'age).read[Int] and
  (__ \ 'lovesChocolate).read[Boolean]
)(Person)
```

Nothing less, nothing more…

<br/>
####COMPILE-TIME
Yes everything is performed at compile-time.  
No runtime bytecode enhancement.  
No runtime introspection.  

> As everything is resolved at compile-time, you will have a compile error if you did not import the required implicits for all the types of the fields.

<br/>
# <a name="scala-macros">Json inception is Scala 2.10 Macros</a>

We needed a Scala feature enabling:

- compile-time code enhancement
- compile-time class/implicits inspection
- compile-time code injection

This is enabled by a new experimental feature introduced in Scala 2.10: [Scala Macros](http://scalamacros.org/)  

Scala macros is a new feature (still experimental) with a huge potential. You can :

- introspect code at compile-time based on Scala reflection API, 
- access all imports, implicits in the current compile context
- create new code expressions, generate compiling errors and inject them into compile chain.

Please note that:

- **We use Scala Macros because it corresponds exactly to our requirements.**
- **We use Scala macros as an enabler, not as an end in itself.**
- **The macro is a helper that generates the code you could write by yourself.**
- **It doesn't add, hide unexpected code behind the curtain.**
- **We follow the *no-surprise* principle**

As you may discover, writing a macro is not a trivial process since your macro code executes in the compiler runtime (or universe).  

    So you write macro code 
      that is compiled and executed 
      in a runtime that manipulates your code 
         to be compiled and executed 
         in a future runtime…           
**That's also certainly why I called it *Inception* ;)**

So it requires some mental exercises to follow exactly what you do. The API is also quite complex and not fully documented yet. Therefore, you must persevere when you begin using macros.

I'll certainly write other articles about Scala macros because there are lots of things to say.  
This article is also meant **to begin the reflection about the right way to use Scala Macros**.  
Great power means greater responsability so it's better to discuss all together and establish a few good manners…

<br/>
# <a name="writes-format">Writes[T] & Format[T]</a>

>Please remark that JSON inception just works for structures having `unapply/apply` functions with corresponding input/output types.

Naturally, you can also _incept_ `Writes[T]` and `Format[T]`.

## <a name="writes">Writes[T]</a>

```
import play.api.libs.json._
 
implicit val personWrites = Json.writes[Person]
```

## <a name="format">Format[T]</a>

```
import play.api.libs.json._

implicit val personWrites = Json.format[Person]
```

## <a name="cando">Special patterns</a>

- **You can define your Reads/Writes in your companion object**
This is useful because then the implicit Reads/Writes is implicitly infered as soon as you manipulate an instance of your class.

```
import play.api.libs.json._

case class Person(name: String, age: Int)

object Person{
  implicit val personFmt = Json.format[Person]
}
```

- **You can now define Reads/Writes for single-field case class** (known limitation until 2.1-RC2)

```
import play.api.libs.json._

case class Person(names: List[String])

object Person{
  implicit val personFmt = Json.format[Person]
}
```


## <a name="limitations">Known limitations</a>

- **Don't override apply function in companion object** because then the Macro will have several apply functions and won't choose.
- **Json Macros only work when apply and  unapply have corresponding input/output types**: This is naturally the case for case classes. But if you want to the same with a trait, you must implement the same apply/unapply you would have in a case class.
- **Json Macros are known to accept Option/Seq/List/Set & Map[String, _]**. For other generic types, test and if not working, use traditional way of writing Reads/Writes manually.

