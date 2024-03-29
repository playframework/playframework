/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javaguide.forms.u4.User;
import org.junit.Test;
import play.data.Form;
import play.data.FormFactory;
import play.i18n.Lang;
import play.libs.typedmap.TypedMap;
import play.test.WithApplication;

public class JavaFormsDirectFieldAccess extends WithApplication {

  private FormFactory formFactory() {
    return app.injector().instanceOf(FormFactory.class);
  }

  @Test
  public void usingForm() {
    FormFactory formFactory = formFactory();

    final // sneaky final
    // #create
    Form<User> userForm = formFactory.form(User.class).withDirectFieldAccess(true);
    // #create

    Lang lang = new Lang(Locale.getDefault());
    TypedMap attrs = TypedMap.empty();
    Map<String, String> anyData = new HashMap<>();
    anyData.put("email", "bob@gmail.com");
    anyData.put("password", "secret");

    User user = userForm.bind(lang, attrs, anyData).get();

    assertThat(user.email).isEqualTo("bob@gmail.com");
    assertThat(user.password).isEqualTo("secret");
  }
}
