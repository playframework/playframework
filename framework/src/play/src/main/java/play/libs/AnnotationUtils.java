/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Annotation utilities.
 */
public class AnnotationUtils {

    /**
     * Returns a new array whose entries do not contain container annotations anymore but the indirectly present annotations a container annotation
     * was wrapping instead. Annotations inside the given array which are not container annotations will be returned untouched.
     * 
     * @param annotations An array of annotations to unwrap. Can contain both container and non container annotations.
     * @return A new array without container annotations but the container annotations' indirectly defined annotations.
     */
    public static <A extends Annotation> Annotation[] unwrapContainerAnnotations(final A[] annotations) {
        final List<Annotation> returnAnnotations = new ArrayList<>();
        for(final Annotation a : annotations) {
            final List<Annotation> indirectlyPresentAnnotations = getIndirectlyPresentAnnotations(a);
            if(!indirectlyPresentAnnotations.isEmpty()) {
                returnAnnotations.addAll(indirectlyPresentAnnotations);
            } else {
                returnAnnotations.add(a);
            }
        }
        return returnAnnotations.toArray(new Annotation[returnAnnotations.size()]);
    }

    /**
     * If the given annotation's {@code value()} method is an {@code Annotation[]} array the annotations of that array will be returned.
     * If the annotation does not have a {@code value()} method or the return type of the {@code value()} method is not an {@code Annotation[]} array
     * an empty list will be returned instead.
     * 
     * @param annotation The annotation which {@code value()} method will be checked for other annotations 
     * @return The annotations defined by the {@code value()} method or an empty list.
     */
    public static <A extends Annotation> List<Annotation> getIndirectlyPresentAnnotations(final A annotation) {
        try {
            final Method method = annotation.getClass().getMethod("value");
            final Object o = method.invoke(annotation);
            if (Annotation[].class.isAssignableFrom(o.getClass())) {
                return Arrays.asList((Annotation[])o);
            }
        } catch (final NoSuchMethodException e) {
            // That's ok, this just wasn't a container annotation -> continue
        } catch (final SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
        return Collections.emptyList();
    }
}
