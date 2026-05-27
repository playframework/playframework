/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Modified from the original Spring Framework source for Play Framework form binding by the Play Framework contributors.
 */

package play.data.internal.binding.core;

import play.data.internal.binding.util.Assert;

/**
 * Helper class for resolving generic types against type variables.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sam Brannen
 * @author Phillip Webb
 * @author Yanming Zhou
 */
public final class GenericTypeResolver {

	private GenericTypeResolver() {
	}

	/**
	 * Resolve the single type argument of the given generic type against
	 * the given target class which is assumed to implement the given type
	 * and possibly declare a concrete type for its type variable.
	 * @param clazz the target class to check against
	 * @param genericType the generic interface or superclass to resolve the type argument from
	 * @return the resolved type of the argument, or {@code null} if not resolvable
	 */
	public static Class<?> resolveTypeArgument(Class<?> clazz, Class<?> genericType) {
		ResolvableType resolvableType = ResolvableType.forClass(clazz).as(genericType);
		if (!resolvableType.hasGenerics()) {
			return null;
		}
		return getSingleGeneric(resolvableType);
	}

	private static Class<?> getSingleGeneric(ResolvableType resolvableType) {
		Assert.isTrue(resolvableType.getGenerics().length == 1,
				() -> "Expected 1 type argument on generic interface [" + resolvableType +
				"] but found " + resolvableType.getGenerics().length);
		return resolvableType.getGeneric().resolve();
	}

	/**
	 * Resolve the type arguments of the given generic type against the given
	 * target class which is assumed to implement or extend from the given type
	 * and possibly declare concrete types for its type variables.
	 * @param clazz the target class to check against
	 * @param genericType the generic interface or superclass to resolve the type argument from
	 * @return the resolved type of each argument, with the array size matching the
	 * number of actual type arguments, or {@code null} if not resolvable
	 */
	public static Class<?> [] resolveTypeArguments(Class<?> clazz, Class<?> genericType) {
		ResolvableType type = ResolvableType.forClass(clazz).as(genericType);
		if (!type.hasGenerics() || !type.hasResolvableGenerics()) {
			return null;
		}
		return type.resolveGenerics(Object.class);
	}

}
