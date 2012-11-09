# Handling and serving JSON requests

## Handling a JSON request

A JSON request is an HTTP request using a valid JSON payload as request body. It must specify the `text/json` or `application/json` mime type in its `Content-Type` header.

By default an `Action` uses an **any content** body parser, which lets you retrieve the body as JSON (actually as a `JsValue`):

```scala
def sayHello = Action { request =>
  request.body.asJson.map { json =>
    (json \ "name").asOpt[String].map { name =>
      Ok("Hello " + name)
    }.getOrElse {
      BadRequest("Missing parameter [name]")
    }
  }.getOrElse {
    BadRequest("Expecting Json data")
  }
}
```

It's better (and simpler) to specify our own `BodyParser` to ask Play to parse the content body directly as JSON:

```scala
def sayHello = Action(parse.json) { request =>
  (request.body \ "name").asOpt[String].map { name =>
    Ok("Hello " + name)
  }.getOrElse {
    BadRequest("Missing parameter [name]")
  }
}
```

> **Note:** When using a JSON body parser, the `request.body` value is directly a valid `JsValue`. 

You can test it with **cURL** from the command line:

```
curl 
  --header "Content-type: application/json" 
  --request POST 
  --data '{"name": "Guillaume"}' 
  http://localhost:9000/sayHello
```

It replies with:

```
HTTP/1.1 200 OK
Content-Type: text/plain; charset=utf-8
Content-Length: 15

Hello Guillaume
```

## Serving a JSON response

In our previous example we handle a JSON request, but we reply with a `text/plain` response. Let’s change that to send back a valid JSON HTTP response:

```scala
def sayHello = Action(parse.json) { request =>
  (request.body \ "name").asOpt[String].map { name =>
    Ok(toJson(
      Map("status" -> "OK", "message" -> ("Hello " + name))
    ))
  }.getOrElse {
    BadRequest(toJson(
      Map("status" -> "KO", "message" -> "Missing parameter [name]")
    ))
  }
}
```

Now it replies with:

```
HTTP/1.1 200 OK
Content-Type: application/json; charset=utf-8
Content-Length: 43

{"status":"OK","message":"Hello Guillaume"}
```

> **Next:** [[Working with XML | ScalaXmlRequests]]