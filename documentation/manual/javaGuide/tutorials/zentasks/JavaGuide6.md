# Invoking actions from Javascript

In the last the last chapter of the tutorial, we implemented a number of new actions that are going to be the backend for the navigation drawer.  In this chapter, we'll add the client side code necessary to complete the behavior for the navigation drawer.

## Javascript routes

The first thing we need to do is implement a Javascript router.  While you could always just make AJAX calls using hard coded URLs, Play provides a client side router that will build these URLs and make the AJAX requests for you.  Building URLs to make AJAX calls can be quite fragile, and if you change your URL structure or parameter names at all, it can be easy to miss things when you update your Javascript code.  For this reason, Play has a Javascript router, that lets us call actions on the server, from Javascript, as if we were invoking them directly.

A Javascript router needs to be generated from our code, to say what actions it should include.  It can be implemented as a regular action that your client side code can download using a script tag.  Alternatively Play has support for embedding the router in a template, but for now we'll just use the action method.  Write a Javascript router action in `app/controllers/Application.java`:

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

We've set the response content type to be `text/javascript`, because the router will be a Javascript file.  Then we've used `Routes.javascriptRouter` to generate the routes.  The first parameter that we've passed to it is `jsRoutes`, this means the router will be bound to the global variable by that name, so in our Javascript/CoffeeScript code, we'll be able to access the router using that variable name.  Then we've passed the list of actions that we want in the router.

Of course, we need to add a route for that in the `conf/routes` file:

    GET     /assets/javascripts/routes          controllers.Application.javascriptRoutes()

Now before we implement the client side code, we need to source all the javascript dependencies that we're going to need in the `app/views/main.scala.html`:

```html
<script type="text/javascript" src="@routes.Assets.at("javascripts/jquery-1.7.1.js")"></script>
<script type="text/javascript" src="@routes.Assets.at("javascripts/jquery-play-1.7.1.js")"></script>
<script type="text/javascript" src="@routes.Assets.at("javascripts/underscore-min.js")"></script>
<script type="text/javascript" src="@routes.Assets.at("javascripts/backbone-min.js")"></script>
<script type="text/javascript" src="@routes.Assets.at("javascripts/main.js")"></script>
<script type="text/javascript" src="@routes.Application.javascriptRoutes()"></script>
```

## CoffeeScript

We are going to implement the client side code using CoffeeScript.  CoffeeScript is a nicer to use Javascript, it compiles to Javascript and is fully interoperable with Javascript, so we can use our Javascript router for example from it.  We could use Javascript, but since Play comes built in with a CoffeeScript compiler, we're going to use that instead.  When we added the Javascript dependencies to `main.scala.html`, we added a dependency on `main.js`.  This doesn't exist yet, it is going to be the artifact that will be produced from the CoffeeScript compilation.  Let's implement it now, open `app/assets/javascripts/main.coffee`:

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

Now the code that you see above may be a little overwhelming to you.  The first block of code activates all the option icons in the page, it's straight forward jquery.  The second is an extension to jquery that we'll use a bit later, that turns a span into one that can be edited in place.  These are just some utility methods that we are going to need to help with writing the rest of the logic.

Let's start to write our Backbone views:

```coffeescript
class Drawer extends Backbone.View

$ -> 
    drawer = new Drawer el: $("#projects")
```

We've now bound our drawer, which has an id of `projects`, to a Backbone view called `Drawer`.  But we haven't done anything useful yet.  In the initialize function of the drawer, let's bind all the groups to a new `Group` class, and all the projects in each group to a new `Project` class:

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

Now we'll add some behavior to the groups.  Let's first add a toggle function to the group, so that we can hide and display the group:

```coffeescript
class Group extends Backbone.View
    events:
        "click    .toggle"          : "toggle"
    toggle: (e) ->
        e.preventDefault()
        @el.toggleClass("closed")
        false
```

Earlier when we created our groups template, we added some buttons, including a new project button.  Let's bind a click event to that:

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

Now you can see that are are using the `jsRoutes` Javascript router that we created before.  It almast looks like we are just making an ordinary call to the `Projects.add` action.  Invoking this actually returns an object that gives us a method for making ajax requests, as well as the ability to get the URL and method for the action.  But, this time you can see we are invoking the `ajax` method, passing in the group name as part of the `data`, and then passing `success` and `error` callbacks.  In fact, the `ajax` method just delegates straight to jQuery's `ajax` method, supplying the URL and the method along the way, so anything you can do with jQuery, you can do here.

> You don't have to use jQuery with the Javascript router, it's just the default implementation that Play provides.  You could use anything, by supplying your own ajax function name to call to the Javascript router when you generate it.

Now if you refresh the page, you should be able to create a new project.  However, the new projects name is "New Project", not really what we want.  Let's implement the functionality to rename it:

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

