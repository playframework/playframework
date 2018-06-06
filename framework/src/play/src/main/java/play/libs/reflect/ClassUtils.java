/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package play.libs.reflect;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

/**
 * Imported from apache.commons.lang3 3.6
 */
abstract class ClassUtils {

    public static int arrayGetLength(final Object array) {
        if (array == null) {
            return 0;
        }
        return Array.getLength(array);
    }

    /**
     * An empty immutable {@code Class} array.
     */
    public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];

    /**
     * <p>Checks if one {@code Class} can be assigned to a variable of
     * another {@code Class}.</p>
     *
     * <p>Unlike the {@link Class#isAssignableFrom(java.lang.Class)} method,
     * this method takes into account widenings of primitive classes and
     * {@code null}s.</p>
     *
     * <p>Primitive widenings allow an int to be assigned to a long, float or
     * double. This method returns the correct result for these cases.</p>
     *
     * <p>{@code Null} may be assigned to any reference type. This method
     * will return {@code true} if {@code null} is passed in and the
     * toClass is non-primitive.</p>
     *
     * <p>Specifically, this method tests whether the type represented by the
     * specified {@code Class} parameter can be converted to the type
     * represented by this {@code Class} object via an identity conversion
     * widening primitive or widening reference conversion. See
     * <em><a href="http://java.sun.com/docs/books/jls/">The Java Language Specification</a></em>,
     * sections 5.1.1, 5.1.2 and 5.1.4 for details.</p>
     *
     * <p><strong>Since Lang 3.0,</strong> this method will default behavior for
     * calculating assignability between primitive and wrapper types <em>corresponding
     * to the running Java version</em>; i.e. autoboxing will be the default
     * behavior in VMs running Java versions >= 1.5.</p>
     *
     * @param cls  the Class to check, may be null
     * @param toClass  the Class to try to assign into, returns false if null
     * @return {@code true} if assignment possible
     */
    public static boolean isAssignable(Class<?> cls, Class<?> toClass) {
        return isAssignable(cls, toClass,
                /* actually play runs on VMs > 8 only so autoboxing is always true */true);
    }

    /**
     * <p>Checks if one {@code Class} can be assigned to a variable of
     * another {@code Class}.</p>
     *
     * <p>Unlike the {@link Class#isAssignableFrom(java.lang.Class)} method,
     * this method takes into account widenings of primitive classes and
     * {@code null}s.</p>
     *
     * <p>Primitive widenings allow an int to be assigned to a long, float or
     * double. This method returns the correct result for these cases.</p>
     *
     * <p>{@code Null} may be assigned to any reference type. This method
     * will return {@code true} if {@code null} is passed in and the
     * toClass is non-primitive.</p>
     *
     * <p>Specifically, this method tests whether the type represented by the
     * specified {@code Class} parameter can be converted to the type
     * represented by this {@code Class} object via an identity conversion
     * widening primitive or widening reference conversion. See
     * <em><a href="http://java.sun.com/docs/books/jls/">The Java Language Specification</a></em>,
     * sections 5.1.1, 5.1.2 and 5.1.4 for details.</p>
     *
     * @param cls  the Class to check, may be null
     * @param toClass  the Class to try to assign into, returns false if null
     * @param autoboxing  whether to use implicit autoboxing/unboxing between primitives and wrappers
     * @return {@code true} if assignment possible
     */
    public static boolean isAssignable(Class<?> cls, Class<?> toClass, boolean autoboxing) {
        if (toClass == null) {
            return false;
        }
        // have to check for null, as isAssignableFrom doesn't
        if (cls == null) {
            return !toClass.isPrimitive();
        }
        //autoboxing:
        if (autoboxing) {
            if (cls.isPrimitive() && !toClass.isPrimitive()) {
                cls = primitiveToWrapper(cls);
                if (cls == null) {
                    return false;
                }
            }
            if (toClass.isPrimitive() && !cls.isPrimitive()) {
                cls = wrapperToPrimitive(cls);
                if (cls == null) {
                    return false;
                }
            }
        }
        if (cls.equals(toClass)) {
            return true;
        }
        if (cls.isPrimitive()) {
            if (toClass.isPrimitive() == false) {
                return false;
            }
            if (Integer.TYPE.equals(cls)) {
                return Long.TYPE.equals(toClass)
                        || Float.TYPE.equals(toClass)
                        || Double.TYPE.equals(toClass);
            }
            if (Long.TYPE.equals(cls)) {
                return Float.TYPE.equals(toClass)
                        || Double.TYPE.equals(toClass);
            }
            if (Boolean.TYPE.equals(cls)) {
                return false;
            }
            if (Double.TYPE.equals(cls)) {
                return false;
            }
            if (Float.TYPE.equals(cls)) {
                return Double.TYPE.equals(toClass);
            }
            if (Character.TYPE.equals(cls)) {
                return Integer.TYPE.equals(toClass)
                        || Long.TYPE.equals(toClass)
                        || Float.TYPE.equals(toClass)
                        || Double.TYPE.equals(toClass);
            }
            if (Short.TYPE.equals(cls)) {
                return Integer.TYPE.equals(toClass)
                        || Long.TYPE.equals(toClass)
                        || Float.TYPE.equals(toClass)
                        || Double.TYPE.equals(toClass);
            }
            if (Byte.TYPE.equals(cls)) {
                return Short.TYPE.equals(toClass)
                        || Integer.TYPE.equals(toClass)
                        || Long.TYPE.equals(toClass)
                        || Float.TYPE.equals(toClass)
                        || Double.TYPE.equals(toClass);
            }
            // should never get here
            return false;
        }
        return toClass.isAssignableFrom(cls);
    }

    /**
     * <p>Checks if an array of Classes can be assigned to another array of Classes.</p>
     *
     * <p>This method calls {@link #isAssignable(Class, Class) isAssignable} for each
     * Class pair in the input arrays. It can be used to check if a set of arguments
     * (the first parameter) are suitably compatible with a set of method parameter types
     * (the second parameter).</p>
     *
     * <p>Unlike the {@link Class#isAssignableFrom(java.lang.Class)} method, this
     * method takes into account widenings of primitive classes and
     * {@code null}s.</p>
     *
     * <p>Primitive widenings allow an int to be assigned to a {@code long},
     * {@code float} or {@code double}. This method returns the correct
     * result for these cases.</p>
     *
     * <p>{@code Null} may be assigned to any reference type. This method will
     * return {@code true} if {@code null} is passed in and the toClass is
     * non-primitive.</p>
     *
     * <p>Specifically, this method tests whether the type represented by the
     * specified {@code Class} parameter can be converted to the type
     * represented by this {@code Class} object via an identity conversion
     * widening primitive or widening reference conversion. See
     * <em><a href="http://java.sun.com/docs/books/jls/">The Java Language Specification</a></em>,
     * sections 5.1.1, 5.1.2 and 5.1.4 for details.</p>
     *
     * @param classArray  the array of Classes to check, may be {@code null}
     * @param toClassArray  the array of Classes to try to assign into, may be {@code null}
     * @param autoboxing  whether to use implicit autoboxing/unboxing between primitives and wrappers
     * @return {@code true} if assignment possible
     */
    public static boolean isAssignable(Class<?>[] classArray, Class<?>[] toClassArray, boolean autoboxing) {
        if (arrayGetLength(classArray) != arrayGetLength(toClassArray)) {
            return false;
        }
        if (classArray == null) {
            classArray = EMPTY_CLASS_ARRAY;
        }
        if (toClassArray == null) {
            toClassArray = EMPTY_CLASS_ARRAY;
        }
        for (int i = 0; i < classArray.length; i++) {
            if (isAssignable(classArray[i], toClassArray[i], autoboxing) == false) {
                return false;
            }
        }
        return true;
    }

    /**
     * Maps primitive {@code Class}es to their corresponding wrapper {@code Class}.
     */
    private static final Map<Class<?>, Class<?>> primitiveWrapperMap = new HashMap<>();
    static {
        primitiveWrapperMap.put(Boolean.TYPE, Boolean.class);
        primitiveWrapperMap.put(Byte.TYPE, Byte.class);
        primitiveWrapperMap.put(Character.TYPE, Character.class);
        primitiveWrapperMap.put(Short.TYPE, Short.class);
        primitiveWrapperMap.put(Integer.TYPE, Integer.class);
        primitiveWrapperMap.put(Long.TYPE, Long.class);
        primitiveWrapperMap.put(Double.TYPE, Double.class);
        primitiveWrapperMap.put(Float.TYPE, Float.class);
        primitiveWrapperMap.put(Void.TYPE, Void.TYPE);
    }

    /**
     * Maps wrapper {@code Class}es to their corresponding primitive types.
     */
    private static final Map<Class<?>, Class<?>> wrapperPrimitiveMap = new HashMap<Class<?>, Class<?>>();
    static {
        for (Class<?> primitiveClass : primitiveWrapperMap.keySet()) {
            Class<?> wrapperClass = primitiveWrapperMap.get(primitiveClass);
            if (!primitiveClass.equals(wrapperClass)) {
                wrapperPrimitiveMap.put(wrapperClass, primitiveClass);
            }
        }
    }

    /**
     * <p>Converts the specified primitive Class object to its corresponding
     * wrapper Class object.</p>
     *
     * <p>NOTE: From v2.2, this method handles {@code Void.TYPE},
     * returning {@code Void.TYPE}.</p>
     *
     * @param cls  the class to convert, may be null
     * @return the wrapper class for {@code cls} or {@code cls} if
     * {@code cls} is not a primitive. {@code null} if null input.
     * @since 2.1
     */
    static Class<?> primitiveToWrapper(final Class<?> cls) {
        Class<?> convertedClass = cls;
        if (cls != null && cls.isPrimitive()) {
            convertedClass = primitiveWrapperMap.get(cls);
        }
        return convertedClass;
    }


    /**
     * <p>Converts the specified wrapper class to its corresponding primitive
     * class.</p>
     *
     * <p>This method is the counter part of {@code primitiveToWrapper()}.
     * If the passed in class is a wrapper class for a primitive type, this
     * primitive type will be returned (e.g. {@code Integer.TYPE} for
     * {@code Integer.class}). For other classes, or if the parameter is
     * <b>null</b>, the return value is <b>null</b>.</p>
     *
     * @param cls the class to convert, may be <b>null</b>
     * @return the corresponding primitive type if {@code cls} is a
     * wrapper class, <b>null</b> otherwise
     * @see #primitiveToWrapper(Class)
     * @since 2.4
     */
    public static Class<?> wrapperToPrimitive(Class<?> cls) {
        return wrapperPrimitiveMap.get(cls);
    }

}
