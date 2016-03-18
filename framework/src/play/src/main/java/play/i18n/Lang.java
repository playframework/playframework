/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.i18n;

import java.util.*;

import play.Application;
import play.api.Play;
import play.libs.*;

/**
 * A Lang supported by the application.
 */
public class Lang extends play.api.i18n.Lang {

    /**
     * @deprecated All instances of play.i18n.Lang can be used as a play.api.i18n.Lang (2.5.1)
     */
    @Deprecated
    public final play.api.i18n.Lang underlyingLang;

    public Lang(play.api.i18n.Lang underlyingLang) {
        super(underlyingLang.locale());
        this.underlyingLang = underlyingLang;
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
     * @return the available languages
     * @deprecated Please use Lang.availables(app), since 2.5.0
     */
    @Deprecated
    public static List<Lang> availables() {
        Application current = play.Play.application();
        return availables(current);
    }

    /**
     * Retrieve Lang availables from the application configuration.
     */
    public static List<Lang> availables(Application app) {
        List<play.api.i18n.Lang> langs = Scala.asJava(play.api.i18n.Lang.availables(app.getWrappedApplication()));
        List<play.i18n.Lang> result = new ArrayList<play.i18n.Lang>();
        for(play.api.i18n.Lang lang: langs) {
            result.add(new Lang(lang));
        }
        return result;
    }

    /**
     * Guess the preferred lang in the langs set passed as argument.
     * The first Lang that matches an available Lang wins, otherwise returns the first Lang available in this application.
     *
     * @param langs the set of langs from which to guess the preferred
     * @return the preferred lang.
     * @deprecated Please use preferred(app, langs).  Deprecated since 2.5.0.
     */
    @Deprecated
    public static Lang preferred(List<Lang> langs) {
        play.api.Application app = Play.current();
        List<play.api.i18n.Lang> result = new ArrayList<play.api.i18n.Lang>();
        for(play.i18n.Lang lang: langs) {
            result.add(lang);
        }
        return new Lang(play.api.i18n.Lang.preferred(Scala.toSeq(result), app));
    }

    /**
     * Guess the preferred lang in the langs set passed as argument.
     * The first Lang that matches an available Lang wins, otherwise returns the first Lang available in this application.
     *
     * @param langs the set of langs from which to guess the preferred
     * @return the preferred lang.
     */
    public static Lang preferred(Application app, List<Lang> langs) {
        List<play.api.i18n.Lang> result = new ArrayList<play.api.i18n.Lang>();
        for(play.i18n.Lang lang: langs) {
            result.add(lang);
        }
        return new Lang(play.api.i18n.Lang.preferred(Scala.toSeq(result), app.getWrappedApplication()));
    }
}
