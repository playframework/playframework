<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# JSON with HTTP

Play supports HTTP requests and responses with a content type of JSON by using the HTTP API in combination with the JSON library.

> See  [[HTTP Programming | ScalaActions]] for details on Controllers, Actions, and routing.

We'll demonstrate the necessary concepts by designing a simple RESTful web service to GET a list of entities and accept POSTs to create new entities. The service will use a content type of JSON for all data.

Here's the model we'll use for our service:

@[model](code/ScalaJsonHttpSpec.scala)

## Serving a list of entities in JSON

We'll start by adding the necessary imports to our controller.

@[controller](code/ScalaJsonHttpSpec.scala)

Before we write our `Action`, we'll need the plumbing for doing conversion from our model to a `JsValue` representation. This is accomplished by defining an implicit `Writes[Place]`.

@[serve-json-implicits](code/ScalaJsonHttpSpec.scala)

Next we write our `Action`:

@[serve-json](code/ScalaJsonHttpSpec.scala)

The `Action` retrieves a list of `Place` objects, converts them to a `JsValue` using `Json.toJson` with our implicit `Writes[Place]`, and returns this as the body of the result. Play will recognize the result as JSON and set the appropriate `Content-Type` header and body value for the response. 

The last step is to add a route for our `Action` in `conf/routes`:

```
GET   /places               controllers.Application.listPlaces
```

We can test the action by making a request with a browser or HTTP tool. This example uses the unix command line tool [cURL](http://curl.haxx.se/).

```
curl --include http://localhost:9000/places
```

Response:

```
HTTP/1.1 200 OK
Content-Type: application/json; charset=utf-8
Content-Length: 141

[{"name":"Sandleford","location":{"lat":51.377797,"long":-1.318965}},{"name":"Watership Down","location":{"lat":51.235685,"long":-1.309197}}]
```

## Creating a new entity instance in JSON

For this `Action` we'll need to define an implicit `Reads[Place]` to convert a `JsValue` to our model.

@[handle-json-implicits](code/ScalaJsonHttpSpec.scala)

Next we'll define the `Action`.

@[handle-json-bodyparser](code/ScalaJsonHttpSpec.scala)

This `Action` is more complicated than our list case. Some things to note:

- This `Action` expects a request with a `Content-Type` header of `text/json` or `application/json` and a body containing a JSON representation of the entity to create.
- It uses a JSON specific `BodyParser` will parse the request and provide `request.body` as a `JsValue`. 
- We used the `validate` method for conversion which will rely on our implicit `Reads[Place]'.
- To process the validation result, we used a `fold` with error and success flows. This pattern may be familiar as it is also used for form submission.
- The `Action` also sends JSON responses.

Finally we'll add a route binding in `conf/routes`:

```
POST  /places               controllers.Application.savePlace
```

We'll test this action with valid and invalid requests to verify our success and error flows. 

Testing the action with a valid data:

```
curl --include
  --request POST
  --header "Content-type: application/json" 
  --data '{"name":"Nuthanger Farm","location":{"lat" : 51.244031,"long" : -1.263224}}' 
  http://localhost:9000/places
```

Response:

```
HTTP/1.1 200 OK
Content-Type: application/json; charset=utf-8
Content-Length: 57

{"status":"OK","message":"Place 'Nuthanger Farm' saved."}
```

Testing the action with a invalid data, missing "name" field:

```
curl --include
  --request POST
  --header "Content-type: application/json"
  --data '{"location":{"lat" : 51.244031,"long" : -1.263224}}' 
  http://localhost:9000/places
```
Response:

```
HTTP/1.1 400 Bad Request
Content-Type: application/json; charset=utf-8
Content-Length: 79

{"status":"KO","message":{"obj.name":[{"msg":"error.path.missing","args":[]}]}}
```
Testing the action with a invalid data, wrong data type for "lat":

```
curl --include
  --request POST
  --header "Content-type: application/json" 
  --data '{"name":"Nuthanger Farm","location":{"lat" : "xxx","long" : -1.263224}}' 
  http://localhost:9000/places
```
Response:

```
HTTP/1.1 400 Bad Request
Content-Type: application/json; charset=utf-8
Content-Length: 92

{"status":"KO","message":{"obj.location.lat":[{"msg":"error.expected.jsnumber","args":[]}]}}
```

## Summary
Play is designed to support REST with JSON and developing these services should hopefully be straightforward. The bulk of the work is in writing `Reads` and `Writes` for your model, which is covered in detail in the next section. 
