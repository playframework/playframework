<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Testing web service clients

A lot of code can go into writing a web service client - preparing the request, serializing and deserializing the bodies, setting the correct headers.  Since a lot of this code works with strings and weakly typed maps, testing it is very important.  However testing it also presents some challenges.  Some common approaches include:

### Test against the actual web service

This of course gives the highest level of confidence in the client code, however it is usually not practical.  If it's a third party web service, there may be rate limiting in place that prevents your tests from running (and running automated tests against a third party service is not considered being a good netizen).  It may not be possible to set up or ensure the existence of the necessary data that your tests require on that service, and your tests may have undesirable side effects on the service.

### Test against a test instance of the web service

This is a little better than the previous one, however it still has a number of problems.  Many third party web services don't provide test instances.  It also means your tests depend on the test instance being running, meaning that test service could cause your build to fail.  If the test instance is behind a firewall, it also limits where the tests can be run from.

### Mock the http client

This approach gives the least confidence in the test code - often this kind of testing amounts to testing no more than that the code does what it does, which is of no value.  Tests against mock web service clients show that the code runs and does certain things, but gives no confidence as to whether anything that the code does actually correlates to valid HTTP requests being made.

### Mock the web service

This approach is a good compromise between testing against the actual web service and mocking the http client.  Your tests will show that all the requests it makes are valid HTTP requests, that serialisation/deserialisation of bodies work, etc, but they will be entirely self contained, not depending on any third party services.

Play provides some helper utilities for mocking a web service in tests, making this approach to testing a very viable and attractive option.

## Testing a GitHub client

As an example, let's say you've written a GitHub client, and you want to test it.  The client is very simple, it just allows you to look up the names of the public repositories:

@[client](code/webservice/ScalaTestingWebServiceClients.scala)

Note that it takes the GitHub API base URL as a parameter - we'll override this in our tests so that we can point it to our mock server.

To test this, we want an embedded Play server that will implement this endpoint.  We can do that using the [`Server`](api/scala/index.html#play.core.server.Server) `withRouter` helper in combination with the [[String Interpolating Routing DSL|ScalaSirdRouter]]:

@[mock-service](code/webservice/ScalaTestingWebServiceClients.scala)

The `withRouter` method takes a block of code that takes as input the port number that the server starts on.  By default, Play starts the server on a random free port - this means that you don't need to worry about resource contention on build servers or assigning ports to tests, but it means that your code does need to be told which port is going to be used.

Now to test the GitHub client, we need a `WSClient` for it.  Play provides a [`WsTestClient`](api/scala/index.html#play.api.test.WsTestClient$) trait that has some factory methods for creating test clients.  The `withClient` takes an implicit port, this is handy to use in combination with the `Server.withRouter` method.

The client that the `WsTestClient.withClient` method creates here is a special client - if you give it a relative URL, then it will default the hostname to `localhost` and the port number to the port number passed in implicitly.  Using this, we can simply set the base url for our GitHub client to be an empty String.

Putting it all together, we have this:

@[full-test](code/webservice/ScalaTestingWebServiceClients.scala)

### Returning files

In the previous example, we built the json manually for the mocked service.  It often will be better to capture an actual response from the service your testing, and return that.  To assist with this, Play provides a `sendResource` method that allows easily creating results from files on the classpath.

So after making a request on the actual GitHub API, create a file to store it in the test resources directory.  The test resources directory is either `test/resources` if you're using a Play directory layout, or `src/test/resources` if you're using a standard sbt directory layout.  In this case, we'll call it `github/repositories.json`, and it will contain the following:

```json
[
  {
    "id": 1296269,
    "owner": {
      "login": "octocat",
      "id": 1,
      "avatar_url": "https://github.com/images/error/octocat_happy.gif",
      "gravatar_id": "",
      "url": "https://api.github.com/users/octocat",
      "html_url": "https://github.com/octocat",
      "followers_url": "https://api.github.com/users/octocat/followers",
      "following_url": "https://api.github.com/users/octocat/following{/other_user}",
      "gists_url": "https://api.github.com/users/octocat/gists{/gist_id}",
      "starred_url": "https://api.github.com/users/octocat/starred{/owner}{/repo}",
      "subscriptions_url": "https://api.github.com/users/octocat/subscriptions",
      "organizations_url": "https://api.github.com/users/octocat/orgs",
      "repos_url": "https://api.github.com/users/octocat/repos",
      "events_url": "https://api.github.com/users/octocat/events{/privacy}",
      "received_events_url": "https://api.github.com/users/octocat/received_events",
      "type": "User",
      "site_admin": false
    },
    "name": "Hello-World",
    "full_name": "octocat/Hello-World",
    "description": "This your first repo!",
    "private": false,
    "fork": false,
    "url": "https://api.github.com/repos/octocat/Hello-World",
    "html_url": "https://github.com/octocat/Hello-World"
  }
]
```

You may decide to modify it to suit your testing needs, for example, if your GitHub client used the URLs in the above response to make requests to other endpoints, you might remove the `https://api.github.com` prefix from them so that they too are relative, and will automatically be routed to localhost on the right port by the test client.

Now, modify the router to serve this resource:

@[send-resource](code/webservice/ScalaTestingWebServiceClients.scala)

Note that Play will automatically set a content type of `application/json` due to the filename's extension of `.json`.

### Extracting setup code

The tests implemented so far are fine if you only have one test you want to run, but if you have many methods that you want to test, it may make more sense to extract the mock client setup code out into one helper method.  For example, we could define a `withGitHubClient` method:

@[with-github-client](code/webservice/ScalaTestingWebServiceClients.scala)

And then using it in a test looks like:

@[with-github-test](code/webservice/ScalaTestingWebServiceClients.scala)
