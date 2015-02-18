<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Handling and serving XML requests

## Handling an XML request

An XML request is an HTTP request using a valid XML payload as the request body. It must specify the `application/xml` or `text/xml` MIME type in its `Content-Type` header.

By default an `Action` uses a **any content** body parser, which lets you retrieve the body as XML (actually as a `NodeSeq`):

@[xml-request-body-asXml](code/ScalaXmlRequests.scala)

It’s way better (and simpler) to specify our own `BodyParser` to ask Play to parse the content body directly as XML:

@[xml-request-body-parser](code/ScalaXmlRequests.scala)

> **Note:** When using an XML body parser, the `request.body` value is directly a valid `NodeSeq`. 

You can test it with **cURL** from a command line:

```
curl 
  --header "Content-type: application/xml" 
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

@[xml-request-body-parser-xml-response](code/ScalaXmlRequests.scala)

Now it replies with:

```
HTTP/1.1 200 OK
Content-Type: application/xml; charset=utf-8
Content-Length: 46

<message status="OK">Hello Guillaume</message>
```
