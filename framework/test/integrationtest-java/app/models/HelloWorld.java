package models;

import play.db.ebean.Model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class HelloWorld extends Model {

    public static final String HELLO_WORLD = "Hello world!";

    @Id
    public long id;

    public String field1;

    public String field2;

    public static Finder<Long, HelloWorld> find = new Finder(Long.class, HelloWorld.class);


}
