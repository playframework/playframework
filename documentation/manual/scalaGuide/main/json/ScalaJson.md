# The Play JSON library

## Overview

The recommend way of dealing with JSON is using Play’s typeclass based JSON library, located at ```play.api.libs.json```. 

This library is built on top of [Jerkson](https://github.com/codahale/jerkson/), which is a Scala wrapper around the super-fast Java based JSON library, [Jackson](http://jackson.codehaus.org/). 

The benefit of this approach is that both the Java and the Scala side of Play can share the same underlying library (Jackson), while Scala users can enjoy the extra type safety that Play’s JSON support brings to the table.

`play.api.libs.json` package contains seven JSON data types: 

- ```JsObject```
- ```JsNull```
- ```JsUndefined```
- ```JsBoolean```
- ```JsNumber```
- ```JsArray```
- ```JsString```

All of them inherit from the generic JSON value, ```JsValue```.

## Parsing a Json String

You can easily parse any JSON string as a `JsValue`:

```
val json: JsValue = Json.parse(jsonString)
```

## Navigating into a Json tree

As soon as you have a `JsValue` you can navigate into the tree. The API looks like the one provided to navigate into XML document by Scala using `NodeSeq`:

```
val json = Json.parse(jsonString)

val maybeName = (json \ "user" \ name).asOpt[String]
val emails = (json \ "user" \\ "emails").map(_.as[String])
```

> **Note** that navigating using \ and \\ never fails. You must handle the error case at the end using `asOpt[T]` that will return `None` if the value is missing. Otherwiser you can use `as[T]` that we fail with an exception if the value was missing.

## Converting a Scala value to Json

As soon as you have a type class able to transform the Scala type to Json, it is pretty easy to generate any Scala value to Json. For example let's create a simple Json object:

```
val jsonNumber = Json.toJson(4)
```

Or create a json array:

```
val jsonArray = Json.toJson(Seq(1, 2, 3, 4))
```

Here we have no problem to convert a `Seq[Int]` into a Json array. However it is more complicated if the `Seq` contains heterogeous values:

```
val jsonArray = Json.toJson(Seq(1, "Bob", 3, 4))
```

Because there is no way to convert a `Seq[Any]` to Json (`Any` could be anything including something not supported by Json right?)

A simple solution is to handle it as a `Seq[JsValue]`:

```
val jsonArray = Json.toJson(Seq(
  toJson(1), toJson("Bob"), toJson(3), toJson(4)
))
```

Now let's see a last example of creating a more complex Json object:

```
val jsonObject = Json.toJson(
  Map(
    "users" -> Seq(
      toJson(
        Map(
          "name" -> toJson("Bob"),
          "age" -> toJson(31),
          "email" -> toJson("bob@gmail.com")
        )
      ),
      toJson(
        Map(
          "name" -> toJson("Kiki"),
          "age" -> toJson(25),
          "email" -> JsNull
        )
      )
    )
  )
)
```

That will generate this Json result:

```
{
  "users":[
    {
      "name": "Bob",
      "age": 31.0,
      "email": "bob@gmail.com"
    },
    {
      "name": "Kiki",
      "age":  25.0,
      "email": null
    }
  ]
}
```

## Serializing Json

Serializing a `JsValue` to its json String representation is easy:

```
val jsonString: String = Json.stringify(jsValue)
```

## Other options

While the typeclass based solution describe above is the on that's recommended, nothing stopping users from using any other JSON libraries if needed.

For example, here is a small snippet which demonstrates how to marshal plain scala objects into JSON and send it over the wire using the bundled, reflection based [[Jerkson | https://github.com/codahale/jerkson/]] library:

```scala
import com.codahale.jerkson.Json._

val json = generate(
  Map( 
    "url"-> "http://nytimes.com",
    "attributes" -> Map(
      "name" -> "nytimes", 
      "country" -> "US",
      "id" -> 25
    ), 
    "links" -> List(
      "http://link1",
      "http://link2"
    )
  )
)
```

> **Next:** [[Handling and serving Json requests | ScalaJsonRequests]]