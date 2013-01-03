import org.junit.Assert;
import org.junit.Test;

import static play.data.Form.form;

import java.util.List;

import play.data.Form;
import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.Required;
import play.data.validation.ValidationError;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class FormTest {

  @Test
  public void testFormValidation() {
    Form<Registration> form = form(Registration.class).bind(
        ImmutableMap.of("email", "test@benmccann.com", "password1", "changeme", "password2", "alskjfd"));
    Assert.assertTrue(form.hasErrors());
    Assert.assertTrue(form.errors().containsKey("password"));
  }

  public static class Registration {
    @Required @Email public String email;
    @Required public String password1;
    @Required public String password2;

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public String getPassword1() {
      return password1;
    }

    public void setPassword1(String password1) {
      this.password1 = password1;
    }

    public String getPassword2() {
      return password2;
    }

    public void setPassword2(String password2) {
      this.password2 = password2;
    }

    public List<ValidationError> validate() {
      if (!password1.equals(password2)) {
        return ImmutableList.of(new ValidationError("password", "Passwords do not match.", ImmutableList.of()));
      }
      return null;
    }
  }

}
