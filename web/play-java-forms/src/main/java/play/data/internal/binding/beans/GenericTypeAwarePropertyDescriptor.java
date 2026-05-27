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
import java.util.Objects;

import play.data.internal.binding.core.BridgeMethodResolver;
import play.data.internal.binding.core.MethodParameter;
import play.data.internal.binding.core.ResolvableType;
import play.data.internal.binding.core.convert.Property;
import play.data.internal.binding.core.convert.TypeDescriptor;
import play.data.internal.binding.util.Assert;
import play.data.internal.binding.util.ClassUtils;
import play.data.internal.binding.util.StringUtils;

/**
 * Extension of the standard JavaBeans {@link PropertyDescriptor} class,
 * overriding {@code getPropertyType()} such that a generically declared
 * type variable will be resolved against the containing bean class.
 *
 * @author Juergen Hoeller
 */
final class GenericTypeAwarePropertyDescriptor extends PropertyDescriptor {

	private final Class<?> beanClass;

	private final Method readMethod;

	private final Method writeMethod;

	private MethodParameter writeMethodParameter;

	private ResolvableType readMethodType;

	private volatile TypeDescriptor typeDescriptor;

	private Class<?> propertyType;

	public GenericTypeAwarePropertyDescriptor(Class<?> beanClass, String propertyName,
			Method readMethod, Method writeMethod) throws IntrospectionException {

		super(propertyName, null, null);
		this.beanClass = beanClass;

		Method readMethodToUse = (readMethod != null ? BridgeMethodResolver.findBridgedMethod(readMethod) : null);
		Method writeMethodToUse = (writeMethod != null ? BridgeMethodResolver.findBridgedMethod(writeMethod) : null);
		if (writeMethodToUse == null && readMethodToUse != null) {
			// Fallback: Original JavaBeans introspection might not have found matching setter
			// method due to lack of bridge method resolution, in case of the getter using a
			// covariant return type whereas the setter is defined for the concrete property type.
			Method candidate = ClassUtils.getMethodIfAvailable(
					this.beanClass, "set" + StringUtils.capitalize(getName()), (Class<?>[]) null);
			if (candidate != null && candidate.getParameterCount() == 1) {
				writeMethodToUse = candidate;
			}
		}
		this.readMethod = readMethodToUse;
		this.writeMethod = writeMethodToUse;

		if (this.writeMethod != null) {
			this.writeMethodParameter = new MethodParameter(this.writeMethod, 0).withContainingClass(this.beanClass);
		}

		if (this.readMethod != null) {
			this.readMethodType = ResolvableType.forMethodReturnType(this.readMethod, this.beanClass);
			this.propertyType = this.readMethodType.resolve(this.readMethod.getReturnType());
		}
		else if (this.writeMethodParameter != null) {
			this.propertyType = this.writeMethodParameter.getParameterType();
		}
	}


	public Class<?> getBeanClass() {
		return this.beanClass;
	}

	@Override
	public Method getReadMethod() {
		return this.readMethod;
	}

	@Override
	public Method getWriteMethod() {
		return this.writeMethod;
	}

	public Method getWriteMethodForActualAccess() {
		Assert.state(this.writeMethod != null, "No write method available");
		return this.writeMethod;
	}

	public ResolvableType getReadMethodType() {
		Assert.state(this.readMethodType != null, "No read method available");
		return this.readMethodType;
	}

	public TypeDescriptor getTypeDescriptor() {
		TypeDescriptor typeDescriptor = this.typeDescriptor;
		if (typeDescriptor == null) {
			Property property = new Property(getBeanClass(), getReadMethod(), getWriteMethod(), getName());
			typeDescriptor = new TypeDescriptor(property);
			this.typeDescriptor = typeDescriptor;
		}
		return typeDescriptor;
	}

	@Override
	public Class<?> getPropertyType() {
		return this.propertyType;
	}

	@Override
	public boolean equals(Object other) {
		return (this == other || (other instanceof GenericTypeAwarePropertyDescriptor that &&
				getBeanClass().equals(that.getBeanClass()) &&
				PropertyDescriptorUtils.equals(this, that)));
	}

	@Override
	public int hashCode() {
		return Objects.hash(getBeanClass(), getReadMethod(), getWriteMethod());
	}

}
