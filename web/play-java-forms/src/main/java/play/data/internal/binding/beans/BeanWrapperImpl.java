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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import play.data.internal.binding.core.ResolvableType;
import play.data.internal.binding.core.convert.TypeDescriptor;
import play.data.internal.binding.util.Assert;
import play.data.internal.binding.util.ReflectionUtils;

/**
 * Bean property accessor implementation that should be sufficient
 * for all typical use cases. Caches introspection results for efficiency.
 *
 * <p><b>NOTE: This is - for almost all purposes - an
 * internal class.</b> It is just public in order to allow for access from
 * other framework packages.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Stephane Nicoll
 * @see #setPropertyValues
 * @see #setPropertyValue
 * @see #getPropertyValue
 * @see #getPropertyType
 */
public class BeanWrapperImpl extends AbstractNestablePropertyAccessor {

	/**
	 * Cached introspections results for this object, to prevent encountering
	 * the cost of JavaBeans introspection every time.
	 */
	private CachedIntrospectionResults cachedIntrospectionResults;


	/**
	 * Create a new BeanWrapperImpl for the given object.
	 * @param object the object wrapped by this accessor
	 */
	public BeanWrapperImpl(Object object) {
		super(object);
	}

	/**
	 * Create a new BeanWrapperImpl for the given object,
	 * registering a nested path that the object is in.
	 * @param object the object wrapped by this accessor
	 * @param nestedPath the nested path of the object
	 * @param parent the containing accessor (must not be {@code null})
	 */
	private BeanWrapperImpl(Object object, String nestedPath, BeanWrapperImpl parent) {
		super(object, nestedPath, parent);
	}


	@Override
	public void setWrappedInstance(Object object, String nestedPath, Object rootObject) {
		super.setWrappedInstance(object, nestedPath, rootObject);
		setIntrospectionClass(getWrappedClass());
	}

	/**
	 * Set the class to introspect.
	 * Needs to be called when the target object changes.
	 * @param clazz the class to introspect
	 */
	protected void setIntrospectionClass(Class<?> clazz) {
		if (this.cachedIntrospectionResults != null && this.cachedIntrospectionResults.getBeanClass() != clazz) {
			this.cachedIntrospectionResults = null;
		}
	}

	/**
	 * Obtain a lazily initialized CachedIntrospectionResults instance
	 * for the wrapped object.
	 */
	private CachedIntrospectionResults getCachedIntrospectionResults() {
		if (this.cachedIntrospectionResults == null) {
			this.cachedIntrospectionResults = CachedIntrospectionResults.forClass(getWrappedClass());
		}
		return this.cachedIntrospectionResults;
	}


	@Override
	protected PropertyHandler getLocalPropertyHandler(String propertyName) {
		PropertyDescriptor pd = getCachedIntrospectionResults().getPropertyDescriptor(propertyName);
		return (pd != null ? new BeanPropertyHandler((GenericTypeAwarePropertyDescriptor) pd) : null);
	}

	@Override
	protected BeanWrapperImpl newNestedPropertyAccessor(Object object, String nestedPath) {
		return new BeanWrapperImpl(object, nestedPath, this);
	}

	@Override
	protected NotWritablePropertyException createNotWritablePropertyException(String propertyName) {
		throw new NotWritablePropertyException(getRootClass(), getNestedPath() + propertyName);
	}

	private class BeanPropertyHandler extends PropertyHandler {

		private final GenericTypeAwarePropertyDescriptor pd;

		public BeanPropertyHandler(GenericTypeAwarePropertyDescriptor pd) {
			super(pd.getPropertyType(), pd.getReadMethod() != null, pd.getWriteMethod() != null);
			this.pd = pd;
		}

		@Override
		public TypeDescriptor toTypeDescriptor() {
			return this.pd.getTypeDescriptor();
		}

		@Override
		public ResolvableType getResolvableType() {
			return this.pd.getReadMethodType();
		}

		@Override
		public TypeDescriptor getMapValueType(int nestingLevel) {
			return new TypeDescriptor(
					this.pd.getReadMethodType().getNested(nestingLevel).asMap().getGeneric(1),
					null, this.pd.getTypeDescriptor().getAnnotations());
		}

		@Override
		public TypeDescriptor getCollectionType(int nestingLevel) {
			return new TypeDescriptor(
					this.pd.getReadMethodType().getNested(nestingLevel).asCollection().getGeneric(),
					null, this.pd.getTypeDescriptor().getAnnotations());
		}

		@Override
		public TypeDescriptor nested(int level) {
			return this.pd.getTypeDescriptor().nested(level);
		}

		@Override
		public Object getValue() throws Exception {
			Method readMethod = this.pd.getReadMethod();
			Assert.state(readMethod != null, "No read method available");
			ReflectionUtils.makeAccessible(readMethod);
			return readMethod.invoke(getWrappedInstance(), (Object[]) null);
		}

		@Override
		public void setValue(Object value) throws Exception {
			Method writeMethod = this.pd.getWriteMethodForActualAccess();
			ReflectionUtils.makeAccessible(writeMethod);
			writeMethod.invoke(getWrappedInstance(), value);
		}
	}

}
