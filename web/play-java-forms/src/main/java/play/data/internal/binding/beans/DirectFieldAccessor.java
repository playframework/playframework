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

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.util.HashMap;
import java.util.Map;

import play.data.internal.binding.core.ResolvableType;
import play.data.internal.binding.core.convert.TypeDescriptor;
import play.data.internal.binding.util.ReflectionUtils;

/**
 * {@link ConfigurablePropertyAccessor} implementation that directly accesses
 * instance fields. Allows for direct binding to fields instead of going through
 * JavaBean setters.
 *
 * <p>The vast majority of the {@link BeanWrapperImpl} features have
 * been merged to {@link AbstractPropertyAccessor}, which means that property
 * traversal as well as collections and map access is now supported here as well.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see BeanWrapperImpl
 * @see play.data.internal.binding.validation.DirectFieldBindingResult
 * @see play.data.internal.binding.validation.DataBinder#initDirectFieldAccess()
 */
public class DirectFieldAccessor extends AbstractNestablePropertyAccessor {

	private final Map<String, FieldPropertyHandler> fieldMap = new HashMap<>();


	/**
	 * Create a new DirectFieldAccessor for the given object.
	 * @param object the object wrapped by this DirectFieldAccessor
	 */
	public DirectFieldAccessor(Object object) {
		super(object);
	}

	/**
	 * Create a new DirectFieldAccessor for the given object,
	 * registering a nested path that the object is in.
	 * @param object the object wrapped by this DirectFieldAccessor
	 * @param nestedPath the nested path of the object
	 * @param parent the containing DirectFieldAccessor (must not be {@code null})
	 */
	protected DirectFieldAccessor(Object object, String nestedPath, DirectFieldAccessor parent) {
		super(object, nestedPath, parent);
	}


	@Override
	protected PropertyHandler getLocalPropertyHandler(String propertyName) {
		FieldPropertyHandler propertyHandler = this.fieldMap.get(propertyName);
		if (propertyHandler == null) {
			Field field = ReflectionUtils.findField(getWrappedClass(), propertyName);
			if (field != null) {
				propertyHandler = new FieldPropertyHandler(field);
				this.fieldMap.put(propertyName, propertyHandler);
			}
		}
		return propertyHandler;
	}

	@Override
	protected DirectFieldAccessor newNestedPropertyAccessor(Object object, String nestedPath) {
		return new DirectFieldAccessor(object, nestedPath, this);
	}

	@Override
	protected NotWritablePropertyException createNotWritablePropertyException(String propertyName) {
		throw new NotWritablePropertyException(getRootClass(), getNestedPath() + propertyName);
	}


	private class FieldPropertyHandler extends PropertyHandler {

		private final Field field;

		private final ResolvableType resolvableType;

		public FieldPropertyHandler(Field field) {
			super(field.getType(), true, true);
			this.field = field;
			this.resolvableType = ResolvableType.forField(this.field);
		}

		@Override
		public TypeDescriptor toTypeDescriptor() {
			return new TypeDescriptor(this.resolvableType, this.field.getType(), this.field.getAnnotations());
		}

		@Override
		public ResolvableType getResolvableType() {
			return this.resolvableType;
		}

		@Override
		public TypeDescriptor getMapValueType(int nestingLevel) {
			return new TypeDescriptor(this.resolvableType.getNested(nestingLevel).asMap().getGeneric(1),
					null, this.field.getAnnotations());
		}

		@Override
		public TypeDescriptor getCollectionType(int nestingLevel) {
			return new TypeDescriptor(this.resolvableType.getNested(nestingLevel).asCollection().getGeneric(),
					null, this.field.getAnnotations());
		}

		@Override
		public TypeDescriptor nested(int level) {
			return TypeDescriptor.nested(this.field, level);
		}

		@Override
		public Object getValue() throws Exception {
			try {
				ReflectionUtils.makeAccessible(this.field);
				return this.field.get(getWrappedInstance());
			}
			catch (IllegalAccessException | InaccessibleObjectException ex) {
				throw new InvalidPropertyException(getWrappedClass(),
						this.field.getName(), "Field is not accessible", ex);
			}
		}

		@Override
		public void setValue(Object value) throws Exception {
			try {
				ReflectionUtils.makeAccessible(this.field);
				this.field.set(getWrappedInstance(), value);
			}
			catch (IllegalAccessException | InaccessibleObjectException ex) {
				throw new InvalidPropertyException(getWrappedClass(), this.field.getName(),
						"Field is not accessible", ex);
			}
		}
	}

}
