# Adding some AJAX actions

Now that we can log in, let's start writing functionality for our application.  We'll start simple, by adding dynamic
functionality to the navigation drawer, that is, the sidebar with the list of projects.

To implement the client side logic, we are going to use CoffeeScript, a language that makes Javascript very simple and
easy to work with.  We could just as easily use JavasSript, but Play comes with a build in CoffeeScript compiler, so
we'll see how we can utilise that.

Additionally, we'll use Backbone.js to manage our views.  In contrast to a typical Backbone app, where your models live
on the client side, we'll keep our models all on the server side, and just use Backbone views, binding them to the views
rendered by Plays scala templates.  This allows us to use Plays templating system, and also means that our application
will be much easier to make work in browsers that don't support Javascript, be crawled by search engines, etc.

## The Project controller

Let's start with the backend.  We're going to add a few new backend actions, specifically to:

* Add a project
* Rename a project
* Delete a project
* Add a group

To start, create a controller class called `app/controllers/Projects.java`:

```java
package controllers;

import play.*;
import play.mvc.*;
import play.data.*;
import static play.data.Form.*;
import java.util.*;
import models.*;
import views.html.*;
import views.html.projects.*;

@Security.Authenticated(Secured.class)
public class Projects extends Controller {

}
```

An important thing to notice here is that we have annotated the entire class with our security authenticator.  With the
dashboard, we just annotated the method, but these annotations can also be placed at the class level to say that every
method in this class must use this action.  This can save us a lot of boiler plate code, and also saves us from
accidentally forgetting to annotate a method.

Now let's add a method to create a new project:

```java
 public static Result add() {
    Project newProject = Project.create(
        "New project",
        form().bindFromRequest().get("group"),
        request().username()
    );
    return ok(item.render(newProject));
}
```

We've used our existing `create` method on our `Project` model to create the new project, owned by the currently logged
in user, which is returned by `request().username()`.

Also notice that we are reusing that `item` template that we created earlier to render the new project.  Now you'll be
begin to see why we created our templates in the structure that we did earlier.  This method only renders a small part
of the page, that's ok, we'll be using this fragment from an AJAX action.

Let's now add a method to rename a project, but before we do, let's consider the security requirements of this function.
A user should only be allowed to rename a project if they are a member of that project.  Let's write a utility method in
our `app/controllers/Secured.java` class that checks this:

```java
public static boolean isMemberOf(Long project) {
    return Project.isMember(
        project,
        Context.current().request().username()
    );
}
```

You may notice here that we've used `Context.current()` to get the `request()`.  This is a convenient way to get access
to a request if you aren't in an action.  Underneath, it uses thread locals to find the current request, response,
session and so on.

Our `isMemberOf` method has used a new method that we haven't written on our `Project` model yet.  In fact we are going
to need a few new methods on the `Project` object, so let's open `app/models/Project.java` now to add them:

```java
public static boolean isMember(Long project, String user) {
    return find.where()
        .eq("members.email", user)
        .eq("id", project)
        .findRowCount() > 0;
}

public static String rename(Long projectId, String newName) {
    Project project = find.ref(projectId);
    project.name = newName;
    project.update();
    return newName;
}
```

Having added a `rename` method to our `Project` model, we are now ready to implement our action in
`app/controllers/Projects.java`:

```java
public static Result rename(Long project) {
    if (Secured.isMemberOf(project)) {
        return ok(
            Project.rename(
                project,
                form().bindFromRequest().get("name")
            )
        );
    } else {
        return forbidden();
    }
}
```

Notice that first we check that the current user is a member of the project, and if they aren't, we return them a
`forbidden` response.  Also notice our use of the `form()` method.  We've seen this before, when we were populating and
validating our login form.  However this time, we haven't passed in a bean to decode the form into and to validate it
with.  Rather, we've used what's called a dynamic form.  A dynamic just parses a form submission into a map of string
keys to string values, and is very convenient for simple form submissions with only one or two values where you don't
want to do any validation. 

Let's move on to our method to delete a project:

```java
public static Result delete(Long project) {
    if(Secured.isMemberOf(project)) {
        Project.find.ref(project).delete();
        return ok();
    } else {
        return forbidden();
    }
}
```

And finally, let's add a method to create a group:

```java
public static Result addGroup() {
    return ok(
        group.render("New group", new ArrayList<Project>())
    );
}
```

Now that we have our controller methods implemented, let's add routes to these controllers in `conf/routes`:

    POST    /projects                   controllers.Projects.add()
    POST    /projects/groups            controllers.Projects.addGroup()
    DELETE  /projects/:project          controllers.Projects.delete(project: Long)
    PUT     /projects/:project          controllers.Projects.rename(project: Long)

Now do a quick refresh of the application in the browser, to make sure there are no compile errors.

## Javascript routes

