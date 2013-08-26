# A first iteration for the data model

Here we will start to write the model for our blog engine.

## Introduction to Ebean

The model layer has a central position in a play application (and in fact in all well designed applications).  It is the demain-specific representation of the information on which the application operates.  As we want to create a task management system, the model layer will contain classes like `User`, `Project` and `Task`.

Because most model objects need to survive between application restarts, we have to save them in a persistent datastore. A common choice is to use a relational database.  But because Java is an object oriented language, we will use an **Object Relational Mapper** to help reduce the impedance mismatch.

> Though Play does come with support for relational databases out of the box, there is nothing stopping you from using Play framework with a NoSQL database, in fact, this is a very common way to implement models in Play framework. However we will use a relational database for this tutorial.

Ebean is a Java ORM library that aims to implement a very simple interface to mapping Java objects to the database.  It uses JPA annotations for mapping classes to tables, but if you have had experience with JPA before, Ebean differs in that it is sessionless.  This can greatly simplify the way you interact with the database, removing many of the surprises of things being done at odd times, such as session flushing, and errors with regards to stale or detached objects, that can occur when using JPA.

## Starting with the User class

We will start to code ZenTasks by creating the `User` class.  Create a new file called `app/models/User.java`, and declare a first implementation of the `User` class:

```java
package models;

import javax.persistence.*;
import play.db.ebean.*;
import com.avaje.ebean.*;

@Entity
public class User extends Model {

    @Id
    public String email;
    public String name;
    public String password;
    
    public User(String email, String name, String password) {
      this.email = email;
      this.name = name;
      this.password = password;
    }

    public static Finder<String,User> find = new Finder<String,User>(
        String.class, User.class
    ); 
}
```

The `@Entity` annotation marks this class as a managed Ebean entity, and the `Model` superclass automatically provides a set of useful JPA helpers that we will discover later.  All fields of this class will be automatically persisted to the database.

> It's not required that your model objects extend the `play.db.ebean.Model` class.  You can use plain Ebean as well. But extending this class is a good choice in most cases as it will make a lot of the Ebean stuff easier.

If you have used JPA before, you know that every JPA entity must provide an `@Id` property.  In this case, we are choosing `email` to be the id field.

The `find` field will be used to programatically make queries, which we will see later.

Now if you're a Java developer with any experience at all, warning sirens are probably clanging like mad at the sight of a public variable.  In Java (as in other object-oriented languages), best practice says to make all fields private and provide accessors and mutators.  This is to promote encapsulation, a concept critical to object oriented design.  In fact, play takes care of that for you and automatically generates getters and setters while preserving encapsulation; we will see how it works later in this tutorial.

You can now refresh the application homepage.  This time you should see something different:

[[images/evolution.png]]

Play has automatically detected that you've added a new model, and has generated a new **evolution** for it.  An evolution is an SQL script that migrates the database schema from one state to the next in your application.  In our case, our database state is empty, and Play has generated scripts that create the tables.  For now during the early stages of development, we will let Play to continue to generate these scripts for us.  Later on in the project lifecycle, you will switch to writing them yourself.  Each time you see this message, you can safe click the apply button.

> If you don't want to have to worry about applying evolutions each time you restart play, you can disable this prompting by adding the argument `-DapplyEvolutions.default=true` when you run the `play` command.

## Writing the first test

A good way to test the newly created `User` class is to write a JUnit test case.  It will allow you to incrementally complete the application model and ensure that all is fine.

Create a new file called `test/models/ModelsTest.java`.  We will start off by setting up the application, with an in memory database, ready to write and run our tests:

```java
package models;

import models.*;
import org.junit.*;
import static org.junit.Assert.*;
import play.test.WithApplication;
import static play.test.Helpers.*;

public class ModelsTest extends WithApplication {
    @Before
    public void setUp() {
        start(fakeApplication(inMemoryDatabase()));
    }
}
```

We have extended the `WithApplication` class.  This is optional, it provides the `start()` method that allows us to easily start a fake application, and it automatically cleans it up after each test has run.  You could manage these yourself, but we are going to let Play manage it for us. 

