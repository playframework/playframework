package play.data;

import play.data.validation.Constraints;

public class UserEmail {

    @Constraints.Email
    public String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
