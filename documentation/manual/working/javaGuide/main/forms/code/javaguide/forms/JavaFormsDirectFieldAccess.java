/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.forms;

import static org.junit.jupiter.api.Assertions.*;
import static play.test.Helpers.fakeApplication;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javaguide.forms.u4.User;
import org.junit.jupiter.api.Test;
import play.Application;
import play.data.Form;
import play.data.FormFactory;
import play.i18n.Lang;
import play.libs.typedmap.TypedMap;
import play.test.junit5.ApplicationExtension;

public class JavaFormsDirectFieldAccess {

  static ApplicationExtension appExtension = new ApplicationExtension(fakeApplication());
  static Application app = appExtension.getApplication();

  private FormFactory formFactory() {
    return app.injector().instanceOf(FormFactory.class);
  }

  @Test
  void usingForm() {
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

    assertEquals("bob@gmail.com", user.email);
    assertEquals("secret", user.password);
  }
}
