package play.i18n;

import java.util.*;
import play.libs.*;

/**
 * A Lang supported by the application.
 */
public class Lang extends play.api.i18n.Lang {
    
    public final play.api.i18n.Lang underlyingLang;
    
    public Lang(play.api.i18n.Lang underlyingLang) {
        super(underlyingLang.language(), underlyingLang.country());
        this.underlyingLang = underlyingLang;
    }
    
    /**
     * A valid ISO Language Code.
     */
    public String language() {
        return underlyingLang.language();
    }
    
    /**
     * A valid ISO Country Code.
     */
    public String country() {
        return underlyingLang.country();
    }
    
    /**
     * The Lang code (such as fr or en-US).
     */
    public String code() {
        return underlyingLang.code();
    }
    
    /**
     * Convert to a Java Locale value.
     */
    public java.util.Locale toLocale() {
        return underlyingLang.toLocale();
    }

    @Override
    public boolean equals(Object other) {
        return underlyingLang.equals(other);
    }

    @Override
    public int hashCode() {
        return underlyingLang.hashCode();
    }
    
    /**
     * Create a Lang value from a code (such as fr or en-US).
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
     */
    public static List<Lang> availables() {
        List<play.api.i18n.Lang> langs = Scala.asJava(play.api.i18n.Lang.availables(play.api.Play.current()));
        List<play.i18n.Lang> result = new ArrayList<play.i18n.Lang>();
        for(play.api.i18n.Lang lang: langs) {
            result.add(new Lang(lang));
        }
        return result;
    }
    
    /**
     * Guess the preferred lang in the langs set passed as argument.
     * The first Lang that matches an available Lang wins, otherwise returns the first Lang available in this application.
     */
    public static Lang preferred(List<Lang> langs) {
        List<play.api.i18n.Lang> result = new ArrayList<play.api.i18n.Lang>();
        for(play.i18n.Lang lang: langs) {
            result.add(lang.underlyingLang);
        }
        return new Lang(play.api.i18n.Lang.preferred(Scala.toSeq(result), play.api.Play.current()));
    }

}