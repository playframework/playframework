package models;

import javax.persistence.*;

@Entity
public class Person {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
	public String id;

    public String name;
}
