package models;

import java.util.*;
import javax.persistence.*;

import javax.validation.*;
import javax.validation.constraints.*;

@Entity
public class User {

    public String name;
    
    @Min(0)
    @Max(100)
    public Integer age;
    
    @Valid
    public List<Email> emails;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<Email> getEmails() {
        return emails;
    }
    
    public void setEmails(List<Email> emails) {
        this.emails = emails;
    }
    
    public Integer getAge() {
        return age;
    }
    
    public void setAge(Integer age) {
        this.age = age;
    }
    
    public String toString() {
        return "User(" + name + "," + age + "," + emails + ")";
    }
    
}

