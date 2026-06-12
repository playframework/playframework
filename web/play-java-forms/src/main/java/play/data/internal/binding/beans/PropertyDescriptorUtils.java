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

package play.data.internal.binding.beans;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import play.data.internal.binding.core.ResolvableType;
import play.data.internal.binding.util.ObjectUtils;
import play.data.internal.binding.util.StringUtils;

/**
 * Common delegate methods for internal {@link PropertyDescriptor} implementations.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
abstract class PropertyDescriptorUtils {

	public static final PropertyDescriptor[] EMPTY_PROPERTY_DESCRIPTOR_ARRAY = {};


	/**
	 * Simple introspection algorithm for basic set/get/is accessor methods,
	 * building corresponding JavaBeans property descriptors for them.
	 * <p>This just supports the basic JavaBeans conventions, without indexed
	 * properties or any customizers, and without other BeanInfo metadata.
	 * For standard JavaBeans introspection, use the JavaBeans Introspector.
	 * @param beanClass the target class to introspect
	 * @return a collection of property descriptors
	 * @throws IntrospectionException from introspecting the given bean class
	 * @see java.beans.Introspector#getBeanInfo(Class)
	 */
	public static Collection<? extends PropertyDescriptor> determineBasicProperties(Class<?> beanClass)
			throws IntrospectionException {

		Map<String, BasicPropertyDescriptor> pdMap = new TreeMap<>();

		for (Method method : beanClass.getMethods()) {
			String methodName = method.getName();

			boolean setter;
			int nameIndex;
			if (methodName.startsWith("set") && method.getParameterCount() == 1) {
				setter = true;
				nameIndex = 3;
			}
			else if (methodName.startsWith("get") && method.getParameterCount() == 0 && method.getReturnType() != void.class) {
				setter = false;
				nameIndex = 3;
			}
			else if (methodName.startsWith("is") && method.getParameterCount() == 0 && method.getReturnType() == boolean.class) {
				setter = false;
				nameIndex = 2;
			}
			else {
				continue;
			}

			String propertyName = StringUtils.uncapitalizeAsProperty(methodName.substring(nameIndex));
			if (propertyName.isEmpty()) {
				continue;
			}

			BasicPropertyDescriptor pd = pdMap.get(propertyName);
			if (pd != null) {
				if (setter) {
					pd.addWriteMethod(method);
				}
				else {
					Method readMethod = pd.getReadMethod();
					if (readMethod == null || readMethod.getReturnType().isAssignableFrom(method.getReturnType())) {
						pd.setReadMethod(method);
					}
				}
			}
			else {
				pd = new BasicPropertyDescriptor(propertyName, beanClass, (!setter ? method : null), (setter ? method : null));
				pdMap.put(propertyName, pd);
			}
		}

		return pdMap.values();
	}

	/**
	 * Compare the given {@code PropertyDescriptors} and return {@code true} if
	 * they are equivalent, i.e. their read method, write method, property type,
	 * and flags are equivalent.
	 * @see java.beans.PropertyDescriptor#equals(Object)
	 */
	public static boolean equals(PropertyDescriptor pd, PropertyDescriptor otherPd) {
		return (ObjectUtils.nullSafeEquals(pd.getReadMethod(), otherPd.getReadMethod()) &&
				ObjectUtils.nullSafeEquals(pd.getWriteMethod(), otherPd.getWriteMethod()) &&
				ObjectUtils.nullSafeEquals(pd.getPropertyType(), otherPd.getPropertyType()) &&
				pd.isBound() == otherPd.isBound() && pd.isConstrained() == otherPd.isConstrained());
	}


	/**
	 * PropertyDescriptor for {@link #determineBasicProperties(Class)},
	 * not performing any early type determination for
	 * {@link #setReadMethod}/{@link #setWriteMethod}.
	 */
	private static class BasicPropertyDescriptor extends PropertyDescriptor {

		private final Class<?> beanClass;

		private Method readMethod;

		private Method writeMethod;

		private final List<Method> candidateWriteMethods = new ArrayList<>();

		public BasicPropertyDescriptor(String propertyName, Class<?> beanClass, Method readMethod, Method writeMethod)
				throws IntrospectionException {

			super(propertyName, readMethod, writeMethod);
			this.beanClass = beanClass;
		}

		@Override
		public void setReadMethod(Method readMethod) {
			this.readMethod = readMethod;
		}

		@Override
		public Method getReadMethod() {
			return this.readMethod;
		}

		@Override
		public void setWriteMethod(Method writeMethod) {
			this.writeMethod = writeMethod;
		}

		void addWriteMethod(Method writeMethod) {
			// Since setWriteMethod() is invoked from the PropertyDescriptor(String, Method, Method)
			// constructor, this.writeMethod may be non-null.
			if (this.writeMethod != null) {
				this.candidateWriteMethods.add(this.writeMethod);
				this.writeMethod = null;
			}
			this.candidateWriteMethods.add(writeMethod);
		}

		@Override
		public Method getWriteMethod() {
			if (this.writeMethod == null && !this.candidateWriteMethods.isEmpty()) {
				if (this.readMethod == null || this.candidateWriteMethods.size() == 1) {
					this.writeMethod = this.candidateWriteMethods.get(0);
				}
				else {
					Class<?> resolvedReadType =
							ResolvableType.forMethodReturnType(this.readMethod, this.beanClass).toClass();
					for (Method method : this.candidateWriteMethods) {
						// 1) Check for an exact match against the resolved types.
						Class<?> resolvedWriteType =
								ResolvableType.forMethodParameter(method, 0, this.beanClass).toClass();
						if (resolvedReadType.equals(resolvedWriteType)) {
							this.writeMethod = method;
							break;
						}

						// 2) Check if the candidate write method's parameter type is compatible with
						// the read method's return type.
						Class<?> parameterType = method.getParameterTypes()[0];
						if (this.readMethod.getReturnType().isAssignableFrom(parameterType)) {
							// If we haven't yet found a compatible write method, or if the current
							// candidate's parameter type is a subtype of the previous candidate's
							// parameter type, track the current candidate as the write method.
							if (this.writeMethod == null ||
									this.writeMethod.getParameterTypes()[0].isAssignableFrom(parameterType)) {
								this.writeMethod = method;
								// We do not "break" here, since we need to compare the current candidate
								// with all remaining candidates.
							}
						}
					}
				}
			}
			return this.writeMethod;
		}
	}

}