Now we need to write some code to use our new actions.  We'll be calling our code from CoffeeScript code on the client
side, but before we get to doing that, Play has a nice little feature that will help us to do that.  Building URLs to
make AJAX calls can be quite fragile, and if you change your URL structure or parameter names at all, it can be easy to
miss things when you update your Javascript code.  For this reason, Play has a Javascript router, that lets us call
actions on the server, from Javascript, as if we were invoking them directly.

A Javascript router needs to be generated from our code, to say what actions it should include.  It can be implemented
as a regular action that your client side code can download using a script tag.  Alternatively Play has support for
embedding the router in a template, but for now we'll just use the action method.  Write a Javascript router action in
`app/controllers/Application.java`:

```java
public static Result javascriptRoutes() {
    response().setContentType("text/javascript");
    return ok(
        Routes.javascriptRouter("jsRoutes",
            controllers.routes.javascript.Projects.add(),
            controllers.routes.javascript.Projects.delete(),
            controllers.routes.javascript.Projects.rename(),
            controllers.routes.javascript.Projects.addGroup()
        )
    );
}
```

We've set the response content type to be `text/javascript`, because the router will be a Javascript file.  Then we've
used `Routes.javascriptRouter` to generate the routes.  The first parameter that we've passed to it is `jsRoutes`, this
means the router will be bound to the global variable by that name, so in our Javascript/CoffeeScript code, we'll be
able to access the router using that variable name.  Then we've passed the list of actions that we want in the router.

Of course, we need to add a route for that in the `conf/routes` file:

    GET     /assets/javascripts/routes          controllers.Application.javascriptRoutes()

Now before we implement the client side code, we need to source all the javascript dependencies that we're going to need
in the `app/views/main.scala.html`:

```html
<script type="text/javascript" src="@routes.Assets.at("javascripts/jquery-1.7.1.js")"></script>
<script type="text/javascript" src="@routes.Assets.at("javascripts/jquery-play-1.7.1.js")"></script>
<script type="text/javascript" src="@routes.Assets.at("javascripts/underscore-min.js")"></script>
<script type="text/javascript" src="@routes.Assets.at("javascripts/backbone-min.js")"></script>
<script type="text/javascript" src="@routes.Assets.at("javascripts/main.js")"></script>
<script type="text/javascript" src="@routes.Application.javascriptRoutes()"></script>
```

## CoffeeScript

As we said before, we are going to implement the client side code using CoffeeScript.  CoffeeScript is a language that
compiles to Javascript, and Play compiles it automatically for us.  When we added the Javascript dependencies to
`main.scala.html`, we added a dependency on `main.js`.  This doesn't exist yet, it is going to be the artifact that will
be produced from the CoffeeScript compilation.  Let's implement it now, open `app/assets/javascripts/main.coffee`:

```coffeescript
$(".options dt, .users dt").live "click", (e) ->
    e.preventDefault()
    if $(e.target).parent().hasClass("opened")
        $(e.target).parent().removeClass("opened")
    else
        $(e.target).parent().addClass("opened")
        $(document).one "click", ->
            $(e.target).parent().removeClass("opened")
    false

$.fn.editInPlace = (method, options...) ->
    this.each ->
        methods = 
            # public methods
            init: (options) ->
                valid = (e) =>
                    newValue = @input.val()
                    options.onChange.call(options.context, newValue)
                cancel = (e) =>
                    @el.show()
                    @input.hide()
                @el = $(this).dblclick(methods.edit)
                @input = $("<input type='text' />")
                    .insertBefore(@el)
                    .keyup (e) ->
                        switch(e.keyCode)
                            # Enter key
                            when 13 then $(this).blur()
                            # Escape key
                            when 27 then cancel(e)
                    .blur(valid)
                    .hide()
            edit: ->
                @input
                    .val(@el.text())
                    .show()
                    .focus()
                    .select()
                @el.hide()
            close: (newName) ->
                @el.text(newName).show()
                @input.hide()
        # jQuery approach: http://docs.jquery.com/Plugins/Authoring
        if ( methods[method] )
            return methods[ method ].apply(this, options)
        else if (typeof method == 'object')
            return methods.init.call(this, method)
        else
            $.error("Method " + method + " does not exist.")
```

Now the code that you see above may be a little overwhelming to you.  The first block of code activates all the option
icons in the page, it's straight forward jquery.  The second is an extension to jquery that we'll use a bit later, that
turns a span into one that can be edited in place.  These are just some utility methods that we are going to need to
help with writing the rest of the logic.

Let's start to write our Backbone views:

```coffeescript
class Drawer extends Backbone.View

$ -> 
    drawer = new Drawer el: $("#projects")
```

We've now bound our drawer, which has an id of `projects`, to a Backbone view called `Drawer`.  But we haven't done
anything useful yet.  In the initialize function of the drawer, let's bind all the groups to a new `Group` class, and
all the projects in each group to a new `Project` class:

