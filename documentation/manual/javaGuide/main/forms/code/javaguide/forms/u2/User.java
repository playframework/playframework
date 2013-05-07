package javaguide.forms.u2;

import play.data.validation.Constraints.Required;

//#user
public class User {

    @Required
    public String email;
    public String password;
}
//#user