We have also implemented a `@Before` annotated method.  This annotation means that this method will be run before each test.  In our case we are starting a new `FakeApplication`, and configuring this application to use a new in memory database.  Because we are using an in memory database, we don't need to worry about clearing the database before each test, since a new clean database is created for us before each test.

Now we will write our first test, which is just going to check that we can insert a row, and retrieve it again:

```java
    @Test
    public void createAndRetrieveUser() {
        new User("bob@gmail.com", "Bob", "secret").save();
        User bob = User.find.where().eq("email", "bob@gmail.com").findUnique();
        assertNotNull(bob);
        assertEquals("Bob", bob.name);
    }
```

You can see that we have programatically created a query using the `User.find` finder, to find a unqiue instance where `email` is equal to Bob's email address.

To run this test case, make sure that you have stopped the running application by pressing Ctrl+D in the play console, and then run `test`.  The test should pass.

Although we could use the find object from anywhere in our code to create queries for users, it's not good practice to spread that code all through our application.  One such query that we need is a query that will authenticate users.  In `User.java`, add the `authenticate()` method:

```java
    public static User authenticate(String email, String password) {
        return find.where().eq("email", email)
            .eq("password", password).findUnique();
    }
```

And now the test case:

```java
    @Test
    public void tryAuthenticateUser() {
        new User("bob@gmail.com", "Bob", "secret").save();
        
        assertNotNull(User.authenticate("bob@gmail.com", "secret"));
        assertNull(User.authenticate("bob@gmail.com", "badpassword"));
        assertNull(User.authenticate("tom@gmail.com", "secret"));
    }
```

Each time you make a modification you can run all the tests from the play test runner to make sure you didn't break anything.

> The above authentication code stores the password in cleartext.  This is considered very bad practice, you should hash the password before storing it, and then hash it before running the query, but that is beyond the scope of this tutorial.

## The Project class

The `Project` class will represent projects that tasks can be a part of.  A project also has a list of members that can be assigned to tasks in the project.  Let's do a first implementation:

```java
package models;

import java.util.*;
import javax.persistence.*;
import play.db.ebean.*;

@Entity
public class Project extends Model {

    @Id
    public Long id;
    public String name;
    public String folder;
    @ManyToMany(cascade = CascadeType.REMOVE)
    public List<User> members = new ArrayList<User>();

    public Project(String name, String folder, User owner) {
        this.name = name;
        this.folder = folder;
        this.members.add(owner);
    }

    public static Model.Finder<Long,Project> find = new Model.Finder(Long.class, Project.class);

    public static Project create(String name, String folder, String owner) {
        Project project = new Project(name, folder, User.find.ref(owner));
        project.save();
        project.saveManyToManyAssociations("members");
        return project;
    }

    public static List<Project> findInvolving(String user) {
        return find.where()
            .eq("members.email", user)
            .findList();
    }
}
```

A project has a name, a folder that it belongs to, and members.  This time you can see that we again have the `@Entity` annotation on the class, extending `Model`, `@Id` on our `id` field and `find` for running queries.  We have also declared a relation with the `User` class, declaring it as `@ManyToMany`.  This means that each user can be member of many projects, and each project can have many users.

We have also implemented a create method.  Note that the many to many `members` association has to be saved explicitly. Note also that we never actually assign the `id` property.  This is because we are going to let the database generate an id for us.

Finally we have implemented another query method, one that finds all projects involving a particular user.  You can see how the dot notation has been used to refer to the `email` property of `User` in the `members` list.

Now we will write a new test in our `ModelsTest` class to test our `Project` class and the query with it:

```java
    @Test
    public void findProjectsInvolving() {
        new User("bob@gmail.com", "Bob", "secret").save();
        new User("jane@gmail.com", "Jane", "secret").save();

        Project.create("Play 2", "play", "bob@gmail.com");
        Project.create("Play 1", "play", "jane@gmail.com");

        List<Project> results = Project.findInvolving("bob@gmail.com");
        assertEquals(1, results.size());
        assertEquals("Play 2", results.get(0).name);
    }
```

> **Don't forget** to import **java.util.List** or you will get a compilation error.

## Finish with Task

