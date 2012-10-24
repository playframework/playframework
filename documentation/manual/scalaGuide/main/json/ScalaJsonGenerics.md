# The Play JSON library with generics

## Overview

When using Play's typeclass based JSON library it is possible to include generics support into these typeclasses. This can be a useful approach if the REST API uses a base structure for query results with the elements of the query changing based on the endpoint. 


## Generic Support with Scala

Given the basic structure of the search results as follows:

```
case class SearchResults[T](
    elements: List[T], 
    page: Int, 
    pageSize: Int, 
    total :Int
)
```

Now you have to simply ensure that the reads and writes method are able to handle the generic by having an implicit argument to support the reads and writes for the type class T.

```
object SearchResults
{
  implicit def searchResultsReads[T](implicit fmt: Reads[T]): Reads[SearchResults[T]] = new Reads[SearchResults[T]] {
	def reads(json: JsValue): SearchResults[T] = new SearchResults[T] (
	  
     (json \ "elements") match {
      	case JsArray(ts) => ts.map(t => fromJson(t)(fmt))
      	case _ => throw new RuntimeException("Elements MUST be a list")
	  },
      (json \ "page").as[Int],
      (json \ "pageSize").as[Int],
      (json \ "total").as[Int]
    )
  }
  
  implicit def searchResultsWrites[T](implicit fmt: Writes[T]): Writes[SearchResults[T]] = new Writes[SearchResults[T]] {
    def writes(ts: SearchResults[T]) = JsObject(Seq(
        "page" -> JsNumber(ts.page),
        "pageSize" -> JsNumber(ts.pageSize),
        "total" -> JsNumber(ts.total),
        "elements" -> JsArray(ts.elements.map(toJson(_)))
    ))  
  }
}
```

## Basic JSON data types

Given the type class above, you can easily create and use any type that corresponds to the basic data types in the `play.api.libs.json` package

```
val input = """{"page": 1,"pageSize":2, "total": 3, "elements" : [1, 2, 3]}"""
val ret = play.api.libs.json.Json.parse(input).as[SearchResults[Int]]
```

## More complex types

A more complex Json object can also be supported by ensuring that it has the serialization and de-serialization methods defined:

```
case class Foo(name: String, entry: Int) 

object Foo {
  implicit object FopReads extends Format[Foo] {
    def reads(json: JsValue) = Foo(
      (json \ "name").as[String],
      (json \ "entry").as[Int])
    def writes(ts: Foo) = JsObject(Seq(
      "name" -> JsString(ts.name),
      "entry" -> JsNumber(ts.entry)))
  }
}
```

With this setup its easy to use the case class '''Foo''' as part of the '''SearchResults'''.

```
val input = """{"page": 1,"pageSize":2, "total": 3, "elements" : [ {"name" : "foo", "entry" : 1 }, {"name" : "bar", "entry" : 2 }]}"""
val ret = play.api.libs.json.Json.parse(input).as[SearchResults[Foo]]
```