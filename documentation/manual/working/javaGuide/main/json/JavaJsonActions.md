<!--- Copyright (C) Lightbend Inc. <https://www.lightbend.com> -->

# Handling and serving JSON

In Java, Play uses the [Jackson](https://github.com/FasterXML/jackson#documentation) JSON library to convert objects to and from JSON. Play's actions work with the `JsonNode` type and the framework provides utility methods for conversion in the `play.libs.Json` API.

## Mapping Java objects to JSON

Jackson allows you to easily convert Java objects to JSON by looking at field names, getters and setters. As an example we'll use the following simple Java object:

@[person-class](code/javaguide/json/JavaJsonActions.java)

We can parse the JSON representation of the object and create a new `Person`:

@[to-json](code/javaguide/json/JavaJsonActions.java)

Similarly, we can write the `Person` object to a `JsonNode`:

@[from-json](code/javaguide/json/JavaJsonActions.java)

## Handling a JSON request

A JSON request is an HTTP request using a valid JSON payload as request body. Its `Content-Type` header must specify the `text/json` or `application/json` MIME type.

By default an action uses an **any content** body parser, which you can use to retrieve the body as JSON (actually as a Jackson `JsonNode`):

@[json-request-as-anycontent](code/javaguide/json/JavaJsonActions.java)

@[json-request-as-anyclazz](code/javaguide/json/JavaJsonActions.java)

Of course it’s way better (and simpler) to specify our own `BodyParser` to ask Play to parse the content body directly as JSON:

@[json-request-as-json](code/javaguide/json/JavaJsonActions.java)

> **Note:** This way, a 400 HTTP response will be automatically returned for non JSON requests with Content-type set to application/json.

You can test it with **`curl`** from a command line:

```bash
curl
  --header "Content-type: application/json"
  --request POST
  --data '{"name": "Guillaume"}'
  http://localhost:9000/sayHello
```

It replies with:

```http
HTTP/1.1 200 OK
Content-Type: text/plain; charset=utf-8
Content-Length: 15

Hello Guillaume
```

## Serving a JSON response

In our previous example we handled a JSON request, but replied with a `text/plain` response. Let’s change that to send back a valid JSON HTTP response:

@[json-response](code/javaguide/json/JavaJsonActions.java)

Now it replies with:

```http
HTTP/1.1 200 OK
Content-Type: application/json; charset=utf-8

{"exampleField1":"foobar","exampleField2":"Hello world!"}
```

You can also return a Java object and have it automatically serialized to JSON by the Jackson library:

@[json-response-dao](code/javaguide/json/JavaJsonActions.java)

If you already have a JSON `String` that you'd like to return, you can do that as well:

@[json-response-string](code/javaguide/json/JavaJsonActions.java)

## Advanced usage

There are two possible ways to customize the `ObjectMapper` for your application.

### Configurations in `application.conf`

Because Play uses Akka Jackson serialization support, you can configure the `ObjectMapper` based on your application needs. The [documentation for jackson-databind Features](https://github.com/FasterXML/jackson-databind/wiki/JacksonFeatures) explains how you can further customize JSON conversion process, including [Mapper](https://github.com/FasterXML/jackson-databind/wiki/Mapper-Features), [Serialization](https://github.com/FasterXML/jackson-databind/wiki/Serialization-Features) and [Deserialization](https://github.com/FasterXML/jackson-databind/wiki/Deserialization-Features) features.

If you would like to use Play's `Json` APIs (`toJson`/`fromJson`) with a customized `ObjectMapper`, you need to add the custom configurations in your `application.conf`. For example, if you want to add a new [module for Joda types](https://github.com/FasterXML/jackson-datatype-joda)

```HOCON
akka.serialization.jackson.play.jackson-modules += "com.fasterxml.jackson.datatype.joda.JodaModule"
```

Or to add set a serialization configuration:

```HOCON
akka.serialization.jackson.play.serialization-features.WRITE_NUMBERS_AS_STRINGS=true
```

### Custom binding for `ObjectMapper`

If you still want to take completely over the `ObjectMapper` creation, this is possible by overriding its binding configuration. You first need to disable the default `ObjectMapper` module in your `application.conf`

```HOCON
play.modules.disabled += "play.core.ObjectMapperModule"
```

Then you can create a provider for your `ObjectMapper`:

@[custom-java-object-mapper](code/javaguide/json/JavaJsonCustomObjectMapper.java)

And bind it via Guice as an eager singleton so that the `ObjectMapper` will be set into the `Json` helper:

@[custom-java-object-mapper2](code/javaguide/json/JavaJsonCustomObjectMapperModule.java)

Afterwards enable the Module:

```HOCON
play.modules.enabled += "path.to.JavaJsonCustomObjectMapperModule"
```
