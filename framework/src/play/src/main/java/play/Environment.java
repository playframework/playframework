/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;

/**
 * The environment for the application.
 *
 * Captures concerns relating to the classloader and the filesystem for the application.
 */
@Singleton
public class Environment {
    private final play.api.Environment env;

    @Inject
    public Environment(play.api.Environment environment) {
        this.env = environment;
    }

    /**
     * The root path that the application is deployed at.
     */
    public File rootPath() {
        return env.rootPath();
    }

    /**
     * The classloader that all application classes and resources can be loaded from.
     */
    public ClassLoader classLoader() {
        return env.classLoader();
    }

    /**
     * The mode of the application.
     */
    public Mode mode() {
        if (env.mode().equals(play.api.Mode.Prod())) {
            return Mode.PROD;
        } else if (env.mode().equals(play.api.Mode.Dev())) {
            return Mode.DEV;
        } else {
            return Mode.TEST;
        }
    }
}
