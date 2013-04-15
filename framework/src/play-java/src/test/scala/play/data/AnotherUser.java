package play.data;

import java.util.*;
import play.libs.F;

public class AnotherUser {

    private String name;
    private List<String> emails = new ArrayList<String>();
    private F.Option<String> company = new F.None();

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setCompany(F.Option<String> company) {
        this.company = company;
    }

    public F.Option<String> getCompany() {
        return this.company;
    }

    public List<String> getEmails() {
        return emails;
    }

}
