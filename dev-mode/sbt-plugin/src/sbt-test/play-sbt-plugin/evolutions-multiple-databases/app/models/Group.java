/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package models;

public class Group {
    public final Long id;
    public final String name;

    public Group(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}