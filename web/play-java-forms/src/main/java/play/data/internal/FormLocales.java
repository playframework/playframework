/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data.internal;

import java.util.Collections;
import java.util.Locale;
import play.i18n.Lang;
import play.i18n.Langs;

/**
 * Resolves the locale that Java forms use for binding, formatting and validation messages.
 *
 * <p>This is internal API and may change without notice.
 */
public final class FormLocales {

  private FormLocales() {}

  /**
   * @param langs the application's supported languages
   * @param lang the requested language, may be {@code null}
   * @return the locale of the supported language that best matches {@code lang}, or of the
   *     application's default language if {@code lang} is {@code null} or not supported
   */
  public static Locale resolve(Langs langs, Lang lang) {
    return langs
        .preferred(lang != null ? Collections.singleton(lang) : langs.availables())
        .toLocale();
  }
}
