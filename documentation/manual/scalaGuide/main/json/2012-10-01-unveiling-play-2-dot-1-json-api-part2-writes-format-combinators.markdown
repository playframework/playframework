---
layout: post
title: "Unveiling Play 2.1 Json API - Part 2 : Writes/Format combinators"
date: 2012-10-01 11:10
comments: true
external-url: 
categories: [playframework,play2.1,json,serialization,validation,combinators,applicative]
keywords: play 2.1, json, json serialization, json parsing, json validation, json cumulative errors, play framework, combinators, applicative
---

>In [Part 1 article](/2012/09/08/unveiling-play-2-dot-1-json-api-part1-jspath-reads-combinators/), we gave a quick overview of new Play2.1 JSON API features around JsPath and Reads combinators. It's funny to be able to read JSON to Scala structures but it's better to be able to **write Scala structures to JSON (`Writes[T]`)** and it's even better to **mix Reads and Writes (`Format[T]`)** sometimes.

**Now let's focus on Writes and Format in the details ;)**
<br/>
<br/>

# <a name="writes">Writes[T] hasn't changed (except combinators)</a>

## <a name="writes-2_0">Writes in Play2.0.x</a>

Do you remember how you had to write a Json `Writes[T]` in `Play2.0.x` ?  
You had to override the `writes` function.  

{% codeblock lang:scala %}
trait Writes[-A] {
  self =>
  /**
   * Convert the object into a JsValue
   */
  def writes(o: A): JsValue
}
{% endcodeblock %}	

Take the same simple case class we used in Part 1:
{% codeblock lang:scala %}
case class Creature(
  name: String, 
  isDead: Boolean, 
  weight: Float
)
{% endcodeblock %}	

In `Play2.0.x`, you would write your `Writes[Creature]` as following (using new Json syntax to re-show it even if it didn't exist in Play2.0.x ;) ):
{% codeblock lang:scala %}
import play.api.libs.json._

implicit val creatureWrites = new Writes[Creature] {
  def writes(c: Creature): JsValue = {
    Json.obj(
    	"name" -> c.name,
    	"isDead" -> c.isDead,
    	"weight" -> c.weight
    )
  }
}

scala> val gizmo = Creature("gremlins", false, 1.0F)
scala> val gizmojs = Json.toJson(gizmo)
gizmojs: play.api.libs.json.JsValue = {"name":"gremlins","isDead":false,"weight":1.0}

{% endcodeblock %}	

## <a name="writes-2_1">Writes in Play2.1.x</a>

No suspense to be kept: **in Play2.1, you write Writes exactly in the same way** :D

So what's the difference?  
As presented in Part 1, `Reads` could be combined using simple logical operators.  
Using functional Scala power, we were able to **provide combinators for `Writes[T]`**.

> If you want more theoretical aspects about the way it was implemented based on generic functional structures adapted to our needs, you can read this post ["Applicatives are too restrictive, breaking Applicatives and introducing Functional Builders"](http://sadache.tumblr.com/post/30955704987/applicatives-are-too-restrictive-breaking-applicativesfrom) written by [@sadache](http://www.github.com/sadache)  

## <a name="writes-combined">Writes main change: *combinators*</a>

Once again, code first: re-writing previous `Writes[T]` using combinators.

{% codeblock lang:scala %}
// IMPORTANT import this to have the required tools in your scope
import play.api.libs.json._
// imports required functional generic structures
import play.api.libs.json.util._
// imports implicit structure for Writes only (always try to import only what you need)
import play.api.libs.json.Writes._

implicit val creatureWrites = (
  (__ \ "name").write[String] and
  (__ \ "isDead").write[Boolean] and
  (__ \ "weight").write[Float]
)(unlift(Creature.unapply))

// or using the operators inspired by Scala parser combinators for those who know them
implicit val creatureWrites = (
  (__ \ "name").write[String] ~
  (__ \ "isDead").write[Boolean] ~
  (__ \ "weight").write[Float]
)(unlift(Creature.unapply))

scala> val c = Creature("gremlins", false, 1.0F)
scala> val js = Json.toJson(c)
js: play.api.libs.json.JsValue = {"name":"gremlins","isDead":false,"weight":1.0}

{% endcodeblock %}	

It looks exactly like `Reads[T]` except a few things, isn't it?  
Let's explain a bit (by copying Reads article changing just a few things… I'm lazy ;)):

