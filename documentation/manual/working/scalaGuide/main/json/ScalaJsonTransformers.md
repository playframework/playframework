<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# <a name="json-to-json">JSON transformers</a>

> Please note this documentation was initially published as an article by Pascal Voitot ([@mandubian](https://github.com/mandubian)) on [mandubian.com](http://mandubian.com/2012/10/29/unveiling-play-2-dot-1-json-api-part3-json-transformers/)

Now you should know how to validate JSON and convert into any structure you can write in Scala and back to JSON. But as soon as I've begun to use those combinators to write web applications, I almost immediately encountered a case : read JSON from network, validate it and convert it into… JSON. 


## <a name="json-to-json">Introducing JSON _coast-to-coast_ design</a>

### <a name="doomed-to-OO">Are we doomed to convert JSON to OO?</a>

For a few years now, in almost all web frameworks (except recent JS serverside stuff maybe in which JSON is the default data structure), we have been used to get JSON from network and **convert JSON (or even POST/GET data) into OO structures** such as classes (or case classes in Scala). Why?  

- for a good reason : **OO structures are "language-native"** and allows **manipulating data with respect to your business logic** in a seamless way while ensuring isolation of business logic from web layers.
- for a more questionable reason : **ORM frameworks talk to DB only with OO structures** and we have (kind of) convinced ourselves that it was impossible to do else… with the well-known good & bad features of ORMs… (not here to criticize those stuff)


### <a name="is-default-case">Is OO conversion really the default usecase?</a>

**In many cases, you don't really need to perform any real business logic with data but validating/transforming before storing or after extracting.**  

Let's take the CRUD case: 

- You just get the data from the network, validate them a bit and insert/update into DB. 
- In the other way, you just retrieve data from DB and send them outside.  

So, generally, for CRUD ops, you convert JSON into a OO structure just because the frameworks are only able to speak OO.

>**I don't say or pretend you shouldn't use JSON to OO conversion but maybe this is not the most common case and we should keep conversion to OO only when we have real business logic to fulfill.**


### <a name="new-players">New tech players change the way of manipulating JSON</a>

Besides this fact, we have some new DB types such as Mongo (or CouchDB) accepting document structured data looking almost like JSON trees (_isn't BSON, Binary JSON?_).  
With these DB types, we also have new great tools such as [ReactiveMongo](http://www.reactivemongo.org) which provides reactive environment to stream data to and from Mongo in a very natural way.  
I've been working with Stephane Godbillon to integrate ReactiveMongo with Play2.1 while writing the [Play2-ReactiveMongo module](https://github.com/zenexity/Play-ReactiveMongo). Besides Mongo facilities for Play2.1, this module provides _Json To/From BSON conversion typeclasses_.  

> **So it means you can manipulate JSON flows to and from DB directly without even converting into OO.**

### <a name="new-players">JSON _coast-to-coast_ design</a>

Taking this into account, we can easily imagine the following: 

- receive JSON,
- validate JSON,
- transform JSON to fit expected DB document structure,
- directly send JSON to DB (or somewhere else)

This is exactly the same case when serving data from DB:

- extract some data from DB as JSON directly,
- filter/transform this JSON to send only mandatory data in the format expected by the client (for ex, you don't want some secure info to go out),
- directly send JSON to the client

In this context, we can easily imagine **manipulating a flow of JSON data** from client to DB and back without any (explicit) transformation in anything else than JSON.  
Naturally, when you plug this transformation flow on **reactive infrastructure provided by Play2.1**, it suddenly opens new horizons.  

> This is the so-called (by me) **JSON coast-to-coast design**: 
> 
> - Don't consider JSON data chunk by chunk but as a **continuous flow of data from client to DB (or else) through server**,
> - Treat the **JSON flow like a pipe that you connect to others pipes** while applying modifications, transformations alongside,
> - Treat the flow in a **fully asynchronous/non-blocking** way.
>
> This is also one of the reason of being of Play2.1 reactive architecture…  
> I believe **considering your app through the prism of flows of data changes drastically the way you design** your web apps in general. It may also open new functional scopes that fit today's webapps requirements quite better than classic architecture. Anyway, this is not the subject here ;)

So, as you have deduced by yourself, to be able to manipulate Json flows based on validation and transformation directly, we needed some new tools. JSON combinators were good candidates but they are a bit too generic.  
That's why we have created some specialized combinators and API called **JSON transformers** to do that.

<br/>
# <a name="json-transf-are-reads">JSON transformers are `Reads[T <: JsValue]`</a>

You may tell JSON transformers are just `f:JSON => JSON`.  
So a JSON transformer could be simply a `Writes[A <: JsValue]`.  
But, a JSON transformer is not only a function: as we said, we also want to validate JSON while transforming it.  
As a consequence, a JSON transformer is a `Reads[A <: Jsvalue]`.

> **Keep in mind that a Reads[A <: JsValue] is able to transform and not only to read/validate**

## <a name="step-pick">Use `JsValue.transform` instead of `JsValue.validate`</a>

We have provided a function helper in `JsValue` to help people consider a `Reads[T]` is a transformer and not only a validator:

`JsValue.transform[A <: JsValue](reads: Reads[A]): JsResult[A]`

This is exactly the same `JsValue.validate(reads)`

## The details

In the code samples below, we’ll use the following JSON:

```
{
  "key1" : "value1",
  "key2" : {
    "key21" : 123,
    "key22" : true,
    "key23" : [ "alpha", "beta", "gamma"]
    "key24" : {
      "key241" : 234.123,
      "key242" : "value242"
    }
  },
  "key3" : 234
}
```

## <a name="step-pick">Case 1: Pick JSON value in JsPath</a>


### <a name="step-pick-1">Pick value as JsValue</a>

```
import play.api.libs.json._

val jsonTransformer = (__ \ 'key2 \ 'key23).json.pick

scala> json.transform(jsonTransformer)
res9: play.api.libs.json.JsResult[play.api.libs.json.JsValue] = 
	JsSuccess(
	  ["alpha","beta","gamma"],
	  /key2/key23
	)	
```

####`(__ \ 'key2 \ 'key23).json...` 
  - All JSON transformers are in `JsPath.json.`

####`(__ \ 'key2 \ 'key23).json.pick` 
  - `pick` is a `Reads[JsValue]` which picks the value **IN** the given JsPath. Here `["alpha","beta","gamma"]`
  
####`JsSuccess(["alpha","beta","gamma"],/key2/key23)`
  - This is a simply successful `JsResult`
  - For info, `/key2/key23` represents the JsPath where data were read but don't care about it, it's mainly used by Play API to compose JsResult(s))
  - `["alpha","beta","gamma"]` is just due to the fact that we have overriden `toString`

<br/>
> **Reminder** 
> `jsPath.json.pick` gets ONLY the value inside the JsPath
<br/>


### <a name="step-pick-2">Pick value as Type</a>

```
import play.api.libs.json._

val jsonTransformer = (__ \ 'key2 \ 'key23).json.pick[JsArray]

scala> json.transform(jsonTransformer)
res10: play.api.libs.json.JsResult[play.api.libs.json.JsArray] = 
	JsSuccess(
	  ["alpha","beta","gamma"],
	  /key2/key23
	)

```

####`(__ \ 'key2 \ 'key23).json.pick[JsArray]` 
  - `pick[T]` is a `Reads[T <: JsValue]` which picks the value (as a `JsArray` in our case) **IN** the given JsPath
  
<br/>
> **Reminder**
> `jsPath.json.pick[T <: JsValue]` extracts ONLY the typed value inside the JsPath

<br/>

## <a name="step-pickbranch">Case 2: Pick branch following JsPath</a>

### <a name="step-pickbranch-1">Pick branch as JsValue</a>

```
import play.api.libs.json._

val jsonTransformer = (__ \ 'key2 \ 'key24 \ 'key241).json.pickBranch

scala> json.transform(jsonTransformer)
res11: play.api.libs.json.JsResult[play.api.libs.json.JsObject] = 
  JsSuccess(
	{
	  "key2": {
	    "key24":{
	      "key241":234.123
	    }
	  }
	},
	/key2/key24/key241
  )

```

####`(__ \ 'key2 \ 'key23).json.pickBranch` 
  - `pickBranch` is a `Reads[JsValue]` which picks the branch from root to given JsPath
  
####`{"key2":{"key24":{"key242":"value242"}}}`
  - The result is the branch from root to given JsPath including the JsValue in JsPath
  
<br/>
> **Reminder:**
> `jsPath.json.pickBranch` extracts the single branch down to JsPath + the value inside JsPath

<br/>


## <a name="step-copyfrom">Case 3: Copy a value from input JsPath into a new JsPath</a>

```
import play.api.libs.json._

val jsonTransformer = (__ \ 'key25 \ 'key251).json.copyFrom( (__ \ 'key2 \ 'key21).json.pick )

scala> json.transform(jsonTransformer)
res12: play.api.libs.json.JsResult[play.api.libs.json.JsObject] 
  JsSuccess( 
    {
      "key25":{
        "key251":123
      }
    },
    /key2/key21
  )

```

####`(__ \ 'key25 \ 'key251).json.copyFrom( reads: Reads[A <: JsValue] )` 
  - `copyFrom` is a `Reads[JsValue]`
  - `copyFrom` reads the JsValue from input JSON using provided Reads[A]
  - `copyFrom` copies this extracted JsValue as the leaf of a new branch corresponding to given JsPath
  
####`{"key25":{"key251":123}}`
  - `copyFrom` reads value `123` 
  - `copyFrom` copies this value into new branch `(__ \ 'key25 \ 'key251)`

> **Reminder:**
> `jsPath.json.copyFrom(Reads[A <: JsValue])` reads value from input JSON and creates a new branch with result as leaf

<br/>
## <a name="step-update">Case 4: Copy full input Json & update a branch</a>

```
import play.api.libs.json._

val jsonTransformer = (__ \ 'key2 \ 'key24).json.update( 
	__.read[JsObject].map{ o => o ++ Json.obj( "field243" -> "coucou" ) }
)

scala> json.transform(jsonTransformer)
res13: play.api.libs.json.JsResult[play.api.libs.json.JsObject] = 
  JsSuccess(
    {
      "key1":"value1",
      "key2":{
        "key21":123,
        "key22":true,
        "key23":["alpha","beta","gamma"],
        "key24":{
          "key241":234.123,
          "key242":"value242",
          "field243":"coucou"
        }
      },
      "key3":234
    },
  )

```

####`(__ \ 'key2).json.update(reads: Reads[A < JsValue])` 
- is a `Reads[JsObject]`

####`(__ \ 'key2 \ 'key24).json.update(reads)` does 3 things: 
- extracts value from input JSON at JsPath `(__ \ 'key2 \ 'key24)`
- applies `reads` on this relative value and re-creates a branch `(__ \ 'key2 \ 'key24)` adding result of `reads` as leaf
- merges this branch with full input JSON replacing existing branch (so it works only with input JsObject and not other type of JsValue)

####`JsSuccess({…},)`
- Just for info, there is no JsPath as 2nd parameter there because the JSON manipulation was done from Root JsPath

<br/>
> **Reminder:**
> `jsPath.json.update(Reads[A <: JsValue])` only works for JsObject, copies full input `JsObject` and updates jsPath with provided `Reads[A <: JsValue]`


<br/>
## <a name="step-put">Case 5: Put a given value in a new branch</a>

```
import play.api.libs.json._

val jsonTransformer = (__ \ 'key24 \ 'key241).json.put(JsNumber(456))

scala> json.transform(jsonTransformer)
res14: play.api.libs.json.JsResult[play.api.libs.json.JsObject] = 
  JsSuccess(
    {
      "key24":{
        "key241":456
      }
    },
  )

```

####`(__ \ 'key24 \ 'key241).json.put( a: => JsValue )` 
- is a Reads[JsObject]

####`(__ \ 'key24 \ 'key241).json.put( a: => JsValue )`
- creates a new branch `(__ \ 'key24 \ 'key241)`
- puts `a` as leaf of this branch.

####`jsPath.json.put( a: => JsValue )`
- takes a JsValue argument passed by name allowing to pass even a closure to it.

####`jsPath.json.put` 
- doesn't care at all about input JSON
- simply replace input JSON by given value

<br/>
> **Reminder: **
> `jsPath.json.put( a: => Jsvalue )` creates a new branch with a given value without taking into account input JSON

<br/>
## <a name="step-prune">Case 6: Prune a branch from input JSON</a>

```
import play.api.libs.json._

val jsonTransformer = (__ \ 'key2 \ 'key22).json.prune

scala> json.transform(jsonTransformer)
res15: play.api.libs.json.JsResult[play.api.libs.json.JsObject] = 
  JsSuccess(
    {
      "key1":"value1",
      "key3":234,
      "key2":{
        "key21":123,
        "key23":["alpha","beta","gamma"],
        "key24":{
          "key241":234.123,
          "key242":"value242"
        }
      }
    },
    /key2/key22/key22
  )

```

####`(__ \ 'key2 \ 'key22).json.prune` 
- is a `Reads[JsObject]` that works only with JsObject

####`(__ \ 'key2 \ 'key22).json.prune`
- removes given JsPath from input JSON (`key22` has disappeared under `key2`)

Please note the resulting JsObject hasn't same keys order as input JsObject. This is due to the implementation of JsObject and to the merge mechanism. But this is not important since we have overriden `JsObject.equals` method to take this into account.

> **Reminder:**
> `jsPath.json.prune` only works with JsObject and removes given JsPath form input JSON)  
> 
> Please note that:
> - `prune` doesn't work for recursive JsPath for the time being
> - if `prune` doesn't find any branch to delete, it doesn't generate any error and returns unchanged JSON.

# <a name="more-complicated">More complicated cases</a>

## <a name="more-complicated-pick-update">Case 7: Pick a branch and update its content in 2 places</a>

```
import play.api.libs.json._
import play.api.libs.json.Reads._

val jsonTransformer = (__ \ 'key2).json.pickBranch(
  (__ \ 'key21).json.update( 
    of[JsNumber].map{ case JsNumber(nb) => JsNumber(nb + 10) }
  ) andThen 
  (__ \ 'key23).json.update( 
    of[JsArray].map{ case JsArray(arr) => JsArray(arr :+ JsString("delta")) }
  )
)

scala> json.transform(jsonTransformer)
res16: play.api.libs.json.JsResult[play.api.libs.json.JsObject] = 
  JsSuccess(
    {
      "key2":{
        "key21":133,
        "key22":true,
        "key23":["alpha","beta","gamma","delta"],
        "key24":{
          "key241":234.123,
          "key242":"value242"
        }
      }
    },
    /key2
  )

```

####`(__ \ 'key2).json.pickBranch(reads: Reads[A <: JsValue])` 
- extracts branch `__ \ 'key2` from input JSON and applies `reads` to the relative leaf of this branch (only to the content)

####`(__ \ 'key21).json.update(reads: Reads[A <: JsValue])` 
- updates `(__ \ 'key21)` branch

####`of[JsNumber]` 
- is just a `Reads[JsNumber]` 
- extracts a JsNumber from `(__ \ 'key21)`

####`of[JsNumber].map{ case JsNumber(nb) => JsNumber(nb + 10) }` 
- reads a JsNumber (_value 123_ in `__ \ 'key21`)
- uses `Reads[A].map` to increase it by _10_ (in immutable way naturally)

####`andThen` 
- is just the composition of 2 `Reads[A]`
- first reads is applied and then result is piped to second reads

####`of[JsArray].map{ case JsArray(arr) => JsArray(arr :+ JsString("delta")` 
- reads a JsArray (_value [alpha, beta, gamma] in `__ \ 'key23`_)
- uses `Reads[A].map` to append `JsString("delta")` to it

>Please note the result is just the `__ \ 'key2` branch since we picked only this branch

<br/>
## <a name="more-complicated-pick-prune">Case 8: Pick a branch and prune a sub-branch</a>

```
import play.api.libs.json._

val jsonTransformer = (__ \ 'key2).json.pickBranch(
  (__ \ 'key23).json.prune
)

scala> json.transform(jsonTransformer)
res18: play.api.libs.json.JsResult[play.api.libs.json.JsObject] = 
  JsSuccess(
    {
      "key2":{
        "key21":123,
        "key22":true,
        "key24":{
          "key241":234.123,
          "key242":"value242"
        }
      }
    },
    /key2/key23
  )

```

####`(__ \ 'key2).json.pickBranch(reads: Reads[A <: JsValue])` 
- extracts branch `__ \ 'key2` from input JSON and applies `reads` to the relative leaf of this branch (only to the content)


####`(__ \ 'key23).json.prune` 
- removes branch `__ \ 'key23` from relative JSON

>Please remark the result is just the `__ \ 'key2` branch without `key23` field.


# <a name="combinators">What about combinators?</a>

I stop there before it becomes boring (if not yet)…

Just keep in mind that you have now a huge toolkit to create generic JSON transformers.  
You can compose, map, flatmap transformers together into other transformers. So possibilities are almost infinite.

But there is a final point to treat: mixing those great new JSON transformers with previously presented Reads combinators.
This is quite trivial as JSON transformers are just `Reads[A <: JsValue]`

Let's demonstrate by writing a __Gizmo to Gremlin__ JSON transformer.

Here is Gizmo:

```
val gizmo = Json.obj(
  "name" -> "gizmo",
  "description" -> Json.obj(
    "features" -> Json.arr( "hairy", "cute", "gentle"),
    "size" -> 10,
    "sex" -> "undefined",
    "life_expectancy" -> "very old",
    "danger" -> Json.obj(
      "wet" -> "multiplies",
      "feed after midnight" -> "becomes gremlin"
    )
  ),
  "loves" -> "all"
)
```

Here is Gremlin:

```
val gremlin = Json.obj(
  "name" -> "gremlin",
  "description" -> Json.obj(
    "features" -> Json.arr("skinny", "ugly", "evil"),
    "size" -> 30,
    "sex" -> "undefined",
    "life_expectancy" -> "very old",
    "danger" -> "always"
  ),
  "hates" -> "all"
)
```

Ok let's write a JSON transformer to do this transformation

```
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

val gizmo2gremlin = (
	(__ \ 'name).json.put(JsString("gremlin")) and
	(__ \ 'description).json.pickBranch(
		(__ \ 'size).json.update( of[JsNumber].map{ case JsNumber(size) => JsNumber(size * 3) } ) and
		(__ \ 'features).json.put( Json.arr("skinny", "ugly", "evil") ) and
		(__ \ 'danger).json.put(JsString("always"))
		reduce
	) and
	(__ \ 'hates).json.copyFrom( (__ \ 'loves).json.pick )
) reduce

scala> gizmo.transform(gizmo2gremlin)
res22: play.api.libs.json.JsResult[play.api.libs.json.JsObject] = 
  JsSuccess(
    {
      "name":"gremlin",
      "description":{
        "features":["skinny","ugly","evil"],
        "size":30,
        "sex":"undefined",
        "life_expectancy":
        "very old","danger":"always"
      },
      "hates":"all"
    },
  )
```

Here we are ;)  
I'm not going to explain all of this because you should be able to understand now.  
Just remark:

####`(__ \ 'features).json.put(…)` is after `(__ \ 'size).json.update` so that it overwrites original `(__ \ 'features)`
<br/>
####`(Reads[JsObject] and Reads[JsObject]) reduce` 
- It merges results of both `Reads[JsObject]` (JsObject ++ JsObject)
- It also applies the same JSON to both `Reads[JsObject]` unlike `andThen` which injects the result of the first reads into second one.
