/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play;

/**
 * Application mode, either `DEV`, `TEST`, or `PROD`.
 */
public enum Mode {
    DEV, TEST, PROD;

    public play.api.Mode.Mode asScala() {
        if (this == DEV) {
            return play.api.Mode.dev();
        } else if (this == PROD) {
            return play.api.Mode.prod();
        }
        return play.api.Mode.test();
    }
}
