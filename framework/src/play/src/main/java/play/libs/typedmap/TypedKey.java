/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.typedmap;

import play.api.libs.typedmap.TypedKey$;

/**
 * A TypedKey is a key that can be used to get and set values in a
 * {@link TypedMap} or any object with typed keys. This class uses reference
 * equality for comparisons, so each new instance is different key.
 */
public final class TypedKey<A> {

    private final play.api.libs.typedmap.TypedKey<A> underlying;

    public TypedKey(play.api.libs.typedmap.TypedKey<A> underlying) {
        this.underlying = underlying;
    }

    /**
     * @return the underlying Scala TypedKey which this instance wraps.
     *
     * @deprecated As of release 2.6.8. Use {@link #asScala()}
     */
    @Deprecated
    public play.api.libs.typedmap.TypedKey<A> underlying() {
        return underlying;
    }

    /**
     * @return the underlying Scala TypedKey which this instance wraps.
     */
    public play.api.libs.typedmap.TypedKey<A> asScala() {
        return underlying;
    }

    /**
     * Bind this key to a value.
     *
     * @param value The value to bind this key to.
     * @return A bound value.
     */
    public TypedEntry<A> bindValue(A value) {
        return new TypedEntry<A>(this, value);
    }

    /**
     * Creates a TypedKey without a name.
     *
     * @param <A> The type of value this key is associated with.
     * @return A fresh key.
     */
    public static <A> TypedKey<A> create() {
        return new TypedKey<>(TypedKey$.MODULE$.apply());
    }

    /**
     * Creates a TypedKey with the given name.
     *
     * @param displayName The name to display when printing this key.
     * @param <A> The type of value this key is associated with.
     * @return A fresh key.
     */
    public static <A> TypedKey<A> create(String displayName) {
        return new TypedKey<>(TypedKey$.MODULE$.apply(displayName));
    }

    @Override
    public String toString() {
        return underlying.toString();
    }

    @Override
    public int hashCode() {
        return underlying.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TypedKey) {
            return this.underlying.equals(((TypedKey) obj).underlying);
        } else {
            return false;
        }
    }
}
