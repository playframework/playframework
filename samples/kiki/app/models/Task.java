package models;

import java.util.*;
import javax.persistence.*;

import javax.validation.*;
import javax.validation.constraints.*;

import org.hibernate.validator.constraints.*;

import play.data.format.*;

@Entity
public class Task {

    @Id
    @Min(40)
    public Long id;
    
    @NotEmpty
    public String name;
    
    public boolean done;
    
    @Valid
    public User user;
    
    @Formats.DateTime(pattern="dd/MM/yy")
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
    
    public void setUser(User u) {
        this.user = u;
    }
    
    public User getUser() {
        return user;
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
    
    public String toString() {
        return "Task(" + id + "," + name + "," + user + "," + done + "," + dueDate + ")";
    }
    
}

