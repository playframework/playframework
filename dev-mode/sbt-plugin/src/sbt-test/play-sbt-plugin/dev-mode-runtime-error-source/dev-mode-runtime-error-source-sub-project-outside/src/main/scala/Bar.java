/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package outside;

public class Bar {
    public static void fail() {
        throw new RuntimeException("Exception thrown in sub-project-outside");
    }
}
