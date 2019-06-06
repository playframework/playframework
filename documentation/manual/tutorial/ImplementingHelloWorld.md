<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->

# Implementing Hello World

To see how simple it is to work with Play, let's add a customized `"Hello World"` greeting to this tutorial app.

The main steps include:

1. Create the Hello World page
1. Add an action method
1. Define a route
1. Customize the greeting

## 1. Create the Hello World page

Follow the instructions below to add a new Hello World page to this project.

With any text editor, create a file named `hello.scala.html` and save it in the `app/views` directory of this project. Add the following contents to the file:

@[hello-world-page](code/javaguide/hello/hello.scala.html)

This Twirl and HTML markup accomplishes the following:

1. The `@` sign tells the template engine to interpret what follows.
1. In this case, `@main("Hello")` calls the main template, `main.scala.html` and passes it the page title of `"Hello"`.
1. The content section contains the `Hello World` greeting. The main template will insert this into the body of the page.

Now we are ready to add an action method that will render the new page.

## 2. Add an action method

To add an action method for the new page:

Open the `app/controllers/HomeController.java` (or `.scala`) file. Under the tutorial method and before the closing brace, add the following method:

Java
: 
@[hello-world-hello-action](code/javaguide/hello/HelloController.java)

Scala
: 
@[hello-world-hello-action](code/scalaguide/hello/HelloController.scala)

To have Play call the new action method when the browser requests the `hello` page, we need to add a route that maps the page to the method.

## 3. Define a route

To define a route for the new Hello page:

Open the `conf/routes` file and add the following line:

@[hello-world-hello-route](code/routes)

When you add a route to the `routes` file, Play's routes compiler will automatically generate a router class that calls that action using an instance of your controller. For more information see the [routing documentation](https://www.playframework.com/documentation/2.6.x/ScalaRouting#HTTP-routing). By default, the controller instances are created using dependency injection (see docs for [[Java|JavaDependencyInjection]] and [[Scala|ScalaDependencyInjection]]).

You are now ready to test the new page. If you stopped the application for some reason, restart it with the `sbt run` command.

Enter the URL <http://localhost:9000/hello> to view the results of your work. The browser should respond with something like the following:

[[images/hello-page.png]]

## 4. Customize the greeting

As the final part of this tutorial, we'll modify the hello page to accept an HTTP request parameter. The steps include a deliberate mistake to demonstrate how Play provides useful feedback.

To customize the Hello World greeting, follow the instructions below.

In the `app/controllers/HomeController.java` (or `.scala`) file, modify the `hello` action method to accept a name parameter using the following code:

Java
: 
@[hello-world-hello-error-action](code/javaguide/hello/HelloController.java)

Scala
: 
@[hello-world-hello-error-action](code/scalaguide/hello/HelloController.scala)

In the `conf/routes` file, add a `(name: String)` parameter at the end of the `hello`:

@[hello-world-hello-name-route](code/routes)

In Twirl templates, all variables and their types must be declared. In the `app/views/hello.scala.html` file:

1. Insert a new line at the top of the file.
1. On that line, add an @ directive that declares the name parameter and its type: `@(name: String)`
1. To use the variable on the page, change the text in the `<h2>` heading from `Hello World!` to `<h2>Hello @name!</h2>`.

The end result will be:

@[](code/javaguide/hello/helloName.scala.html)

In the browser, enter the following URL and pass in any name as a query parameter to the hello method: <http://localhost:9000/hello?name=MyName>. Play responds with a helpful compilation error that lets you know that the render method in the return value requires a typed parameter:

[[images/hello-error.png]]

To fix the compilation error, modify the `hello` action method in `HomeController` so that the it includes the `name` parameter when rendering the view:

Java
: 
@[hello-world-hello-correct-action](code/javaguide/hello/HelloController.java)

Scala
: 
@[hello-world-hello-correct-action](code/scalaguide/hello/HelloController.scala)

Save the file and refresh the browser. The page should display a customized greeting similar to the following:

[[images/hello-name.png]]

## Summary

Thanks for trying our tutorial. You learned how to use an action method, routes, Twirl template, and input parameter to create a customized Hello World greeting! You experienced how template compilation makes it easier to identify and fix problems and how auto-reloading saves time.

This was just a simple example to get you started. Let's now see other official examples and tutorials from the community.
