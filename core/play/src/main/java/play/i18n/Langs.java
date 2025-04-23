/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.i18n;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import play.libs.Scala;

/** Manages languages in Play */
@Singleton
public class Langs {
  private final play.api.i18n.Langs langs;
  private final List<Lang> availables;

  @Inject
  public Langs(play.api.i18n.Langs langs) {
    this.langs = langs;
    List<Lang> availables = new ArrayList<>();
    for (play.api.i18n.Lang lang : Scala.asJava(langs.availables())) {
      availables.add(new Lang(lang));
    }
    this.availables = Collections.unmodifiableList(availables);
  }

  /**
   * The available languages.
   *
   * <p>These can be configured in {$code application.conf}, like so:
   *
   * <pre>
   * play.i18n.langs = ["fr", "en", "de"]
   * </pre>
   *
   * @return The available languages.
   */
  public List<Lang> availables() {
    return availables;
  }

  /**
   * Select a preferred language, given the list of candidates.
   *
   * <p>Will select the preferred language, based on what languages are available, or return the
   * default language if none of the candidates are available.
   *
   * @param candidates The candidate languages
   * @return The preferred language
   */
  public Lang preferred(Collection<Lang> candidates) {
    return new Lang(langs.preferred(Scala.asScala(candidates)));
  }

  /**
   * @return the Scala version for this Langs.
   */
  public play.api.i18n.Langs asScala() {
    return langs;
  }
}
