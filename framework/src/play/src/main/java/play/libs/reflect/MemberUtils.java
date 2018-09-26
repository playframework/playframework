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

import java.lang.reflect.*;

/**
 * Imported from apache.commons.lang3 3.6
 */
abstract class MemberUtils {

    private static final int ACCESS_TEST = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE;

    /** Array of primitive number types ordered by "promotability" */
    private static final Class<?>[] ORDERED_PRIMITIVE_TYPES = { Byte.TYPE, Short.TYPE,
            Character.TYPE, Integer.TYPE, Long.TYPE, Float.TYPE, Double.TYPE };

    /**
     * Returns whether a given set of modifiers implies package access.
     * @param modifiers to test
     * @return {@code true} unless {@code package}/{@code protected}/{@code private} modifier detected
     */
    static boolean isPackageAccess(final int modifiers) {
        return (modifiers & ACCESS_TEST) == 0;
    }

    /**
     * Returns whether a {@link Member} is accessible.
     * @param m Member to check
     * @return {@code true} if <code>m</code> is accessible
     */
    static boolean isAccessible(final Member m) {
        return m != null && Modifier.isPublic(m.getModifiers()) && !m.isSynthetic();
    }

    /**
     * XXX Default access superclass workaround.
     *
     * When a {@code public} class has a default access superclass with {@code public} members,
     * these members are accessible. Calling them from compiled code works fine.
     * Unfortunately, on some JVMs, using reflection to invoke these members
     * seems to (wrongly) prevent access even when the modifier is {@code public}.
     * Calling {@code setAccessible(true)} solves the problem but will only work from
     * sufficiently privileged code. Better workarounds would be gratefully
     * accepted.
     * @param o the AccessibleObject to set as accessible
     * @return a boolean indicating whether the accessibility of the object was set to true.
     */
    static boolean setAccessibleWorkaround(final AccessibleObject o) {
        if (o == null || o.isAccessible()) {
            return false;
        }
        final Member m = (Member) o;
        if (!o.isAccessible() && Modifier.isPublic(m.getModifiers()) && isPackageAccess(m.getDeclaringClass().getModifiers())) {
            try {
                o.setAccessible(true);
                return true;
            } catch (final SecurityException e) { // NOPMD
                // ignore in favor of subsequent IllegalAccessException
            }
        }
        return false;
    }

    /**
     * Compares the relative fitness of two Constructors in terms of how well they
     * match a set of runtime parameter types, such that a list ordered
     * by the results of the comparison would return the best match first
     * (least).
     *
     * @param left the "left" Constructor
     * @param right the "right" Constructor
     * @param actual the runtime parameter types to match against
     * {@code left}/{@code right}
     * @return int consistent with {@code compare} semantics
     * @since 3.5
     */
    static int compareConstructorFit(final Constructor<?> left, final Constructor<?> right, final Class<?>[] actual) {
        return compareParameterTypes(Executable.of(left), Executable.of(right), actual);
    }

    /**
     * Compares the relative fitness of two Methods in terms of how well they
     * match a set of runtime parameter types, such that a list ordered
     * by the results of the comparison would return the best match first
     * (least).
     *
     * @param left the "left" Method
     * @param right the "right" Method
     * @param actual the runtime parameter types to match against
     * {@code left}/{@code right}
     * @return int consistent with {@code compare} semantics
     * @since 3.5
     */
    static int compareMethodFit(final Method left, final Method right, final Class<?>[] actual) {
        return compareParameterTypes(Executable.of(left), Executable.of(right), actual);
    }

    /**
     * Compares the relative fitness of two Executables in terms of how well they
     * match a set of runtime parameter types, such that a list ordered
     * by the results of the comparison would return the best match first
     * (least).
     *
     * @param left the "left" Executable
     * @param right the "right" Executable
     * @param actual the runtime parameter types to match against
     * {@code left}/{@code right}
     * @return int consistent with {@code compare} semantics
     */
    private static int compareParameterTypes(final Executable left, final Executable right, final Class<?>[] actual) {
        final float leftCost = getTotalTransformationCost(actual, left);
        final float rightCost = getTotalTransformationCost(actual, right);
        return leftCost < rightCost ? -1 : rightCost < leftCost ? 1 : 0;
    }


    /**
     * Gets the number of steps required to promote a primitive number to another
     * type.
     * @param srcClass the (primitive) source class
     * @param destClass the (primitive) destination class
     * @return The cost of promoting the primitive
     */
    private static float getPrimitivePromotionCost(final Class<?> srcClass, final Class<?> destClass) {
        float cost = 0.0f;
        Class<?> cls = srcClass;
        if (!cls.isPrimitive()) {
            // slight unwrapping penalty
            cost += 0.1f;
            cls = ClassUtils.wrapperToPrimitive(cls);
        }
        for (int i = 0; cls != destClass && i < ORDERED_PRIMITIVE_TYPES.length; i++) {
            if (cls == ORDERED_PRIMITIVE_TYPES[i]) {
                cost += 0.1f;
                if (i < ORDERED_PRIMITIVE_TYPES.length - 1) {
                    cls = ORDERED_PRIMITIVE_TYPES[i + 1];
                }
            }
        }
        return cost;
    }