#####`import play.api.libs.json.Writes._` 
It imports only the required stuff for `Writes[T]` without interfering with other imports.
<br/>
#####`(__ \ "name").write[String]`
You apply `write[String]` on this JsPath (exactly the same as `Reads`)
<br/>
#####`and` is just an operator meaning `Writes[A] and Writes[B] => Builder[Writes[A ~ B]]`
  - `A ~ B` just means `Combine A and B` but it doesn't suppose the way it is combined (can be a tuple, an object, whatever…)
  - `Builder` is not a real type but I introduce it just to tell that the operator `and` doesn't create directly a `Writes[A ~ B]` but an intermediate structure that is able to build a `Writes[A ~ B]` or to combine with another `Writes[C]`
<br/>   
#####`(…)(unlift(Creature.unapply))` builds a `Writes[Creature]`
  - `(__ \ "name").write[String] and (__ \ "isDead").write[Boolean] and (__ \ "weight").write[Float]` builds a `Builder[Writes[String ~ Boolean ~ Float])]` but you want a `Writes[Creature]`.  
  - So you apply the `Builder[Writes[String ~ Boolean ~ String])]` to a function `Creature => (String, Boolean, Float)` to finally obtain a `Writes[Creature]`. Please note that it may seem a bit strange to provide `Creature => (String, Boolean, Float)` to obtain a `Writes[Creature]` from a `Builder[Writes[String ~ Boolean ~ String])]` but it's due to the contravariant nature of `Writes[-T]`.
  - We have `Creature.unapply` but its signature is `Creature => Option[(String, Boolean, Float)]` so we `unlift` it to obtain `Creature => (String, Boolean, Float)`.
<br/>

>The only thing you have to keep in mind is this `unlift` call which might not be natural at first sight!
  
As you can deduce by yourself, the `Writes[T]` is far easier than the `Reads[T]` case because when writing, it doesn't try to validate so there is no error management at all.  

Moreover, due to this, you have to keep in mind that operators provided for `Writes[T]` are not as rich as for `Reads[T]`. Do you remind `keepAnd` and `andKeep` operators?  They don't have any meaning for `Writes[T]`. When writing `A~B`, you write `A and B` but not `only A or only B`. So `and` is the only operators provided for `Writes[T]`.
 

### Complexifying the case

Let's go back to our more complex sample used in end of Part1.
Remember that we had imagined that our creature was modelled as following:

{% codeblock lang:scala %}
case class Creature(
  name: String, 
  isDead: Boolean, 
  weight: Float,
  email: String, // email format and minLength(5)
  favorites: (String, Int), // the stupid favorites
  friends: List[Creature] = Nil, // yes by default it has no friend
  social: Option[String] = None // by default, it's not social
)
{% endcodeblock %}	
Let's write corresponding `Writes[Creature]`

{% codeblock lang:scala %}
// IMPORTANT import this to have the required tools in your scope
import play.api.libs.json._
// imports required functional generic structures
import play.api.libs.json.util._
// imports implicit structure for Writes only (always try to import only what you need)
import play.api.libs.json.Writes._

implicit val creatureWrites: Writes[Creature] = (
  (__ \ "name").write[String] and
  (__ \ "isDead").write[Boolean] and
  (__ \ "weight").write[Float] and
  (__ \ "email").write[String] and
  (__ \ "favorites").write( 
  	(__ \ "string").write[String] and
  	(__ \ "number").write[Int]
  	tupled
  ) and
  (__ \ "friends").lazyWrite(Writes.traversableWrites[Creature](creatureWrites)) and
  (__ \ "social").write[Option[String]]
)(unlift(Creature.unapply))  

