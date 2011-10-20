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
    
    public static Finder<Long,Task> find = new Finder(Long.class, Task.class); 

    public static int count() {
        return new Finder(Long.class, Task.class).findRowCount();
    }

}
