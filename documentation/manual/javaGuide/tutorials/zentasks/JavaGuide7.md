# More backend tasks

In this stage of the tutorial we are going to refine our skills at writing backend controllers.  We won't write anymore CoffeeScript, instead we'll use a script that's already been written for us.  You can download this script from here:


Replace `app/assets/javascripts/main.coffee` with this file.

## View tasks in a project

The first thing we'll do is implement the ability to view the tasks in a project.  Currently you can get an overview on the dashboard, but we want the ability to click on a project and see the tasks for that project in detail, as well as the ability to manage that project and its tasks.

Let's start by writing some more templates.  We'll implement an index template for the tasks in `app/views/tasks/index.scala.html`:

```html
dd@(project: Project, tasks: List[Task])
@(project: Project, tasks: List[Task])

<header>
    <hgroup>
        <h1>@project.folder</h1>
        <h2>@project.name</h2>
    </hgroup>
</header>
<article  class="tasks" id="tasks">
    @tasks.groupBy(_.folder).map {
        case (folder, tasks) => {
            @views.html.tasks.folder(folder, tasks)
        }
    }
    <a href="#newFolder" class="new newFolder">New folder</a>
</article>
```

This template breaks the tasks up into folders within the project, and then renders them each in a folders template.  It also provides a new folder button.  Let's implement the `app/views/tasks/folder.scala.html` template:

```html
@(folder: String, tasks: List[Task])

<div class="folder" data-folder-id="@folder">
    <header>
        <input type="checkbox" />
        <h3>@folder</h3>
        <span class="counter"></span>
        <dl class="options">
            <dt>Options</dt>
            <dd>
                <a class="deleteCompleteTasks" href="#">Remove complete tasks</a>
                <a class="deleteAllTasks" href="#">Remove all tasks</a>
                <a class="deleteFolder" href="#">Delete folder</a>
            </dd>
        </dl>
        <span class="loader">Loading</span>
    </header>
    <ul class="list">
        @tasks.map { task =>
            @views.html.tasks.item( task )
        }
    </ul>
</div>
```

The folders template is providing some management options for the folder.  It also renders each task, reusing the item template that we already implemented.

So now we can render our tasks, let's write the action for serving the tasks.  We'll start with a route in `conf/routes`:

    GET     /projects/:project/tasks    controllers.Tasks.index(project: Long)

And now let's create a new controller class, `app/controllers/Task.java`:

```java
package controllers;

import play.*;
import play.mvc.*;
import play.data.*;
import static play.data.Form.*;

import java.util.*;

import models.*;
import views.html.tasks.*;

@Security.Authenticated(Secured.class)
public class Tasks extends Controller {
}
```

As with the projects controller, every method on this controller is protected by the `Secured` authenticator.  Now let's implement the `index` action:

```java
public static Result index(Long project) {
    if(Secured.isMemberOf(project)) {
        return ok(
            index.render(
                Project.find.byId(project),
                Task.findByProject(project)
            )
        );
    } else {
        return forbidden();
    }
}
```

This should look familiar from our earlier work on the projects controller.  This action uses a method we haven't implemented yet on our `Task` model, `findByProject`, so let's add that to `app/models/Task.java`:

```java
public static List<Task> findByProject(Long project) {
    return Task.find.where()
        .eq("project.id", project)
        .findList();
}
```

We're now almost ready, we just need to provide a link to click on to get to this action.  Our new coffee script file that we've downloaded uses the backbone router to handle AJAX requests to pages, so if we create a link using the format `#/some/path`, it will update the page for that.  So we can use the reverse router to render these links for us.  Let's do that now, first in the navigation drawer, in `app/views/projects/item.scala.html`:

```html
...
<li data-project="@project.id">
    <a class="name" href="#@routes.Tasks.index(project.id)">@project.name</a>
    <button class="delete" href="@routes.Projects.delete(project.id)">Delete</button>
...
```

And now in the dashboard, in `app/views/index.scala.html`:

```html
...
    <div class="folder" data-folder-id="@project.id">
        <header>
            <h3><a href="#@routes.Tasks.index(project.id)">@project.name</a></h3>
        </header>
...
```

Now reload the screen, and you should be able to click the projects in the navigation drawer as well as in the dashboard, and you'll see our new tasks index page.

## Adding a task

Let's now implement the ability to add a task.  We'll start off again with the template, we'll need to modify the `app/views/tasks/folder.scala.html` template we added before to include a form for adding tasks.  Place the form after the list of tasks in the folder, and before the folders closing div:

```html
...
    </ul>
    <form class="addTask">
        <input type="hidden" name="folder" value="@folder" />
        <input type="text" name="taskBody" placeholder="New task..." />
        <input type="text" name="dueDate" class="dueDate" placeholder="Due date: mm/dd/yy" />
        <div class="assignedTo">
            <input type="text" name="assignedTo" placeholder="Assign to..." />
        </div>
        <input type="submit" />
    </form>
</div>
```

Now the CoffeeScript code we added before already knows how to handle this form, we just need to implement an endpoint for it to use.  Let's start off by writing the route:

    POST    /projects/:project/tasks    controllers.Tasks.add(project: Long, folder: String)

Notice here that this time, not only do we have the `project` path parameter, but we also have a `folder` parameter, and it isn't specified in the path.  This is the mechanism used to specify praameters that come from the query String.  So this route is saying that it expects POST requests that look something like this `/projects/123/tasks?folder=foo`.

Now let's implement the `add` action in our `app/controllers/Tasks.java` controller:

```java
public static Result add(Long project, String folder) {
    if(Secured.isMemberOf(project)) {
        Form<Task> taskForm = form(Task.class).bindFromRequest();
        if(taskForm.hasErrors()) {
            return badRequest();
        } else {
            return ok(
                item.render(Task.create(taskForm.get(), project, folder))
            );
        }
    } else {
        return forbidden();
    }
}
```

This action binds the request to our `Task` model object, treating it as a form.  It also validates it, but since we haven't defined any validation for the `Task` model, this will always pass.

Since the CoffeeScript that invokes this action is going to do it using the Javascript reverse router, we'll need to add our new route to that.  Add it in `app/controllers/Application.java`:

```java
...
        controllers.routes.javascript.Projects.addGroup(),
        controllers.routes.javascript.Tasks.add()
    )
...
```

Now refresh the page, enter an event name and a valid email address, and hit enter, and you should have created a new task.

### Validation and formatting

In the above action, we validated our task form, but we don't yet have any constraints defined for the task model.  Let's define them now.  Play supports annotation based validation.  Let's make the title field compulsary, add the required annotation to `app/models/Task.java`:

```java
import play.data.validation.*;

...
    @Constraints.Required
    public String title;
```

Additionally, when binding the form, we want to specify the format that the due date should be in.  You can do that using a format annotation:

```java
import play.data.format.*;

...
    @Formats.DateTime(pattern="MM/dd/yy")
    public Date dueDate;
```