val gizmo = Creature("gremlins", false, 1.0F, "gizmo@midnight.com", ("alpha", 85), List(), Some("@gizmo"))
val gizmojs = Json.toJson(gizmo)
gizmojs: play.api.libs.json.JsValue = {"name":"gremlins","isDead":false,"weight":1.0,"email":"gizmo@midnight.com","favorites":{"string":"alpha","number":85},"friends":[],"social":"@gizmo"}

val zombie = Creature("zombie", true, 100.0F, "shaun@dead.com", ("ain", 2), List(gizmo), None)
val zombiejs = Json.toJson(zombie)
zombiejs: play.api.libs.json.JsValue = {"name":"zombie","isDead":true,"weight":100.0,"email":"shaun@dead.com","favorites":{"string":"ain","number":2},"friends":[{"name":"gremlins","isDead":false,"weight":1.0,"email":"gizmo@midnight.com","favorites":{"string":"alpha","number":85},"friends":[],"social":"@gizmo"}],"social":null

{% endcodeblock %}

You can see that it's quite straightforward. it's far easier than `Reads[T]` as there are no special operator.
Here are the few things to explain:

##### `(__ \ "favorites").write(…)`

    (__ \ "string").write[String] and
  	(__ \ "number").write[Int]
  	tupled

- Remember that `(__ \ "string").write[String](…) and (__ \ "number").write[Int](…) => Builder[Writes[String ~ Int]]`  
- What means `tupled` ? as for `Reads[T]`, it _"tuplizes"_ your Builder: `Builder[Writes[A ~ B]].tupled => Writes[(A, B)]` 
<br/>
##### `(__ \ "friend").lazyWrite(Writes.traversableWrites[Creature](creatureWrites))`

It's the symmetric code for `lazyRead` to treat recursive field on `Creature` class itself:

- `Writes.traversableWrites[Creature](creatureWrites)` creates a `Writes[Traversable[Creature]]` passing the `Writes[Creature]` itself for recursion (please note that a `list[Creature]`should appear very soon ;))
- `(__ \ "friends").lazyWrite[A](r: => Writes[A]))` : `lazyWrite` expects a `Writes[A]` value _passed by name_ to allow the type recursive construction. This is the only refinement that you must keep in mind in this very special recursive case.

> FYI, you may wonder why `Writes.traversableWrites[Creature]: Writes[Traversable[Creature]]` can replace `Writes[List[Creature]]`?  
> This is because `Writes[-T]` is contravariant meaning: if you can write a `Traversable[Creature]`, you can write a `List[Creature]` as `List` inherits `Traversable` (relation of inheritance is reverted by contravariance).

## <a name="format">What about combinators for Format?</a>

Remember in Play2.1, there was a feature called `Format[T] extends Reads[T] with Writes[T]`.  
It mixed `Reads[T]` and `Writes[T]` together to provide serialization/deserialization at the same place.

Play2.1 provide combinators for `Reads[T]` and `Writes[T]`. What about combinators for `Format[T]` ?

Let's go back to our very simple sample:

{% codeblock lang:scala %}
case class Creature(
  name: String, 
  isDead: Boolean, 
  weight: Float
)
{% endcodeblock %}	

Here is how you write the `Reads[Creature]`:
{% codeblock lang:scala %}
import play.api.libs.json.util._
import play.api.libs.json.Reads._

val creatureReads = (
  (__ \ "name").read[String] and
  (__ \ "isDead").read[Boolean] and
  (__ \ "weight").read[Float]
)(Creature)  
{% endcodeblock %}	

>Please remark that I didn't use `implicit` so that there is no implicit `Reads[Creature]` in the context when I'll define `Format[T]`

Here is how you write the `Writes[Creature]`:
{% codeblock lang:scala %}
import play.api.libs.json.util._
import play.api.libs.json.Writes._

val creatureWrites = (
  (__ \ "name").write[String] and
  (__ \ "isDead").write[Boolean] and
  (__ \ "weight").write[Float]
)(unlift(Creature.unapply))  
{% endcodeblock %}	

###How to gather both Reads/Writes to create a `Format[Creature]`?

