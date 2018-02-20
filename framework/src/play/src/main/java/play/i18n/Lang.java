/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.i18n;

import java.util.*;
import java.util.stream.Stream;

import play.Application;
import play.libs.*;
import scala.collection.immutable.Seq;

import static java.util.stream.Collectors.toList;

/**
 * A Lang supported by the application.
 */
public class Lang extends play.api.i18n.Lang {

    public Lang(play.api.i18n.Lang underlyingLang) {
        super(underlyingLang.locale());
    }

    public Lang(java.util.Locale locale) {
        this(new play.api.i18n.Lang(locale));
    }

    /**
     * A valid ISO Language Code.
     */
    public String language() {
        return locale().getLanguage();
    }

    /**
     * A valid ISO Country Code.
     */
    public String country() {
        return locale().getCountry();
    }

    /**
     * The script tag for this Lang
     */
    public String script() {
        return locale().getScript();
    }

    /**
     * The variant tag for this Lang
     */
    public String variant() {
        return locale().getVariant();
    }

    /**
     * The language tag (such as fr or en-US).
     */
    public String code() {
        return locale().toLanguageTag();
    }

    /**
     * Convert to a Java Locale value.
     */
    public java.util.Locale toLocale() {
        return locale();
    }

    /**
     * Create a Lang value from a code (such as fr or en-US).
     *
     * @param code the language code
     * @return the Lang for the code, or null of no matching lang was found.
     */
    public static Lang forCode(String code) {
        try {
            return new Lang(play.api.i18n.Lang.apply(code));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Retrieve Lang availables from the application configuration.
     *
     * @param app the current application.
     * @return the list of available Lang.
     */
    public static List<Lang> availables(Application app) {
        play.api.i18n.Langs langs = app.injector().instanceOf(play.api.i18n.Langs.class);
        List<play.api.i18n.Lang> availableLangs = Scala.asJava(langs.availables());
        return availableLangs.stream().map(Lang::new).collect(toList());
    }

    /**
     * Guess the preferred lang in the langs set passed as argument.
     * The first Lang that matches an available Lang wins, otherwise returns the first Lang available in this application.
     *
     * @param app the currept application
     * @param availableLangs the set of langs from which to guess the preferred
     * @return the preferred lang.
     */
    public static Lang preferred(Application app, List<Lang> availableLangs) {
        play.api.i18n.Langs langs = app.injector().instanceOf(play.api.i18n.Langs.class);
        Stream<Lang> stream = availableLangs.stream();
        List<play.api.i18n.Lang> langSeq = stream.map(l -> new play.api.i18n.Lang(l.toLocale())).collect(toList());
        return new Lang(langs.preferred(Scala.toSeq(langSeq)));
    }
}
