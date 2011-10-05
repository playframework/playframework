package models;

import javax.validation.*;

import play.data.format.*;
import play.data.validation.*;

public class User {
    
    @Constraints.Required
    public String name;
    
    @Valid
    public Credential credential;
    
    public static class Credential {
        
        @Constraints.Required
        public String login;
        
        @Constraints.Required
        public String password;
        
    }
    
}