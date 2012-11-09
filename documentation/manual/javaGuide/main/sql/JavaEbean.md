# Using the Ebean ORM

## Configuring Ebean

Play 2.0 comes with the [[Ebean| http://www.avaje.org/]] ORM. To enable it, add the following line to `conf/application.conf`:

```properties
ebean.default="models.*"
```

This defines a `default` Ebean server, using the `default` data source, which must be properly configured. You can actually create as many Ebean servers you need, and explicitly define the mapped class for each server.

```properties
ebean.orders="models.Order,models.OrderItem"
ebean.customers="models.Customer,models.Address"
```

In this example, we have access to two Ebean servers - each using its own database.

> For more information about Ebean, see the [[Ebean documentation | http://www.avaje.org/ebean/documentation.html]].

## Using the play.db.ebean.Model superclass

Play 2.0 defines a convenient superclass for your Ebean model classes. Here is a typical Ebean class, mapped in Play 2.0:

```java
package models;

import java.util.*;
import javax.persistence.*;

import play.db.ebean.*;
import play.data.format.*;
import play.data.validation.*;

@Entity 
public class Task extends Model {

  @Id
  @Constraints.Min(10)
  public Long id;
  
  @Constraints.Required
  public String name;
  
  public boolean done;
  
  @Formats.DateTime(pattern="dd/MM/yyyy")
  public Date dueDate = new Date();
  
  public static Finder<Long,Task> find = new Finder<Long,Task>(
    Long.class, Task.class
  ); 

}
```
> Play has been designed to generate getter/setter automatically, to ensure compatibility with libraries that expect them to be available at runtime (ORM, Databinder, JSON Binder, etc). **If Play detects any user-written getter/setter in the Model, it will not generate getter/setter in order to avoid any conflict.**

As you can see, we've added a `find` static field, defining a `Finder` for an entity of type `Task` with a `Long` identifier. This helper field is then used to simplify querying our model:

```
// Find all tasks
List<Task> tasks = Task.find.all();
    
// Find a task by ID
Task anyTask = Task.find.byId(34L);

// Delete a task by ID
Task.find.ref(34L).delete();

// More complex task query
List<Task> tasks = find.where()
    .ilike("name", "%coco%")
    .orderBy("dueDate asc")
    .findPagingList(25)
    .getPage(1);
```

## Transactional actions

By default Ebean will not use transactions. However, you can use any transaction helper provided by Ebean to create a transaction. For example:

:: Rob Bygrave - 
The above statement is not correct. Ebean will use implicit transactions. To demarcate transactions you have 3 options: @Transactional, TxRunnable() or a beginTransaction(), commitTransaction()

See http://www.avaje.org/ebean/introtrans.html for examples and an explanation.
:: - end note

```
// run in Transactional scope...  
Ebean.execute(new TxRunnable() {  
  public void run() {  
      
    // code running in "REQUIRED" transactional scope  
    // ... as "REQUIRED" is the default TxType  
    System.out.println(Ebean.currentTransaction());  
      
    // find stuff...  
    User user = Ebean.find(User.class, 1);  
    ...  
      
    // save and delete stuff...  
    Ebean.save(user);  
    Ebean.delete(order);  
    ...  
  }  
});
```

You can also annotate your action method with `@play.db.ebean.Transactional` to compose your action method with an `Action` that will automatically manage a transaction:

```
@Transactional
public static Result save() {
  ...
}
```

> **Next:** [[Integrating with JPA | JavaJPA]]