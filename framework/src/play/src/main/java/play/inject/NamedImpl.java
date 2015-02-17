/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.inject;

import javax.inject.Named;
import java.io.Serializable;
import java.lang.annotation.Annotation;

/**
 * An implementation of the [[javax.inject.Named]] annotation.
 *
 * This allows bindings qualified by name.
 */
// See https://issues.scala-lang.org/browse/SI-8778 for why this is implemented in Java
public class NamedImpl implements Named, Serializable {

    private final String value;

    public NamedImpl(String value) {
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
        if (!(o instanceof Named)) {
            return false;
        }

        Named other = (Named) o;
        return value.equals(other.value());
    }

    public String toString() {
        return "@" + Named.class.getName() + "(value=" + value + ")";
    }

    public Class<? extends Annotation> annotationType() {
        return Named.class;
    }

    private static final long serialVersionUID = 0;
}

