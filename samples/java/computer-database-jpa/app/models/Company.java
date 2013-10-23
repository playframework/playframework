package models;

import java.util.*;

import javax.persistence.*;

import play.data.validation.*;
import play.db.jpa.*;

/**
 * Company entity managed by JPA
 */
@Entity 
public class Company {

    @Id
    public Long id;
    
    @Constraints.Required
    public String name;
    
    public static Company findById(Long id) {
        return JPA.em().find(Company.class, id);
    }

    public static Map<String,String> options() {
        @SuppressWarnings("unchecked")
				List<Company> companies = JPA.em().createQuery("from Company order by name").getResultList();
        LinkedHashMap<String,String> options = new LinkedHashMap<String,String>();
        for(Company c: companies) {
            options.put(c.id.toString(), c.name);
        }
        return options;
    }

}