First we've declared the name of our project to be editable in place, using the helper function we added earlier, and passing the `renameProject` method as the callback.  In our `renameProject` method, we've again used the Javascript router, this time passing a parameter, the id of the project that we are to rename.  Try it out now to see if you can rename a project, by double clicking on the project.

The last thing we want to implement for projects is the remove method, binding to the remove button.  Add the following CoffeeScript to the `Project` backbone class:

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

As one final task that we'll do is add a new group button to the main template, and implement the logic for it.  So let's add a new group button to the `app/views/main.scala.html` template, just before the closing `</nav>` tag:

```html
    </ul>
    <button id="newGroup">New group</button>
</nav>
```

Now add an `addGroup` method to the `Drawer` class, and some code to the `initialize` method that binds clicking the `newGroup` button to it:

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

## Testing our client side code

Although we've tested manually that things are working, Javascript apps can be quite fragile, and easy to break in future.  Play provides a very simple mechanism for testing client side code using [FluentLenium](https://github.com/FluentLenium/FluentLenium).  FluentLenium provides a simple way to represent your pages and the components on them in a way that is reusable, and let's you interact with them and make assertions on them.

### Implementing page objects

Let's start by creating a page that represents our login page.  Open `test/pages/Login.java`:

```java
package pages;

import org.fluentlenium.core.*;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.fluentlenium.FluentLeniumAssertions.assertThat;
import static org.fluentlenium.core.filter.FilterConstructor.*;

import components.*;
import controllers.*;

public class Login extends FluentPage {
    public String getUrl() {
        return routes.Application.login().url();
    }

    public void isAt() {
        assertThat(find("h1", withText("Sign in"))).hasSize(1);
    }

    public void login(String email, String password) {
        fill("input").with(email, password);
        click("button");
    }
}
```

You can ee three methods here.  Firstly, we've declared the URL of our page, conveniently using the reverse router to get this.  Then we've implemented an `isAt` method, this runs some assertions on the page to make sure that we are at this page.  FluentLenium will use this when we go to the page to make sure everything is as expected.  We've written a simple assertion here to ensure that the heading is the login page heading.  Finally, we've implemented an action on the page, which fills the login form with the users email and password, and then clicks the login button.

