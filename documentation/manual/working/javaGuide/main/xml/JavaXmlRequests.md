<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Handling and serving XML requests

## Handling an XML request

An XML request is an HTTP request using a valid XML payload as request body. It must specify the `application/xml` or `text/xml` MIME type in its `Content-Type` header.

By default, an action uses an **any content** body parser, which you can use to retrieve the body as XML (actually as a `org.w3c.Document`):

@[xml-hello](code/javaguide/xml/JavaXmlRequests.java)

Of course it’s way better (and simpler) to specify our own `BodyParser` to ask Play to parse the content body directly as XML:

@[xml-hello-bodyparser](code/javaguide/xml/JavaXmlRequests.java)

> **Note:** This way, a 400 HTTP response will be automatically returned for non-XML requests.

You can test it with **cURL** on the command line:

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

In our previous example, we handled an XML request, but replied with a `text/plain` response. Let’s change it to send back a valid XML HTTP response:

@[xml-reply](code/javaguide/xml/JavaXmlRequests.java)

Now it replies with:

```
HTTP/1.1 200 OK
Content-Type: application/xml; charset=utf-8
Content-Length: 46

<message status="OK">Hello Guillaume</message>
```