    /**
     * Returns the sum of the object transformation cost for each class in the
     * source argument list.
     * @param srcArgs The source arguments
     * @param executable The executable to calculate transformation costs for
     * @return The total transformation cost
     */
    private static float getTotalTransformationCost(final Class<?>[] srcArgs, final Executable executable) {
        final Class<?>[] destArgs = executable.getParameterTypes();
        final boolean isVarArgs = executable.isVarArgs();

        // "source" and "destination" are the actual and declared args respectively.
        float totalCost = 0.0f;
        final long normalArgsLen = isVarArgs ? destArgs.length-1 : destArgs.length;
        if (srcArgs.length < normalArgsLen) {
            return Float.MAX_VALUE;
        }
        for (int i = 0; i < normalArgsLen; i++) {
            totalCost += getObjectTransformationCost(srcArgs[i], destArgs[i]);
        }
        if (isVarArgs) {
            // When isVarArgs is true, srcArgs and dstArgs may differ in length.
            // There are two special cases to consider:
            final boolean noVarArgsPassed = srcArgs.length < destArgs.length;
            final boolean explicitArrayForVarags = srcArgs.length == destArgs.length && srcArgs[srcArgs.length-1].isArray();

            final float varArgsCost = 0.001f;
            final Class<?> destClass = destArgs[destArgs.length-1].getComponentType();
            if (noVarArgsPassed) {
                // When no varargs passed, the best match is the most generic matching type, not the most specific.
                totalCost += getObjectTransformationCost(destClass, Object.class) + varArgsCost;
            } else if (explicitArrayForVarags) {
                final Class<?> sourceClass = srcArgs[srcArgs.length-1].getComponentType();
                totalCost += getObjectTransformationCost(sourceClass, destClass) + varArgsCost;
            } else {
                // This is typical varargs case.
                for (int i = destArgs.length-1; i < srcArgs.length; i++) {
                    final Class<?> srcClass = srcArgs[i];
                    totalCost += getObjectTransformationCost(srcClass, destClass) + varArgsCost;
                }
            }
        }
        return totalCost;
    }

    /**
     * Gets the number of steps required needed to turn the source class into
     * the destination class. This represents the number of steps in the object
     * hierarchy graph.
     * @param srcClass The source class
     * @param destClass The destination class
     * @return The cost of transforming an object
     */
    private static float getObjectTransformationCost(Class<?> srcClass, final Class<?> destClass) {
        if (destClass.isPrimitive()) {
            return getPrimitivePromotionCost(srcClass, destClass);
        }
        float cost = 0.0f;
        while (srcClass != null && !destClass.equals(srcClass)) {
            if (destClass.isInterface() && ClassUtils.isAssignable(srcClass, destClass)) {
                // slight penalty for interface match.
                // we still want an exact match to override an interface match,
                // but
                // an interface match should override anything where we have to
                // get a superclass.
                cost += 0.25f;
                break;
            }
            cost++;
            srcClass = srcClass.getSuperclass();
        }
        /*
         * If the destination class is null, we've traveled all the way up to
         * an Object match. We'll penalize this by adding 1.5 to the cost.
         */
        if (srcClass == null) {
            cost += 1.5f;
        }
        return cost;
    }


    static boolean isMatchingMethod(final Method method, final Class<?>[] parameterTypes) {
        return isMatchingExecutable(Executable.of(method), parameterTypes);
    }

    static boolean isMatchingConstructor(final Constructor<?> method, final Class<?>[] parameterTypes) {
        return isMatchingExecutable(Executable.of(method), parameterTypes);
    }

    private static boolean isMatchingExecutable(final Executable method, final Class<?>[] parameterTypes) {
        final Class<?>[] methodParameterTypes = method.getParameterTypes();
        if (method.isVarArgs()) {
            int i;
            for (i = 0; i < methodParameterTypes.length - 1 && i < parameterTypes.length; i++) {
                if (!ClassUtils.isAssignable(parameterTypes[i], methodParameterTypes[i], true)) {
                    return false;
                }
            }
            final Class<?> varArgParameterType = methodParameterTypes[methodParameterTypes.length - 1].getComponentType();
            for (; i < parameterTypes.length; i++) {
                if (!ClassUtils.isAssignable(parameterTypes[i], varArgParameterType, true)) {
                    return false;
                }
            }
            return true;
        }
        return ClassUtils.isAssignable(parameterTypes, methodParameterTypes, true);
    }

    /**
     * <p> A class providing a subset of the API of java.lang.reflect.Executable in Java 1.8,
     * providing a common representation for function signatures for Constructors and Methods.</p>
     */
    private static final class Executable {
        private final Class<?>[] parameterTypes;
        private final boolean  isVarArgs;

        private static Executable of(final Method method) {
            return new Executable(method);
        }

        private static Executable of(final Constructor<?> constructor) {
            return new Executable(constructor);
        }

        private Executable(final Method method) {
            parameterTypes = method.getParameterTypes();
            isVarArgs = method.isVarArgs();
        }

        private Executable(final Constructor<?> constructor) {
            parameterTypes = constructor.getParameterTypes();
            isVarArgs = constructor.isVarArgs();
        }

        public Class<?>[] getParameterTypes() {
            return parameterTypes;
        }

        public boolean isVarArgs() {
            return isVarArgs;
        }
    }
}
