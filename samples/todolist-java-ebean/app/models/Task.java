package models;

import java.util.*;
import javax.persistence.*;

import play.data.validation.*;
import play.data.format.*;

@Entity 
public class Task {

    @Id
    public Long id;
    
    @Constraints.Required
    public String name;
    
    public boolean done;
    
    @Formats.DateTime(pattern="dd/MM/yyyy")
    public Date dueDate = new Date();
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setDone(Boolean u) {
        this.done = u;
    }
    
    public Boolean getDone() {
        return done;
    }
    
    public void setDueDate(Date u) {
        this.dueDate = u;
    }
    
    public Date getDueDate() {
        return dueDate;
    }
    
}

