/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
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