/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Annotation utilities.
 */
public class AnnotationUtils {

    /**
     * Returns a new array whose entries do not contain container annotations anymore but the indirectly present annotation(s) a container annotation
     * was wrapping instead. An annotation is considered a container annotation if its indirectly present annotation(s) are annotated with {@link Repeatable}.
     * Annotations inside the given array which don't meet the above definition of a container annotations will be returned untouched.
     * 
     * @param annotations An array of annotations to unwrap. Can contain both container and non container annotations.
     * @return A new array without container annotations but the container annotations' indirectly defined annotations.
     */
    public static <A extends Annotation> Annotation[] unwrapContainerAnnotations(final A[] annotations) {
        final List<Annotation> unwrappedAnnotations = new LinkedList<>();
        for (final Annotation maybeContainerAnnotation : annotations) {
            final List<Annotation> indirectlyPresentAnnotations = getIndirectlyPresentAnnotations(maybeContainerAnnotation);
            if (!indirectlyPresentAnnotations.isEmpty()) {
                unwrappedAnnotations.addAll(indirectlyPresentAnnotations);
            } else {
                unwrappedAnnotations.add(maybeContainerAnnotation); // was not a container annotation
            }
        }
        return unwrappedAnnotations.toArray(new Annotation[unwrappedAnnotations.size()]);
    }

    /**
     * If the return type of an existing {@code value()} method of the passed annotation is an {@code Annotation[]} array and the annotations inside that
     * {@code Annotation[]} array are annotated with the {@link Repeatable} annotation the annotations of that array will be returned.
     * If the passed annotation does not have a {@code value()} method or the above criteria are not met an empty list will be returned instead.
     * 
     * @param maybeContainerAnnotation The annotation which {@code value()} method will be checked for other annotations
     * @return The annotations defined by the {@code value()} method or an empty list.
     */
    public static <A extends Annotation> List<Annotation> getIndirectlyPresentAnnotations(final A maybeContainerAnnotation) {
        try {
            final Method method = maybeContainerAnnotation.annotationType().getMethod("value");
            final Object o = method.invoke(maybeContainerAnnotation);
            if (Annotation[].class.isAssignableFrom(o.getClass())) {
                final Annotation[] indirectAnnotations = (Annotation[])o;
                if (indirectAnnotations.length > 0 && indirectAnnotations[0].annotationType().isAnnotationPresent(Repeatable.class)) {
                    return Arrays.asList(indirectAnnotations);
                }
            }
        } catch (final NoSuchMethodException e) {
            // That's ok, this just wasn't a container annotation -> continue
        } catch (final SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
        return Collections.emptyList();
    }
}
