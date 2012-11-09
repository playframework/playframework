# Writing functional tests

## Testing a template

Since a template is a standard Scala function, you can execute it from your test, and check the result:

```scala
"render index template" in {
  val html = views.html.index("Coco")
  
  contentType(html) must equalTo("text/html")
  contentAsString(html) must contain("Hello Coco")
}
```

## Testing your controllers

You can call any `Action` code by providing a `FakeRequest`:

```scala
"respond to the index Action" in {
  val result = controllers.Application.index("Bob")(FakeRequest())
  
  status(result) must equalTo(OK)
  contentType(result) must beSome("text/html")
  charset(result) must beSome("utf-8")
  contentAsString(result) must contain("Hello Bob")
}
```

## Testing the router

Instead of calling the `Action` yourself, you can let the `Router` do it:

```scala
"respond to the index Action" in {
  val Some(result) = routeAndCall(FakeRequest(GET, "/Bob"))
  
  status(result) must equalTo(OK)
  contentType(result) must beSome("text/html")
  charset(result) must beSome("utf-8")
  contentAsString(result) must contain("Hello Bob")
}
```

## Starting a real HTTP server

Sometimes you want to test the real HTTP stack from with your test, in which case you can start a test server:

```scala
"run in a server" in {
  running(TestServer(3333)) {
  
    await(WS.url("http://localhost:3333").get).status must equalTo(OK)
  
  }
}
```

## Testing from within a Web browser.

If you want to test your application using a browser, you can use [[Selenium WebDriver| http://code.google.com/p/selenium/?redir=1]]. Play will start the WebDriver for your, and wrap it in the convenient API provided by [[FluentLenium|https://github.com/FluentLenium/FluentLenium]].

```scala
"run in a browser" in {
  running(TestServer(3333), HTMLUNIT) { browser =>
    
    browser.goTo("http://localhost:3333")
    browser.$("#title").getTexts().get(0) must equalTo("Hello Guest")
    
    browser.$("a").click()
    
    browser.url must equalTo("http://localhost:3333/Coco")
    browser.$("#title").getTexts().get(0) must equalTo("Hello Coco")

  }
}
```

> **Next:** [[Advanced topics | Iteratees]]