#### <a name="format-1">1st way = create from existing reads/writes</a>

You can reuse existing `Reads[T]` and `Writes[T]` to create a `Format[T]` as following:

{% codeblock lang:scala %}
implicit val creatureFormat = Format(creatureReads, creatureWrites)

val gizmojs = Json.obj( 
  "name" -> "gremlins", 
  "isDead" -> false, 
  "weight" -> 1.0F
)

val gizmo = Creature("gremlins", false, 1.0F)

assert(Json.fromJson[Creature](gizmojs).get == gizmo)
assert(Json.toJson(gizmo) == gizmojs)

{% endcodeblock %}	

#### <a name="format-2">2nd way = create using combinators</a>

We have Reads and Writes combinators, isn't it?  
Play2.1 also provides **Format Combinators** due to the magic of functional programming (actually it's not magic, it's just pure functional programming;) )

As usual, code 1st:

{% codeblock lang:scala %}
import play.api.libs.json.util._
import play.api.libs.json.Format._

implicit val creatureFormat = (
  (__ \ "name").format[String] and
  (__ \ "isDead").format[Boolean] and
  (__ \ "weight").format[Float]
)(Creature.apply, unlift(Creature.unapply))  

val gizmojs = Json.obj( 
  "name" -> "gremlins", 
  "isDead" -> false, 
  "weight" -> 1.0F
)

val gizmo = Creature("gremlins", false, 1.0F)

assert(Json.fromJson[Creature](gizmojs).get == gizmo)
assert(Json.toJson(gizmo) == gizmojs)
{% endcodeblock %}	

Nothing too strange…

#####`(__ \ "name").format[String]`
It creates a format[String] reading/writing at the given `JsPath`
<br/>

#####`( )(Creature.apply, unlift(Creature.unapply))`
To map to a Scala structure:

- `Reads[Creature]` requires a function `(String, Boolean, Float) => Creature`
- `Writes[Creature]` requires a function `Creature => (String, Boolean, Float)`

So as `Format[Creature] extends Reads[Creature] with Writes[Creature]` we provide `Creature.apply` and `unlift(Creature.unapply)` and that's all folks...

## <a name="format">More complex case</a>

The previous sample is a bit dumb because the structure is really simple and because reading/writing is symmetric. We have:

{% codeblock lang:scala %}
Json.fromJson[Creature](Json.toJson(creature)) == creature
{% endcodeblock %}	

In this case, you read what you write and vis versa. So you can use the very simple `JsPath.format[T]` functions which build both `Reads[T]` and `Writes[T]` together.  

But if we take our usual more complicated case class, how to write the `Format[T]`?

Remind the code:

{% codeblock lang:scala %}
import play.api.libs.json._
import play.api.libs.json.util._

// The case class
case class Creature(
  name: String, 
  isDead: Boolean, 
  weight: Float,
  email: String, // email format and minLength(5)
  favorites: (String, Int), // the stupid favorites
  friends: List[Creature] = Nil, // yes by default it has no friend
  social: Option[String] = None // by default, it's not social
)

import play.api.data.validation.ValidationError
import play.api.libs.json.Reads._

// defines a custom reads to be reused
// a reads that verifies your value is not equal to a give value
def notEqualReads[T](v: T)(implicit r: Reads[T]): Reads[T] = Reads.filterNot(ValidationError("validate.error.unexpected.value", v))( _ == v )

def skipReads(implicit r: Reads[String]): Reads[String] = r.map( _.substring(2) )

val creatureReads: Reads[Creature] = (
  (__ \ "name").read[String] and
  (__ \ "isDead").read[Boolean] and
  (__ \ "weight").read[Float] and
  (__ \ "email").read(email keepAnd minLength[String](5)) and
  (__ \ "favorites").read( 
  	(__ \ "string").read[String]( notEqualReads("ni") andKeep skipReads ) and
  	(__ \ "number").read[Int]( max(86) or min(875) )
  	tupled
  ) and
  (__ \ "friends").lazyRead( list[Creature](creatureReads) ) and
  (__ \ "social").read(optional[String])
)(Creature)  

