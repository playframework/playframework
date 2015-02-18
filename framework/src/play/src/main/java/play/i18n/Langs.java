/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.i18n;

import scala.collection.JavaConversions;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Manages languages in Play
 */
@Singleton
public class Langs {
    private final play.api.i18n.Langs langs;
    private final List<Lang> availables;

    @Inject
    public Langs(play.api.i18n.Langs langs) {
        this.langs = langs;
        List<Lang> availables = new ArrayList<Lang>();
        for (play.api.i18n.Lang lang : JavaConversions.asJavaIterable(langs.availables())) {
            availables.add(new Lang(lang));
        }
        this.availables = Collections.unmodifiableList(availables);
    }

    /**
     * The available languages.
     *
     * These can be configured in <tt>application.conf</tt>, like so:
     *
     * <pre>
     * play.modules.i18n.langs="fr,en,de"
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
     * Will select the preferred language, based on what languages are available, or return the default language if
     * none of the candidates are available.
     *
     * @param candidates The candidate languages
     * @return The preferred language
     */
    public Lang preferred(Collection<Lang> candidates) {
        return new Lang(langs.preferred((scala.collection.Seq) JavaConversions.collectionAsScalaIterable(candidates).toSeq()));
    }
}
