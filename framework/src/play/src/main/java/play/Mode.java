/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play;

/**
 * Application mode, either `DEV`, `TEST`, or `PROD`.
 */
public enum Mode {
    DEV, TEST, PROD;

    public play.api.Mode asScala() {
        if (this == DEV) {
            return play.api.Mode.Dev$.MODULE$;
        } else if (this == PROD) {
            return play.api.Mode.Prod$.MODULE$;
        }
        return play.api.Mode.Test$.MODULE$;
    }
}
