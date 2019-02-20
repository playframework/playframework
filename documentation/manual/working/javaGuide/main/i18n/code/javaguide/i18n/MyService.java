/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.i18n;

import play.i18n.Lang;

// #inject-lang
import play.i18n.Langs;
import play.i18n.Messages;
import play.i18n.MessagesApi;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

public class MyService {
  private final Langs langs;

  @Inject
  public MyService(Langs langs) {
    this.langs = langs;
  }
}
// #inject-lang

class LangOps {
  private final Langs langs;

  @Inject
  LangOps(Langs langs) {
    this.langs = langs;
  }

  public void ops() {
    Lang lang = langs.availables().get(0);
    // #lang-to-locale
    java.util.Locale locale = lang.toLocale();
    // #lang-to-locale
  }
}

// #current-lang-render
class SomeService {
  private final play.i18n.MessagesApi messagesApi;

  @Inject
  SomeService(MessagesApi messagesApi) {
    this.messagesApi = messagesApi;
  }

  public void message() {
    Collection<Lang> candidates = Collections.singletonList(new Lang(Locale.US));
    Messages messages = messagesApi.preferred(candidates);
    String message = messages.at("home.title");
  }
}
// #current-lang-render

// #inject-messages-api
// ###replace: public class MyClass {
class MyClass {

  private final play.i18n.MessagesApi messagesApi;

  @Inject
  public MyClass(MessagesApi messagesApi) {
    this.messagesApi = messagesApi;
  }
}
// #inject-messages-api
