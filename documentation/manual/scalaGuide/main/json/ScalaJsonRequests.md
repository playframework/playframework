# Handling and serving JSON requests

## Handling a JSON request

A JSON request is an HTTP request using a valid JSON payload as request body. It must specify the `text/json` or `application/json` mime type in its `Content-Type` header.

By default an `Action` uses an **any content** body parser, which lets you retrieve the body as JSON (actually as a `JsValue`):

```scala
package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
// you need this import to have combinators
import play.api.libs.functional.syntax._

object Application extends Controller {
  
  implicit val rds = (
    (__ \ 'name).read[String] and
    (__ \ 'age).read[Long]
  ) tupled

  def sayHello = Action { request =>
    request.body.asJson.map { json =>
      json.validate[(String, Long)].map{ 
        case (name, age) => Ok("Hello " + name + ", you're "+age)
      }.recoverTotal{
        e => BadRequest("Detected error:"+ JsError.toFlatJson(e))
      }
    }.getOrElse {
      BadRequest("Expecting Json data")
    }
  }
}
```

It's better (and simpler) to specify our own `BodyParser` to ask Play to parse the content body directly as JSON:

```scala
  def sayHello = Action(parse.json) { request =>
    request.body.validate[(String, Long)].map{ 
      case (name, age) => Ok("Hello " + name + ", you're "+age)
    }.recoverTotal{
      e => BadRequest("Detected error:"+ JsError.toFlatJson(e))
    }
  }
```

> **Note:** When using a JSON body parser, the `request.body` value is directly a valid `JsValue`. 

Please note:

#### `implicits Reads[(String, Long)]` 
It defines an implicits Reads using combinators which can validate and transform input JSON.

#### `json.validate[(String, Long)]` 
It explicitly validates & transforms input JSON according to implicit `Reads[(String, Long)]`


You can test it with **cURL** from the command line:

#### `json.validate[(String, Long)].map{ (String, Long) => ... } `

This maps the result in case of success to transform it into an action result.

#### `json.validate[(String, Long)].recoverTotal{ e: JsError => ... }`

`recoverTotal` takes a function to manage errors and returns a default value:
- it ends the `JsResult` modification chain and returns the successful inner value 
- or if detected a failure, it returns the result of the function provided to `recoverTotal`.

#### `JsError.toFlatJson(e)`
This is a helper that transforms the `JsError` into a flattened JsObject form :

```
JsError(List((/age,List(ValidationError(validate.error.missing-path,WrappedArray()))), (/name,List(ValidationError(validate.error.missing-path,WrappedArray())))))
```

would become JsValue:

```
{"obj.age":[{"msg":"validate.error.missing-path","args":[]}],"obj.name":[{"msg":"validate.error.missing-path","args":[]}]}
```

> Please note a few other helpers should be provided later.


**Let's try it**

### case OK
```
curl 
  --header "Content-type: application/json" 
  --request POST 
  --data '{"name": "Toto", "age": 32}' 
  http://localhost:9000/sayHello
```

It replies with:

```
HTTP/1.1 200 OK
Content-Type: text/plain; charset=utf-8
Content-Length: 47

Hello Toto, you're 32
```

### case KO "JSON missing field"
```
curl 
  --header "Content-type: application/json" 
  --request POST 
  --data '{"name2": "Toto", "age2": 32}' 
  http://localhost:9000/sayHello
```

It replies with:


```
HTTP/1.1 400 Bad Request
Content-Type: text/plain; charset=utf-8
Content-Length: 106

Detected error:{"obj.age":[{"msg":"validate.error.missing-path","args":[]}],"obj.name":[{"msg":"validate.error.missing-path","args":[]}]}
```

### case KO "JSON bad type"
```
curl 
  --header "Content-type: application/json" 
  --request POST 
  --data '{"name": "Toto", "age": "chboing"}' 
  http://localhost:9000/sayHello
```

It replies with:


```
HTTP/1.1 400 Bad Request
Content-Type: text/plain; charset=utf-8
Content-Length: 100

Detected error:{"obj.age":[{"msg":"validate.error.expected.jsnumber","args":[]}]}
```


## Serving a JSON response

In our previous example we handle a JSON request, but we reply with a `text/plain` response. Let’s change that to send back a valid JSON HTTP response:

```scala
  def sayHello = Action(parse.json) { request =>
    request.body.validate[(String, Long)].map{ 
      case (name, age) => Ok(Json.obj("status" ->"OK", "message" -> ("Hello "+name+" , you're "+age) ))
    }.recoverTotal{
      e => BadRequest(Json.obj("status" ->"KO", "message" -> JsError.toFlatJson(e)))
    }
  }
```

Now it replies with:

```
HTTP/1.1 200 OK
Content-Type: application/json; charset=utf-8
Content-Length: 47

{"status":"OK","message":"Hello Toto, you're 32"}
```

## Sending JSON directly

Sending the list of Todos with Play 2.1 and JSON is very simple:

```scala
import play.api.libs.json.Json

def tasksAsJson() = Action {
  Ok(Json.toJson(Task.all().map { t=>
    (t.id.toString, t.label)
  } toMap))
}
```

> **Next:** [[Working with XML | ScalaXmlRequests]]