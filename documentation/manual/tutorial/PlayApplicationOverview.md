<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->

# Play Application Overview

This tutorial is implemented as a simple Play application that we can examine to start learning about Play. Let's first look at what happens at runtime. When you enter <http://localhost:9000/> in your browser:

1. The browser requests the root `/` URI from the HTTP server using the `GET` method.
1. The Play internal HTTP Server receives the request.
1. Play resolves the request using the `routes` file, which maps URIs to controller action methods.
1. The action method renders the `index` page, using Twirl templates.
1. The HTTP server returns the response as an HTML page.

At a high level, the flow looks something like this:

[[images/play-request-response.png]]

## Explore the project

Next, let's look at the tutorial project to locate the implementation for:

1. The routes file that maps the request to the controller method.
1. The controller action method that defines how to handle a request to the root URI.
1. The Twirl template that the action method calls to render the HTML markup.

Follow these steps to drill down into the source files:

> **Note:** In the following procedures, for Windows shells, use \ in place of / in path names (no need to change URL path names though).

Using a command window or GUI, look at the contents of the top-level project directory. The following directories contain application components:

1. The `app` subdirectory contains directories for `controllers` and `views`, which will be familiar to those experienced with the Model View Controller (MVC) architecture. Since this simple project does not need an external data repository, it does not contain a `models` directory, but this is where you would add it.
1. The `public` subdirectory contains directories for `images`, `javascripts`, and `stylesheets`.
1. The `conf` directory contains application configuration. For details on the rest of the project's structure see [[Anatomy of a Play Application|Anatomy]].

To locate the controller action method, open `app/controllers/HomeController.java` (or `.scala`) file with your favorite text editor. The `Homecontroller` class includes the `index` action method, as shown below. This is a very simple action method that generate an HTML page from the `index.scala.html` Twirl template file.

Java
: 
@[hello-world-index-action](code/javaguide/hello/HelloController.java)

Scala
: 
@[hello-world-index-action](code/scalaguide/hello/HelloController.scala)

To view the route that maps the browser request to the controller method, open the `conf/routes` file. A route consists of an HTTP method, a path, and an action. This control over the URL schema makes it easy to design clean, human-readable, bookmarkable URLs. The following line maps a GET request for the root URL `/` to the `index` action in `HomeController`:

@[hello-world-index-route](code/routes)

Open `app/views/index.scala.html` with your text editor. The main directive in this file calls the main template `main.scala.html` with the string Welcome to generate the page. You can open `app/views/main.scala.html` to see how a `String` parameter sets the page title.

With this overview of the tutorial application, you are ready to add a "Hello World" greeting.