```coffeescript
class Drawer extends Backbone.View
    initialize: ->
        @el.children("li").each (i,group) ->
            new Group
                el: $(group)
            $("li",group).each (i,project) ->
                new Project
                    el: $(project)

class Group extends Backbone.View

class Project extends Backbone.View
```

Now we'll add some behavior to the groups.  Let's first add a toggle function to the group, so that we can hide and
display the group:

```coffeescript
class Group extends Backbone.View
    events:
        "click    .toggle"          : "toggle"
    toggle: (e) ->
        e.preventDefault()
        @el.toggleClass("closed")
        false
```

Earlier when we created our groups template, we added some buttons, including a new project button.  Let's bind a click
event to that:

```coffeescript
class Group extends Backbone.View
    events:
        "click    .toggle"          : "toggle"
        "click    .newProject"      : "newProject"
    newProject: (e) ->
            @el.removeClass("closed")
        jsRoutes.controllers.Projects.add().ajax
            context: this
            data:
                group: @el.attr("data-group")
            success: (tpl) ->
                _list = $("ul",@el)
                _view = new Project
                    el: $(tpl).appendTo(_list)
                _view.el.find(".name").editInPlace("edit")
            error: (err) ->
                $.error("Error: " + err)
```

Now you can see that are are using the `jsRoutes` Javascript router that we created before.  It almast looks like we are
just making an ordinary call to the `Projects.add` action.  Invoking this actually returns an object that gives us a
method for making ajax requests, as well as the ability to get the URL and method for the action.  But, this time you
can see we are invoking the `ajax` method, passing in the group name as part of the `data`, and then passing `success`
and `error` callbacks.  In fact, the `ajax` method just delegates straight to jQuery's `ajax` method, supplying the URL
and the method along the way, so anything you can do with jQuery, you can do here.

> You don't have to use jQuery with the Javascript router, it's just the default implementation that Play provides.  You
> could use anything, by supplying your own ajax function name to call to the Javascript router when you generate it.

Now if you refresh the page, you should be able to create a new project.  However, the new projects name is "New
Project", not really what we want.  Let's implement the functionality to rename it:

```coffeescript
class Project extends Backbone.View
    initialize: ->
        @id = @el.attr("data-project")
        @name = $(".name", @el).editInPlace
            context: this
            onChange: @renameProject
    renameProject: (name) ->
        @loading(true)
        jsRoutes.controllers.Projects.rename(@id).ajax
            context: this
            data:
                name: name
            success: (data) ->
                @loading(false)
                @name.editInPlace("close", data)
            error: (err) ->
                @loading(false)
                $.error("Error: " + err)
    loading: (display) ->
        if (display)
            @el.children(".delete").hide()
            @el.children(".loader").show()
        else
            @el.children(".delete").show()
            @el.children(".loader").hide()
```

First we've declared the name of our project to be editable in place, using the helper function we added earlier, and
passing the `renameProject` method as the callback.  In our `renameProject` method, we've again used the Javascript
router, this time passing a parameter, the id of the project that we are to rename.  Try it out now to see if you can
rename a project, by double clicking on the project.

The last thing we want to implement for projects is the remove method, binding to the remove button.  Add the following
CoffeeScript to the `Project` backbone class:

```coffeescript
    events:
        "click      .delete"        : "deleteProject"
    deleteProject: (e) ->
        e.preventDefault()
        @loading(true)
        jsRoutes.controllers.Projects.delete(@id).ajax
            context: this
            success: ->
                @el.remove()
                @loading(false)
            error: (err) ->
                @loading(false)
                $.error("Error: " + err)
        false
```

Once again, we are using the Javascript router to invoke the delete method on the `Projects` controller.

As one final task that we'll do is add a new group button to the main template, and implement the logic for it.  So
let's add a new group button to the `app/views/main.scala.html` template, just before the closing `</nav>` tag:

```html
    </ul>
    <button id="newGroup">New group</button>
</nav>
```

Now add an `addGroup` method to the `Drawer` class, and some code to the `initialize` method that binds clicking the
`newGroup` button to it:

```coffeescript
class Drawer extends Backbone.View
    initialize: ->
        ...
        $("#newGroup").click @addGroup
    addGroup: ->
        jsRoutes.controllers.Projects.addGroup().ajax
            success: (data) ->
                _view = new Group
                    el: $(data).appendTo("#projects")
                _view.el.find(".groupName").editInPlace("edit")
```

Try it out, you can now create a new group.

We now have a working navigation drawer.  We've seen how to implement a few more actions, how the Javascript router
works, and to use CoffeeScript in our Play application, and how to use the Javascript router from our CoffeeScript code.
There are a few functions we haven't implemented yet, these include renaming a group and deleting a group.  You could
try implementing them on your own, or check the code in the ZenTasks sample app to see how it's done.

Commit your work to git.

> Go to the [next part](JavaGuide6)
