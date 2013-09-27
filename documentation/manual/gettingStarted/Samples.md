# Sample applications

The Play binary package comes with a comprehensive set of sample applications written in both Java and Scala. This is a very good place to look for code snippets and examples.

> The sample applications are available in the `samples/` directory of your Play installation.

## Hello world

[[images/helloworld.png]]

This is a very basic application that demonstrates Play fundamentals:

- Writing controllers and actions.
- Routing and reverse routing.
- Linking to public assets.
- Using the template engine.
- Handling forms with validation.

## Computer database

[[images/computerdatabase.png]]

This is a classic CRUD application, backed by a JDBC database. It demonstrates:

- accessing a JDBC database, using Ebean in Java and Anorm in Scala
- table pagination and CRUD forms
- integrating with a CSS framework ([Twitter Bootstrap](http://twitter.github.com/bootstrap/)).

Twitter Bootstrap requires a different form layout to the default layout provided by the Play form helper, so this application also provides an example of integrating a custom form input constructor.

## Forms

[[images/forms.png]]

This is a dummy application presenting several typical form usages. It demonstrates: 

- writing complex forms with validation
- handling forms with dynamically repeated values.

## ZenTasks

[[images/zentask.png]]

This advanced todo list demonstrates a modern Ajax-based web application. This is a work in progress, and we plan to add features in the future releases. For now you can check it out to learn how to:

- integrate authentication and security
- use Ajax and JavaScript reverse routing
- integrate with compiled assets - LESS CSS and CoffeeScript.

## CometClock

[[images/comet-clock.png]]

This a very simple Comet demonstration pushing clock events from the server to the Web browser using a the forever-frame technique. It demonstrates how to:

- create a Comet connection
- use Akka actors (in the Java version)
- write custom Enumerators (in the Scala version).

## WebSocket chat

[[images/websocket-chat.png]]

This application is a chat room, built using WebSockets. Additionally, there is a bot used that talks in the same chat room. It demonstrates:

- WebSocket connections
- advanced Akka usage.

## Comet monitoring

[[images/rps-screenshot.png]]

This mobile web application monitors Play server performance. It demonstrates:

- advanced usage of Enumerators and Enumeratees.

&nbsp;

> **Next:** 
>
> – [[Play for Scala developers | ScalaHome]]
> – [[Play for Java developers | JavaHome]]