The last thing that we need for our model draft, and most important thing, is tasks.

```java
package models;

import java.util.*;
import javax.persistence.*;
import play.db.ebean.*;

@Entity
public class Task extends Model {

    @Id
    public Long id;
    public String title;
    public boolean done = false;
    public Date dueDate;
    @ManyToOne
    public User assignedTo;
    public String folder;
    @ManyToOne
    public Project project;

    public static Model.Finder<Long,Task> find = new Model.Finder(Long.class, Task.class);

    public static List<Task> findTodoInvolving(String user) {
       return find.fetch("project").where()
                .eq("done", false)
                .eq("project.members.email", user)
           .findList();
    }

    public static Task create(Task task, Long project, String folder) {
        task.project = Project.find.ref(project);
        task.folder = folder;
        task.save();
        return task;
    }
}
```

Each task has a generated id, a title, a flag to say whether it is done or not, a date that it must be completed by, a user it is assigned to, a folder and a project.  The `assignedTo` and `project` relationships are mapped using `@ManyToOne`.  This means a task may have one user, and one project, while each user may have many tasks assigned to them, and each project may have many tasks.

We also have a simple query, this time finding all the todo tasks, that is, those tasks that aren't done, assigned to a particular user, and a create method.

Let's write a test for this class as well.

```java
    @Test
    public void findTodoTasksInvolving() {
        User bob = new User("bob@gmail.com", "Bob", "secret");
        bob.save();

        Project project = Project.create("Play 2", "play", "bob@gmail.com");
        Task t1 = new Task();
        t1.title = "Write tutorial";
        t1.assignedTo = bob;
        t1.done = true;
        t1.save();

        Task t2 = new Task();
        t2.title = "Release next version";
        t2.project = project;
        t2.save();

        List<Task> results = Task.findTodoInvolving("bob@gmail.com");
        assertEquals(1, results.size());
        assertEquals("Release next version", results.get(0).title);
    }
```

## Using Fixtures to write more complicated tests

When you start to write more complex tests, you often need a set of data to test on.  Creating and saving instances of Java classes can be quite cumbersome, for this reason, Play makes it easy to use YAML files to define Java objects, which you can then easily declare your data.  When declaring data, be sure to use the YAML `!!` type operator to specify the model class of the data that you are declaring.

Edit the `conf/test-data.yml` file and start to describe a User:

```yaml
- !!models.User
    email:      bob@gmail.com
    name:       Bob
    password:   secret
...
```

Notice that this object is defined as part of a root object that is a list.  We can now define more objects to be a part of that, however, our dataset is a little large, so you can download a full dataset [here](javaGuide/tutorials/zentasks/files/test-data.yml).

Now we create a test case that loads this data and runs some assertions over it:

```java
    @Test
    public void fullTest() {
        Ebean.save((List) Yaml.load("test-data.yml"));

        // Count things
        assertEquals(3, User.find.findRowCount());
        assertEquals(7, Project.find.findRowCount());
        assertEquals(5, Task.find.findRowCount());

        // Try to authenticate as users
        assertNotNull(User.authenticate("bob@example.com", "secret"));
        assertNotNull(User.authenticate("jane@example.com", "secret"));
        assertNull(User.authenticate("jeff@example.com", "badpassword"));
        assertNull(User.authenticate("tom@example.com", "secret"));

        // Find all Bob's projects
        List<Project> bobsProjects = Project.findInvolving("bob@example.com");
        assertEquals(5, bobsProjects.size());

        // Find all Bob's todo tasks
        List<Task> bobsTasks = Task.findTodoInvolving("bob@example.com");
        assertEquals(4, bobsTasks.size());
    }
```

> You may find it more convenient to load the test data in your `@Before` method, so that the test data is available for every test.

## Save your work

We have now finished a huge part of the task management system.  Now that we have created and tested all these things, we can start to develop the web application itself.

But before continuing, it's time to save your work in git.  Open a command line and type `git status` to see the modifications made since the latest commit:

    git status

As you can see, some new files are not under version control.  Add all the files, and commit your project.

    git add .
    git commit -m "The model layer is ready"

> Go to the [next part](JavaGuide3).
