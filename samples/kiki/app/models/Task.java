package models;

import java.util.*;
import javax.persistence.*;

import javax.validation.*;
import javax.validation.constraints.*;

@Entity
public class Task {

    @Id
    @Min(40)
    public Long id;
    
    public String name;
    
    @Valid
    public User user;
    
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
    
    public String toString() {
        return "Task(" + id + "," + name + "," + user + ")";
    }
    
}

