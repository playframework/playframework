package models;

import javax.validation.*;

import play.data.validation.Constraints.*;

public class User {
   
    public interface All {}
    public interface Step1{}    
	public interface Step2{}    

    @Required(groups = {All.class, Step1.class})
    @MinLength(value = 4, groups = {All.class, Step1.class})
    public String username;
    
    @Required(groups = {All.class, Step1.class})
    @Email(groups = {All.class, Step1.class})
    public String email;
    
    @Required(groups = {All.class, Step1.class})
    @MinLength(value = 6, groups = {All.class, Step1.class})
    public String password;

    @Valid
    public Profile profile;
    
    public User() {}
    
    public User(String username, String email, String password, Profile profile) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.profile = profile;
    }
    
    public static class Profile {
        
        @Required(groups = {All.class, Step2.class})
        public String country;
        
        public String address;
        
        @Min(value = 18, groups = {All.class, Step2.class}) @Max(value = 100, groups = {All.class, Step2.class})
        public Integer age;
        
        public Profile() {}
        
        public Profile(String country, String address, Integer age) {
            this.country = country;
            this.address = address;
            this.age = age;
        }
        
    }
    
}