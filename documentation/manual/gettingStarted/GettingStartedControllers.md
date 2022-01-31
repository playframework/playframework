# Controllers - Getting started

## A minimal controller

Below is a minimal implementation of a controller with a few helper methods to return different HTTP status types.

@[gettingStartedController](code/ScalaGettingStartedController.scala)

https://www.playframework.com/documentation/2.8.x/api/scala/play/api/mvc/InjectedController.html
[`Default`](api/scala/controllers/Default.html) controller API page has all listed helper types you may need.


To wire these functions up to a route, we will need to specify those functions in the `conf/routes` file. 

@[gettingStartedController](code/scalaguide.gettingStarted.GettingStartedController.routes)

[[Find out more about routes and routers here.|ScalaRouting]]

## Calling a Twirl template

Finally, we can render our view layer by calling a twirl template like so 

@[gettingStartedController](code/ScalaGettingStartedController.scala)

We need to add a file called `ourTwirlTemplate.html.scala` in the `app/views` directory that looks like this:

@[gettingStartedTwirlTemplate](code/ourTwirlTemplate.html.scala)

[[Find out more about templates here.|ScalaTemplates]]

## More about controllers

- [[Actions, Controllers and Results|ScalaActions]]
- [[Actions and Controller Composition|ScalaActionsComposition]]


