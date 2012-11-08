# Handling and serving XML requests

## Handling an XML request

An XML request is an HTTP request using a valid XML payload as the request body. It must specify the `text/xml` MIME type in its `Content-Type` header.

By default an `Action` uses a **any content** body parser, which lets you retrieve the body as XML (actually as a `NodeSeq`):

```scala
def sayHello = Action { request =>
  request.body.asXml.map { xml =>
    (xml \\ "name" headOption).map(_.text).map { name =>
      Ok("Hello " + name)
    }.getOrElse {
      BadRequest("Missing parameter [name]")
    }
  }.getOrElse {
    BadRequest("Expecting Xml data")
  }
}
```

It’s way better (and simpler) to specify our own `BodyParser` to ask Play to parse the content body directly as XML:

```scala
def sayHello = Action(parse.xml) { request =>
  (request.body \\ "name" headOption).map(_.text).map { name =>
    Ok("Hello " + name)
  }.getOrElse {
    BadRequest("Missing parameter [name]")
  }
}
```

> **Note:** When using an XML body parser, the `request.body` value is directly a valid `NodeSeq`. 

You can test it with **cURL** from a command line:

```
curl 
  --header "Content-type: text/xml" 
  --request POST 
  --data '<name>Guillaume</name>' 
  http://localhost:9000/sayHello
```

It replies with:

```
HTTP/1.1 200 OK
Content-Type: text/plain; charset=utf-8
Content-Length: 15

Hello Guillaume
```

## Serving an XML response

In our previous example we handle an XML request, but we reply with a `text/plain` response. Let’s change that to send back a valid XML HTTP response:

```scala
def sayHello = Action(parse.xml) { request =>
  (request.body \\ "name" headOption).map(_.text).map { name =>
    Ok(<message status="OK">Hello {name}</message>)
  }.getOrElse {
    BadRequest(<message status="KO">Missing parameter [name]</message>)
  }
}
```

Now it replies with:

```
HTTP/1.1 200 OK
Content-Type: text/xml; charset=utf-8
Content-Length: 46

<message status="OK">Hello Guillaume</message>
```

> **Next:** [[Handling file upload | ScalaFileUpload]]