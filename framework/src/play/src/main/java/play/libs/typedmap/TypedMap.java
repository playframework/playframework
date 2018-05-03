/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.typedmap;

import play.api.libs.typedmap.TypedMap$;
import scala.compat.java8.OptionConverters;

import java.util.Optional;

/**
 * A TypedMap is an immutable map containing typed values. Each entry is
 * associated with a {@link TypedKey} that can be used to look up the value. A
 * <code>TypedKey</code> also defines the type of the value, e.g. a <code>TypedKey&lt;String&gt;</code>
 * would be associated with a <code>String</code> value.
 *
 * Instances of this class are created with the {@link #empty()} method.
 *
 * The elements inside TypedMaps cannot be enumerated. This is a decision
 * designed to enforce modularity. It's not possible to accidentally or
 * intentionally access a value in a TypedMap without holding the
 * corresponding {@link TypedKey}.
 */
public final class TypedMap {

    private final play.api.libs.typedmap.TypedMap underlying;

    public TypedMap(play.api.libs.typedmap.TypedMap underlying) {
        this.underlying = underlying;
    }

    /**
     * @return the underlying Scala TypedMap which this instance wraps.
     *
     * @deprecated As of release 2.6.8. Use {@link #asScala()}
     */
    @Deprecated
    public play.api.libs.typedmap.TypedMap underlying() {
        return underlying;
    }

    /**
     * @return the underlying Scala TypedMap which this instance wraps.
     */
    public play.api.libs.typedmap.TypedMap asScala() {
        return underlying;
    }

    /**
     * Get a value from the map, throwing an exception if it is not present.
     *
     * @param key The key for the value to retrieve.
     * @param <A> The type of value to retrieve.
     * @return The value, if it is present in the map.
     * @throws java.util.NoSuchElementException If the value isn't present in the map.
     */
    public <A> A get(TypedKey<A> key) {
        return underlying.apply(key.asScala());
    }

    /**
     * Get a value from the map, returning an empty {@link Optional} if it is not present.
     *
     * @param key The key for the value to retrieve.
     * @param <A> The type of value to retrieve.
     * @return An <code>Optional</code>, with the value present if it is in the map.
     */
    public <A> Optional<A> getOptional(TypedKey<A> key) {
        return OptionConverters.toJava(underlying.get(key.asScala()));
    }

    /**
     * Check if the map contains a value with the given key.
     *
     * @param key The key to check for.
     * @return True if the value is present, false otherwise.
     */
    public boolean containsKey(TypedKey<?> key) {
        return underlying.contains(key.asScala());
    }

    /**
     * Update the map with the given key and value, returning a new instance of the map.
     *
     * @param key The key to set.
     * @param value The value to use.
     * @param <A> The type of value.
     * @return A new instance of the map with the new entry added.
     */
    public <A> TypedMap put(TypedKey<A> key, A value) {
        return new TypedMap(underlying.updated(key.asScala(), value));
    }

    /**
     * Update the map with several entries, returning a new instance of the map.
     *
     * @param entries The new entries to add to the map.
     * @return A new instance of the map with the new entries added.
     */
    public TypedMap putAll(TypedEntry<?>... entries) {
        play.api.libs.typedmap.TypedMap newUnderlying = underlying;
        for (TypedEntry<?> e : entries) {
            newUnderlying = newUnderlying.updated(((TypedKey<Object>) e.key()).asScala(), e.value());
        }
        return new TypedMap(newUnderlying);
    }

    @Override
    public String toString() {
        return underlying.toString();
    }

    private static TypedMap empty = new TypedMap(TypedMap$.MODULE$.empty());

    /**
     * @return the empty <code>TypedMap</code> instance.
     */
    public static TypedMap empty() {
        return empty;
    }

    /**
     * @param entries the list of typed entries
     * @return a newly built <code>TypedMap</code> from a list of keys and values.
     */
    public static TypedMap create(TypedEntry<?>... entries) {
        return empty.putAll(entries);
    }
}
