/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db;

import java.io.Serializable;
import java.lang.annotation.Annotation;

// See https://issues.scala-lang.org/browse/SI-8778 for why this is implemented in Java
public class NamedDBImpl implements NamedDB, Serializable {

    private final String value;

    public NamedDBImpl(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }

    public int hashCode() {
        // This is specified in java.lang.Annotation.
        return (127 * "value".hashCode()) ^ value.hashCode();
    }

    public boolean equals(Object o) {
        if (!(o instanceof NamedDB)) {
            return false;
        }

        NamedDB other = (NamedDB) o;
        return value.equals(other.value());
    }

    public String toString() {
        return "@" + NamedDB.class.getName() + "(value=" + value + ")";
    }

    public Class<? extends Annotation> annotationType() {
        return NamedDB.class;
    }

    private static final long serialVersionUID = 0;
}
