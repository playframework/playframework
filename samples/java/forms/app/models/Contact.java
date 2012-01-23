package models;

import java.util.*;

import javax.validation.*;

import play.data.validation.Constraints.*;

public class Contact {
    
    @Required
    public String firstname;
    
    @Required
    public String lastname;
    
    public String company;
    
    @Valid
    public List<Information> informations;
    
    public Contact() {}
    
    public Contact(String firstname, String lastname, String company, Information... informations) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.company = company;
        this.informations = new ArrayList<Information>();
        for(Information information: informations) {
            this.informations.add(information);
        }
    }
    
    public static class Information {
        
        @Required
        public String label;
        
        @Email
        public String email;
        
        @Valid
        public List<Phone> phones;
        
        public Information() {}
        
        public Information(String label, String email, String... phones) {
            this.label = label;
            this.email = email;
            this.phones = new ArrayList<Phone>();
            for(String phone: phones) {
                this.phones.add(new Phone(phone));
            }
        }
        
        public static class Phone {
            
            @Required
            @Pattern(value = "[0-9.+]+", message = "A valid phone number is required")
            public String number;
            
            public Phone() {}
                        
            public Phone(String number) {
                this.number = number;
            }
            
        }
        
    }
    
}