import play.api.libs.json.Writes._

val creatureWrites: Writes[Creature] = (
  (__ \ "name").write[String] and
  (__ \ "isDead").write[Boolean] and
  (__ \ "weight").write[Float] and
  (__ \ "email").write[String] and
  (__ \ "favorites").write( 
  	(__ \ "string").write[String] and
  	(__ \ "number").write[Int]
  	tupled
  ) and
  (__ \ "friends").lazyWrite(Writes.traversableWrites[Creature](creatureWrites)) and
  (__ \ "social").write[Option[String]]
)(unlift(Creature.unapply))

{% endcodeblock %}	

As you can see, `creatureReads` and `creatureWrites` are not exactly symmetric and couldn't be merged in one single `Format[Creature]` as done previously.

{% codeblock lang:scala %}
Json.fromJson[Creature](Json.toJson(creature)) != creature
{% endcodeblock %}	

Hopefully, as done previously, we can build a `Format[T]` from a `Reads[T]` and a `Writes[T]`.

{% codeblock lang:scala %}
implicit val creatureFormat: Format[Creature] = Format(creatureReads, creatureWrites)

// Testing Serialization of Creature to Json
val gizmo = Creature("gremlins", false, 1.0F, "gizmo@midnight.com", ("alpha", 85), List(), Some("@gizmo"))
val zombie = Creature("zombie", true, 100.0F, "shaun@dead.com", ("ain", 2), List(gizmo), None)

val zombiejs = Json.obj(
	"name" -> "zombie",
	"isDead" -> true,
	"weight" -> 100.0,
	"email" -> "shaun@dead.com",
	"favorites" -> Json.obj(
		"string" -> "ain",
		"number" -> 2
	),
	"friends" -> Json.arr(
		Json.obj(
			"name" -> "gremlins",
			"isDead" -> false,
			"weight" -> 1.0,
			"email" -> "gizmo@midnight.com",
			"favorites" -> Json.obj(
				"string" -> "alpha",
				"number" -> 85
			),
			"friends" -> Json.arr(),
			"social" -> "@gizmo"
		)
	),
	"social" -> JsNull
)

assert(Json.toJson(zombie) == zombiejs)

// Testing Deserialization of JSON to Creature (note the dissymetric reading)
val gizmo2 = Creature("gremlins", false, 1.0F, "gizmo@midnight.com", ("pha", 85), List(), Some("@gizmo"))
val zombie2 = Creature("zombie", true, 100.0F, "shaun@dead.com", ("n", 2), List(gizmo2), None)

assert(Json.fromJson[Creature](zombiejs).get == zombie2)

{% endcodeblock %}	

<br/>
<br/>
## <a name="conclusion">Conclusion & much more…</a>

So here is the end of this 2nd part.

> Json combinator is cool!  
> Now what about transforming Json into Json???  
> Why would you need this?  
> Hmmm: why not receive JSON, validate/transform it, send it to a managing Json (Mongo for ex or others), read it again from the DB, modify it a bit and send it out…  
> Json "coast to coast" without any model class…

Let's tease a bit: 

{% codeblock lang:scala %}
val jsonTransform = (
  (__ \ "key1").json.pickBranch and
  (__ \ "key2").json.pickBranch(
    (__ \ "key22").json.put( (__ \ "key22").json.pick.map( js => js \ "key221" ) ) and 
    (__ \ "key23").json.put( (__ \ "key23").json.pick.map{ case JsString(s) => JsString(s + "123") } )
    reduce
  )
) reduce

val js = Json.obj(
	"key1" -> "alpha",
	"key2" -> Json.obj("key22" -> Json.obj("key221" -> "beta"), "key23" -> "gamma"),
)

assert(js.validate(jsonTransform), JsSuccess( Json.obj("key1" -> "alpha", "key2" -> Json.obj( "key22" -> "beta", "key23" -> "gamma123" ) ) ) )
{% endcodeblock %}	

> Next parts about Json generators/transformers ;)

Have fun... 