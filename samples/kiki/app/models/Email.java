package models;

import java.util.*;
import javax.persistence.*;

import javax.validation.constraints.*;

@Entity
public class Email {

    @Pattern(regexp=".+@.+")
    public String value;

    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public String toString() {
        return "Email(" + value + ")";
    }
    
}

