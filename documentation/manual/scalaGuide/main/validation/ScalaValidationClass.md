## Working with `Validation`

```scala
def demo = Action(parse.json) { request =>
  import play.api.data.mapping._
  import play.api.data.mapping.json.Rules._

  val json = request.body
  val findFriend = (Path \ "user" \ "friend").read[JsValue, JsValue]
  val validated = findFriend.validate(json)

  validated.fold(
    errors => BadRequest,
    friend => Ok(friend)
  )
}
```