> You can read more about FluentLenium and the APIs it provides [here](https://github.com/FluentLenium/FluentLenium). We won't go into any more details in this tutorial.

Now that we can log in, let's create a page that represents the dashboard in `test/pages/Dashboard.java`:

```java
package pages;

import org.fluentlenium.core.*;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.fluentlenium.FluentLeniumAssertions.assertThat;
import static org.fluentlenium.core.filter.FilterConstructor.*;

import components.*;

public class Dashboard extends FluentPage {
    public String getUrl() {
        return "/";
    }

    public void isAt() {
        assertThat(find("h1", withText("Dashboard"))).hasSize(1);
    }

    public Drawer drawer() {
        return Drawer.from(this);
    }
}
```

It is similarly simple, like the login page.  Eventually we will add more functionality to this page, but for now since we're only testing the drawer, we just provide a method to get the drawer.  Let's see how implementation of the drawer, in `test/components/Drawer.java`:

```java
ackage components;

import java.util.*;

import org.fluentlenium.core.*;
import org.fluentlenium.core.domain.*;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.fluentlenium.FluentLeniumAssertions.assertThat;
import static org.fluentlenium.core.filter.FilterConstructor.*;

public class Drawer {

    private final FluentWebElement element;

    public Drawer(FluentWebElement element) {
        this.element = element;
    }

    public static Drawer from(Fluent fluent) {
        return new Drawer(fluent.findFirst("nav"));
    }

    public DrawerGroup group(String name) {
        return new DrawerGroup(element.findFirst("#projects > li[data-group=" + name + "]"));
    }
}
```

Since our drawer is not a page, but rather is a component of a page, we haven't extended `FluentPage` this time, rather we are simply wrapping a `FluentWebElement`, this is the `<nav>` element that the drawer lives in.  We've provided a method to look up a group by name, let's see the implementation of the group now, open `test/components/DrawerGroup.java`:

```java
package components;

import org.fluentlenium.core.*;
import org.fluentlenium.core.domain.*;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.fluentlenium.FluentLeniumAssertions.assertThat;
import static org.fluentlenium.core.filter.FilterConstructor.*;
import java.util.*;
import com.google.common.base.Predicate;

public class DrawerGroup {
    private final FluentWebElement element;

    public DrawerGroup(FluentWebElement element) {
        this.element = element;
    }

    public List<DrawerProject> projects() {
        List<DrawerProject> projects = new ArrayList<DrawerProject>();
        for (FluentWebElement e: (Iterable<FluentWebElement>) element.find("ul > li")) {
            projects.add(new DrawerProject(e));
        }
        return projects;
    }

    public DrawerProject project(String name) {
        for (DrawerProject p: projects()) {
            if (p.name().equals(name)) {
                return p;
            }
        }
        return null;
    }

    public Predicate hasProject(final String name) {
        return new Predicate() {
            public boolean apply(Object o) {
                return project(name) != null;
            }
        };
    }

    public void newProject() {
        element.findFirst(".newProject").click();
    }
}
```

Like with `Drawer`, we have a method for looking up a project by name.  We've also provided a method for checking if a project with a particular name exists, we've used `Predicate` to capture this, which will make it easy for us later when we tell FluentLenium to wait until certain conditions are true.

Finally, the last component of our model that we'll build out is `test/componenst/DrawerProject.java`:

```java
package components;

import org.fluentlenium.core.*;
import org.fluentlenium.core.domain.*;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.fluentlenium.FluentLeniumAssertions.assertThat;
import static org.fluentlenium.core.filter.FilterConstructor.*;
import org.openqa.selenium.Keys;
import com.google.common.base.Predicate;

public class DrawerProject {
    private final FluentWebElement element;

    public DrawerProject(FluentWebElement element) {
        this.element = element;
    }

    public String name() {
        FluentWebElement a = element.findFirst("a.name");
        if (a.isDisplayed()) {
            return a.getText().trim();
        } else {
            return element.findFirst("input").getValue().trim();
        }
    }

    public void rename(String newName) {
        element.findFirst(".name").doubleClick();
        element.findFirst("input").text(newName);
        element.click();
    }

    public Predicate isInEdit() {
        return new Predicate() {
            public boolean apply(Object o) {
                return element.findFirst("input") != null && element.findFirst("input").isDisplayed();
            }
        };
    }
```

The `DrawerProject` allows us to lookup the name of the project, rename the project, and has a predicate for checking if the project name is in edit mode.  So, it's been a bit of work to get this far with our selenium tests, and we haven't written any tests yet!  The great thing is though, all these components and pages are going to be reusable from all of our selenium tests, so when something about our markup changes, we can just update these components, and all the tests will still work.

### Implementing the tests

Open `test/views/DrawerTest.java`, and add the following setup code:

```java
package views;

import org.junit.*;

import play.test.*;
import static play.test.Helpers.*;

import org.fluentlenium.core.*;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.fluentlenium.FluentLeniumAssertions.assertThat;
import static org.fluentlenium.core.filter.FilterConstructor.*;
import com.google.common.base.*;

import pages.*;
import components.*;

public class DrawerTest extends WithBrowser {

    public Drawer drawer;

    @Before
    public void setUp() {
        start();
        Login login = browser.createPage(Login.class);
        login.go();
        login.login("bob@example.com", "secret");
        drawer = browser.createPage(Dashboard.class).drawer();
    }
}
```

This time we've made our test case extend `WithBrowser`, which gives us a mock web browser to work with.  The default browser is HtmlUnit, a Java based headless browser, but you can also use other browmsers, such as Firefox and Chrome. In our `setUp` method we've called `start()`, which starts both the browser and the server.  We've then created a login page, navigated to it, and logged in, and finally, we've created a dashboard page and retrieved the drawer from it. We're now ready to write our first test case:

```java
    @Test
    public void newProject() throws Exception {
        drawer.group("Personal").newProject();
        dashboard.await().until(drawer.group("Personal").hasProject("New project"));
        dashboard.await().until(drawer.group("Personal").project("New project").isInEdit());
    }
```

Here we're testing new project creation.  We get the `Personal` group, using the methods we've already defined on drawer, and you can see we're using those predicates we wrote for testing if a group has a project, and if it's in edit mode (as it should be after we've created it).  Let's write another test, this time testing the rename functionality:

```java
    @Test
    public void renameProject() throws Exception {
        drawer.group("Personal").project("Private").rename("Confidential");
        dashboard.await().until(Predicates.not(drawer.group("Personal").hasProject("Private")));
        dashboard.await().until(drawer.group("Personal").hasProject("Confidential"));
        dashboard.await().until(Predicates.not(drawer.group("Personal").project("Confidential").isInEdit()));
    }
```

We rename a project, and then wait for it to disappear, wait for the new one to appear, and then ensure that the new one is not in edit mode.  In this tutorial we're going to leave the client side tests there, but you can try now to implement your own tests for deleting projects and adding new groups.

So now we have a working and tested navigation drawer.  We've seen how to implement a few more actions, how the Javascript router works, and to use CoffeeScript in our Play application, and how to use the Javascript router from our CoffeeScript code.  We've also seen how we can use the page object pattern to write tests for a client side code running in a headless browser.  There are a few functions we haven't implemented yet, these include renaming a group and deleting a group.  You could try implementing them on your own, or check the code in the ZenTasks sample app to see how it's done.

Commit your work to git.

> Go to the [next part](JavaGuide7)
