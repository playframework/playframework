package models;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;

import play.data.validation.Constraints;
import play.db.ebean.Model;


/**
 * Company entity managed by Ebean
 */
@Entity 
public class Company extends Model {

    @Id
    public Long id;
    
    @Constraints.Required
    public String name;
    
    /**
     * Generic query helper for entity Company with id Long
     */
    public static Model.Finder<Long,Company> find
        = new Model.Finder<Long, Company>(Long.class, Company.class);

    public static Map<String,String> options() {
        LinkedHashMap<String,String> options = new LinkedHashMap<String,String>();
        for(Company c: Company.find.orderBy("name").findList()) {
            options.put(c.id.toString(), c.name);
        }
        return options;
    }

}

