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

import java.beans.PropertyChangeEvent;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import play.data.internal.binding.core.CollectionFactory;
import play.data.internal.binding.core.ResolvableType;
import play.data.internal.binding.core.convert.ConversionException;
import play.data.internal.binding.core.convert.ConverterNotFoundException;
import play.data.internal.binding.core.convert.TypeDescriptor;
import play.data.internal.binding.util.Assert;
import play.data.internal.binding.util.ObjectUtils;
import play.data.internal.binding.util.StringUtils;

/**
 * A basic {@link ConfigurablePropertyAccessor} that provides the necessary
 * infrastructure for all typical use cases.
 *
 * <p>This accessor will convert collection and array values to the corresponding
 * target collections or arrays, if necessary.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Sam Brannen
 * @see #setPropertyValues
 * @see #setPropertyValue
 * @see #getPropertyValue
 * @see #getPropertyType
 */
public abstract class AbstractNestablePropertyAccessor extends AbstractPropertyAccessor {

	/**
	 * We'll create a lot of these objects, so we don't want a new logger every time.
	 */
	private static final Log logger = LogFactory.getLog(AbstractNestablePropertyAccessor.class);

	Object wrappedObject;

	private String nestedPath = "";

	Object rootObject;

	/** Map with cached nested Accessors: nested path -> Accessor instance. */
	private Map<String, AbstractNestablePropertyAccessor> nestedPropertyAccessors;


	/**
	 * Create a new accessor for the given object.
	 * @param object the object wrapped by this accessor
	 */
	protected AbstractNestablePropertyAccessor(Object object) {
		setWrappedInstance(object);
	}

	/**
	 * Create a new accessor for the given object,
	 * registering a nested path that the object is in.
	 * @param object the object wrapped by this accessor
	 * @param nestedPath the nested path of the object
	 * @param parent the containing accessor (must not be {@code null})
	 */
	protected AbstractNestablePropertyAccessor(Object object, String nestedPath, AbstractNestablePropertyAccessor parent) {
		setWrappedInstance(object, nestedPath, parent.getWrappedInstance());
		setAutoGrowNestedPaths(parent.isAutoGrowNestedPaths());
		setAutoGrowCollectionLimit(parent.getAutoGrowCollectionLimit());
		setConversionService(parent.getConversionService());
	}


	/**
	 * Switch the target object, replacing the cached introspection results only
	 * if the class of the new object is different to that of the replaced object.
	 * @param object the new target object
	 */
	public void setWrappedInstance(Object object) {
		setWrappedInstance(object, "", null);
	}

	/**
	 * Switch the target object, replacing the cached introspection results only
	 * if the class of the new object is different to that of the replaced object.
	 * @param object the new target object
	 * @param nestedPath the nested path of the object
	 * @param rootObject the root object at the top of the path
	 */
	public void setWrappedInstance(Object object, String nestedPath, Object rootObject) {
		this.wrappedObject = ObjectUtils.unwrapOptional(object);
		Assert.notNull(this.wrappedObject, "Target object must not be null");
		this.nestedPath = (nestedPath != null ? nestedPath : "");
		this.rootObject = (!this.nestedPath.isEmpty() ? rootObject : this.wrappedObject);
		this.nestedPropertyAccessors = null;
		this.typeConverterDelegate = new TypeConverterDelegate(this);
	}

	public final Object getWrappedInstance() {
		Assert.state(this.wrappedObject != null, "No wrapped object");
		return this.wrappedObject;
	}

	public final Class<?> getWrappedClass() {
		return getWrappedInstance().getClass();
	}

	/**
	 * Return the nested path of the object wrapped by this accessor.
	 */
	public final String getNestedPath() {
		return this.nestedPath;
	}

	/**
	 * Return the root object at the top of the path of this accessor.
	 * @see #getNestedPath
	 */
	public final Object getRootInstance() {
		Assert.state(this.rootObject != null, "No root object");
		return this.rootObject;
	}

	/**
	 * Return the class of the root object at the top of the path of this accessor.
	 * @see #getNestedPath
	 */
	public final Class<?> getRootClass() {
		return getRootInstance().getClass();
	}

	@Override
	public void setPropertyValue(String propertyName, Object value, Locale locale) throws BeansException {
		AbstractNestablePropertyAccessor nestedPa;
		try {
			nestedPa = getPropertyAccessorForPropertyPath(propertyName, locale);
		}
		catch (NotReadablePropertyException ex) {
			throw new NotWritablePropertyException(getRootClass(), this.nestedPath + propertyName,
					"Nested property in path '" + propertyName + "' does not exist", ex);
		}
		PropertyTokenHolder tokens = getPropertyNameTokens(getFinalPath(nestedPa, propertyName));
		nestedPa.setPropertyValue(tokens, new PropertyValue(propertyName, value), locale);
	}

	@Override
	public void setPropertyValue(PropertyValue pv, Locale locale) throws BeansException {
		PropertyTokenHolder tokens = (PropertyTokenHolder) pv.resolvedTokens;
		if (tokens == null) {
			String propertyName = pv.getName();
			AbstractNestablePropertyAccessor nestedPa;
			try {
				nestedPa = getPropertyAccessorForPropertyPath(propertyName, locale);
			}
			catch (NotReadablePropertyException ex) {
				throw new NotWritablePropertyException(getRootClass(), this.nestedPath + propertyName,
						"Nested property in path '" + propertyName + "' does not exist", ex);
			}
			tokens = getPropertyNameTokens(getFinalPath(nestedPa, propertyName));
			if (nestedPa == this) {
				pv.getOriginalPropertyValue().resolvedTokens = tokens;
			}
			nestedPa.setPropertyValue(tokens, pv, locale);
		}
		else {
			setPropertyValue(tokens, pv, locale);
		}
	}

	protected void setPropertyValue(PropertyTokenHolder tokens, PropertyValue pv, Locale locale) throws BeansException {
		if (tokens.keys != null) {
			processKeyedProperty(tokens, pv, locale);
		}
		else {
			processLocalProperty(tokens, pv, locale);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void processKeyedProperty(PropertyTokenHolder tokens, PropertyValue pv, Locale locale) {
		Object propValue = getPropertyHoldingValue(tokens, locale);
		PropertyHandler ph = getLocalPropertyHandler(tokens.actualName);
		if (ph == null) {
			throw new InvalidPropertyException(
					getRootClass(), this.nestedPath + tokens.actualName, "No property handler found");
		}
		Assert.state(tokens.keys != null, "No token keys");
		String lastKey = tokens.keys[tokens.keys.length - 1];

		if (propValue.getClass().isArray()) {
			Class<?> componentType = propValue.getClass().componentType();
			int arrayIndex = Integer.parseInt(lastKey);
			try {
				Object convertedValue = convertIfNecessary(tokens.canonicalName, pv.getValue(),
						componentType, ph.nested(tokens.keys.length), locale);
				int length = Array.getLength(propValue);
				if (arrayIndex >= length && arrayIndex < getAutoGrowCollectionLimit()) {
					Object newArray = Array.newInstance(componentType, arrayIndex + 1);
					System.arraycopy(propValue, 0, newArray, 0, length);
					int lastKeyIndex = tokens.canonicalName.lastIndexOf('[');
					String propName = tokens.canonicalName.substring(0, lastKeyIndex);
					setPropertyValue(propName, newArray, locale);
					propValue = getPropertyValue(propName, locale);
				}
				Array.set(propValue, arrayIndex, convertedValue);
			}
			catch (IndexOutOfBoundsException ex) {
				throw new InvalidPropertyException(getRootClass(), this.nestedPath + tokens.canonicalName,
						"Invalid array index in property path '" + tokens.canonicalName + "'", ex);
			}
		}

		else if (propValue instanceof List list) {
			TypeDescriptor requiredType = ph.getCollectionType(tokens.keys.length);
			int index = Integer.parseInt(lastKey);
			Object convertedValue = convertIfNecessary(tokens.canonicalName, pv.getValue(),
					requiredType.getResolvableType().resolve(), requiredType, locale);
			int size = list.size();
			if (index >= size && index < getAutoGrowCollectionLimit()) {
				for (int i = size; i < index; i++) {
					try {
						list.add(null);
					}
					catch (NullPointerException ex) {
						throw new InvalidPropertyException(getRootClass(), this.nestedPath + tokens.canonicalName,
								"Cannot set element with index " + index + " in List of size " +
								size + ", accessed using property path '" + tokens.canonicalName +
								"': List does not support filling up gaps with null elements");
					}
				}
				list.add(convertedValue);
			}
			else {
				try {
					list.set(index, convertedValue);
				}
				catch (IndexOutOfBoundsException ex) {
					throw new InvalidPropertyException(getRootClass(), this.nestedPath + tokens.canonicalName,
							"Invalid list index in property path '" + tokens.canonicalName + "'", ex);
				}
			}
		}

		else if (propValue instanceof Map map) {
			TypeDescriptor mapKeyType = ph.getMapKeyType(tokens.keys.length);
			TypeDescriptor mapValueType = ph.getMapValueType(tokens.keys.length);
			// IMPORTANT: Do not pass full property name in here.
			Object convertedMapKey = convertIfNecessary(null, lastKey,
					mapKeyType.getResolvableType().resolve(), mapKeyType, locale);
			Object convertedMapValue = convertIfNecessary(tokens.canonicalName, pv.getValue(),
					mapValueType.getResolvableType().resolve(), mapValueType, locale);
			map.put(convertedMapKey, convertedMapValue);
		}

		else {
			throw new InvalidPropertyException(getRootClass(), this.nestedPath + tokens.canonicalName,
					"Property referenced in indexed property path '" + tokens.canonicalName +
					"' is neither an array nor a List nor a Map; returned value was [" + propValue + "]");
		}
	}

	private Object getPropertyHoldingValue(PropertyTokenHolder tokens, Locale locale) {
		// Apply indexes and map keys: fetch value for all keys but the last one.
		Assert.state(tokens.keys != null, "No token keys");
		PropertyTokenHolder getterTokens = new PropertyTokenHolder(tokens.actualName);
		getterTokens.canonicalName = tokens.canonicalName;
		getterTokens.keys = new String[tokens.keys.length - 1];
		System.arraycopy(tokens.keys, 0, getterTokens.keys, 0, tokens.keys.length - 1);

		Object propValue;
		try {
			propValue = getPropertyValue(getterTokens, locale);
		}
		catch (NotReadablePropertyException ex) {
			throw new NotWritablePropertyException(getRootClass(), this.nestedPath + tokens.canonicalName,
					"Cannot access indexed value in property referenced " +
					"in indexed property path '" + tokens.canonicalName + "'", ex);
		}

		if (propValue == null) {
			// null map value case
			if (isAutoGrowNestedPaths()) {
				int lastKeyIndex = tokens.canonicalName.lastIndexOf('[');
				getterTokens.canonicalName = tokens.canonicalName.substring(0, lastKeyIndex);
				propValue = setDefaultValue(getterTokens, locale);
			}
			else {
				throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + tokens.canonicalName,
						"Cannot access indexed value in property referenced " +
						"in indexed property path '" + tokens.canonicalName + "': returned null");
			}
		}
		return propValue;
	}

	private void processLocalProperty(PropertyTokenHolder tokens, PropertyValue pv, Locale locale) {
		PropertyHandler ph = getLocalPropertyHandler(tokens.actualName);
		if (ph == null || !ph.isWritable()) {
			throw createNotWritablePropertyException(tokens.canonicalName);
		}

		try {
			Object originalValue = pv.getValue();
			Object valueToApply = originalValue;
			if (!Boolean.FALSE.equals(pv.conversionNecessary)) {
				if (pv.isConverted()) {
					valueToApply = pv.getConvertedValue();
				}
				else {
					valueToApply = convertForProperty(
							tokens.canonicalName, originalValue, ph.toTypeDescriptor(), locale);
				}
				pv.getOriginalPropertyValue().conversionNecessary = (valueToApply != originalValue);
			}
			ph.setValue(valueToApply);
		}
		catch (TypeMismatchException ex) {
			throw ex;
		}
		catch (InvocationTargetException ex) {
			PropertyChangeEvent propertyChangeEvent = new PropertyChangeEvent(
					getRootInstance(), this.nestedPath + tokens.canonicalName, null, pv.getValue());
			if (ex.getTargetException() instanceof ClassCastException) {
				throw new TypeMismatchException(propertyChangeEvent, ph.getPropertyType(), ex.getTargetException());
			}
			else {
				Throwable cause = ex.getTargetException();
				if (cause instanceof UndeclaredThrowableException) {
					// May happen, for example, with Groovy-generated methods
					cause = cause.getCause();
				}
				throw new MethodInvocationException(propertyChangeEvent, cause);
			}
		}
		catch (Exception ex) {
			PropertyChangeEvent pce = new PropertyChangeEvent(
					getRootInstance(), this.nestedPath + tokens.canonicalName, null, pv.getValue());
			throw new MethodInvocationException(pce, ex);
		}
	}

	@Override
	public Class<?> getPropertyType(String propertyName, Locale locale) throws BeansException {
		if (this.wrappedObject == null) {
			return null;
		}
		try {
			PropertyHandler ph = getPropertyHandler(propertyName, locale);
			if (ph != null) {
				return ph.getPropertyType();
			}
			else {
				// Maybe an indexed/mapped property...
				Object value = getPropertyValue(propertyName, locale);
				if (value != null) {
					return value.getClass();
				}
			}
		}
		catch (InvalidPropertyException ex) {
			// Consider as not determinable.
		}
		return null;
	}

	@Override
	public TypeDescriptor getPropertyTypeDescriptor(String propertyName, Locale locale) throws BeansException {
		try {
			AbstractNestablePropertyAccessor nestedPa = getPropertyAccessorForPropertyPath(propertyName, locale);
			String finalPath = getFinalPath(nestedPa, propertyName);
			PropertyTokenHolder tokens = getPropertyNameTokens(finalPath);
			PropertyHandler ph = nestedPa.getLocalPropertyHandler(tokens.actualName);
			if (ph != null) {
				if (tokens.keys != null) {
					if (ph.isReadable() || ph.isWritable()) {
						return ph.nested(tokens.keys.length);
					}
				}
				else {
					if (ph.isReadable() || ph.isWritable()) {
						return ph.toTypeDescriptor();
					}
				}
			}
		}
		catch (InvalidPropertyException ex) {
			// Consider as not determinable.
		}
		return null;
	}

	@Override
	public boolean isReadableProperty(String propertyName, Locale locale) {
		try {
			PropertyHandler ph = getPropertyHandler(propertyName, locale);
			if (ph != null) {
				return ph.isReadable();
			}
			else {
				// Maybe an indexed/mapped property...
				getPropertyValue(propertyName, locale);
				return true;
			}
		}
		catch (InvalidPropertyException ex) {
			// Cannot be evaluated, so can't be readable.
		}
		return false;
	}

	private Object convertIfNecessary(String propertyName, Object newValue, Class<?> requiredType, TypeDescriptor td, Locale locale)
			throws TypeMismatchException {

		Assert.state(this.typeConverterDelegate != null, "No TypeConverterDelegate");
		try {
			return this.typeConverterDelegate.convertIfNecessary(propertyName, newValue, requiredType, td, locale);
		}
		catch (ConverterNotFoundException | IllegalStateException ex) {
			PropertyChangeEvent pce =
					new PropertyChangeEvent(getRootInstance(), this.nestedPath + propertyName, null, newValue);
			throw new ConversionNotSupportedException(pce, requiredType, ex);
		}
		catch (ConversionException | IllegalArgumentException ex) {
			PropertyChangeEvent pce =
					new PropertyChangeEvent(getRootInstance(), this.nestedPath + propertyName, null, newValue);
			throw new TypeMismatchException(pce, requiredType, ex);
		}
	}

	protected Object convertForProperty(String propertyName, Object newValue, TypeDescriptor td, Locale locale)
			throws TypeMismatchException {

		return convertIfNecessary(propertyName, newValue, td.getType(), td, locale);
	}

	@Override
	public Object getPropertyValue(String propertyName, Locale locale) throws BeansException {
		AbstractNestablePropertyAccessor nestedPa = getPropertyAccessorForPropertyPath(propertyName, locale);
		PropertyTokenHolder tokens = getPropertyNameTokens(getFinalPath(nestedPa, propertyName));
		return nestedPa.getPropertyValue(tokens, locale);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	protected Object getPropertyValue(PropertyTokenHolder tokens, Locale locale) throws BeansException {
		String propertyName = tokens.canonicalName;
		String actualName = tokens.actualName;
		PropertyHandler ph = getLocalPropertyHandler(actualName);
		if (ph == null || !ph.isReadable()) {
			throw new NotReadablePropertyException(getRootClass(), this.nestedPath + propertyName);
		}
		try {
			Object value = ph.getValue();
			if (tokens.keys != null) {
				if (value == null) {
					if (isAutoGrowNestedPaths()) {
						value = setDefaultValue(new PropertyTokenHolder(tokens.actualName), locale);
					}
					else {
						throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + propertyName,
								"Cannot access indexed value of property referenced in indexed " +
										"property path '" + propertyName + "': returned null");
					}
				}
				StringBuilder indexedPropertyName = new StringBuilder(tokens.actualName);
				// apply indexes and map keys
				for (int i = 0; i < tokens.keys.length; i++) {
					String key = tokens.keys[i];
					if (value == null) {
						throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + propertyName,
								"Cannot access indexed value of property referenced in indexed " +
										"property path '" + propertyName + "': returned null");
					}
					else if (value.getClass().isArray()) {
						int index = Integer.parseInt(key);
						value = growArrayIfNecessary(value, index, indexedPropertyName.toString(), locale);
						value = Array.get(value, index);
					}
					else if (value instanceof List list) {
						int index = Integer.parseInt(key);
						growCollectionIfNecessary(list, index, indexedPropertyName.toString(), ph, i + 1);
						value = list.get(index);
					}
					else if (value instanceof Map map) {
						Class<?> mapKeyType = ph.getResolvableType().getNested(i + 1).asMap().resolveGeneric(0);
						// IMPORTANT: Do not pass full property name in here.
						TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(mapKeyType);
						Object convertedMapKey = convertIfNecessary(null, key, mapKeyType, typeDescriptor, locale);
						value = map.get(convertedMapKey);
					}
					else if (value instanceof Iterable iterable) {
						// Apply index to Iterator in case of a Set/Collection/Iterable.
						int index = Integer.parseInt(key);
						if (value instanceof Collection<?> coll) {
							if (index < 0 || index >= coll.size()) {
								throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
										"Cannot get element with index " + index + " from Collection of size " +
												coll.size() + ", accessed using property path '" + propertyName + "'");
							}
						}
						Iterator<Object> it = iterable.iterator();
						boolean found = false;
						int currIndex = 0;
						for (; it.hasNext(); currIndex++) {
							Object elem = it.next();
							if (currIndex == index) {
								value = elem;
								found = true;
								break;
							}
						}
						if (!found) {
							throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
									"Cannot get element with index " + index + " from Iterable of size " +
											currIndex + ", accessed using property path '" + propertyName + "'");
						}
					}
					else {
						throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
								"Property referenced in indexed property path '" + propertyName +
										"' is neither an array nor a List/Set/Collection/Iterable nor a Map; " +
										"returned value was [" + value + "]");
					}
					indexedPropertyName.append(PROPERTY_KEY_PREFIX).append(key).append(PROPERTY_KEY_SUFFIX);
				}
			}
			return value;
		}
		catch (InvalidPropertyException ex) {
			throw ex;
		}
		catch (IndexOutOfBoundsException ex) {
			throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
					"Index of out of bounds in property path '" + propertyName + "'", ex);
		}
		catch (NumberFormatException | TypeMismatchException ex) {
			throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
					"Invalid index in property path '" + propertyName + "'", ex);
		}
		catch (InvocationTargetException ex) {
			throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
					"Getter for property '" + actualName + "' threw exception", ex);
		}
		catch (Exception ex) {
			throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
					"Illegal attempt to get property '" + actualName + "' threw exception", ex);
		}
	}


	/**
	 * Return the {@link PropertyHandler} for the specified {@code propertyName}, navigating
	 * if necessary. Return {@code null} if not found rather than throwing an exception.
	 * @param propertyName the property to obtain the descriptor for
	 * @return the property descriptor for the specified property,
	 * or {@code null} if not found
	 * @throws BeansException in case of introspection failure
	 */
	protected PropertyHandler getPropertyHandler(String propertyName, Locale locale) throws BeansException {
		Assert.notNull(propertyName, "Property name must not be null");
		AbstractNestablePropertyAccessor nestedPa = getPropertyAccessorForPropertyPath(propertyName, locale);
		return nestedPa.getLocalPropertyHandler(getFinalPath(nestedPa, propertyName));
	}

	/**
	 * Return a {@link PropertyHandler} for the specified local {@code propertyName}.
	 * Only used to reach a property available in the current context.
	 * @param propertyName the name of a local property
	 * @return the handler for that property, or {@code null} if it has not been found
	 */
	protected abstract PropertyHandler getLocalPropertyHandler(String propertyName);

	/**
	 * Create a new nested property accessor instance.
	 * Can be overridden in subclasses to create a PropertyAccessor subclass.
	 * @param object the object wrapped by this PropertyAccessor
	 * @param nestedPath the nested path of the object
	 * @return the nested PropertyAccessor instance
	 */
	protected abstract AbstractNestablePropertyAccessor newNestedPropertyAccessor(Object object, String nestedPath);

	/**
	 * Create a {@link NotWritablePropertyException} for the specified property.
	 */
	protected abstract NotWritablePropertyException createNotWritablePropertyException(String propertyName);


	private Object growArrayIfNecessary(Object array, int index, String name, Locale locale) {
		if (!isAutoGrowNestedPaths()) {
			return array;
		}
		int length = Array.getLength(array);
		if (index >= length && index < getAutoGrowCollectionLimit()) {
			Class<?> componentType = array.getClass().componentType();
			Object newArray = Array.newInstance(componentType, index + 1);
			System.arraycopy(array, 0, newArray, 0, length);
			for (int i = length; i < Array.getLength(newArray); i++) {
				Array.set(newArray, i, newValue(componentType, null, name));
			}
			setPropertyValue(name, newArray, locale);
			Object defaultValue = getPropertyValue(name, locale);
			Assert.state(defaultValue != null, "Default value must not be null");
			return defaultValue;
		}
		else {
			return array;
		}
	}

	private void growCollectionIfNecessary(Collection<Object> collection, int index, String name,
			PropertyHandler ph, int nestingLevel) {

		if (!isAutoGrowNestedPaths()) {
			return;
		}
		int size = collection.size();
		if (index >= size && index < getAutoGrowCollectionLimit()) {
			Class<?> elementType = ph.getResolvableType().getNested(nestingLevel).asCollection().resolveGeneric();
			if (elementType != null) {
				for (int i = collection.size(); i < index + 1; i++) {
					collection.add(newValue(elementType, null, name));
				}
			}
		}
	}

	/**
	 * Get the last component of the path. Also works if not nested.
	 * @param pa property accessor to work on
	 * @param nestedPath property path we know is nested
	 * @return last component of the path (the property on the target bean)
	 */
	protected String getFinalPath(AbstractNestablePropertyAccessor pa, String nestedPath) {
		if (pa == this) {
			return nestedPath;
		}
		return nestedPath.substring(PropertyAccessorUtils.getLastNestedPropertySeparatorIndex(nestedPath) + 1);
	}

	/**
	 * Recursively navigate to return a property accessor for the nested property path.
	 * @param propertyPath property path, which may be nested
	 * @return a property accessor for the target bean
	 */
	protected AbstractNestablePropertyAccessor getPropertyAccessorForPropertyPath(String propertyPath, Locale locale) {
		int pos = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(propertyPath);
		// Handle nested properties recursively.
		if (pos > -1) {
			String nestedProperty = propertyPath.substring(0, pos);
			String nestedPath = propertyPath.substring(pos + 1);
			AbstractNestablePropertyAccessor nestedPa = getNestedPropertyAccessor(nestedProperty, locale);
			return nestedPa.getPropertyAccessorForPropertyPath(nestedPath, locale);
		}
		else {
			return this;
		}
	}

	/**
	 * Retrieve a Property accessor for the given nested property.
	 * Create a new one if not found in the cache.
	 * <p>Note: Caching nested PropertyAccessors is necessary now,
	 * to keep registered custom editors for nested properties.
	 * @param nestedProperty property to create the PropertyAccessor for
	 * @return the PropertyAccessor instance, either cached or newly created
	 */
	private AbstractNestablePropertyAccessor getNestedPropertyAccessor(String nestedProperty, Locale locale) {
		Map<String, AbstractNestablePropertyAccessor> nestedAccessors = this.nestedPropertyAccessors;
		if (nestedAccessors == null) {
			nestedAccessors = new HashMap<>();
			this.nestedPropertyAccessors = nestedAccessors;
		}
		// Get value of bean property.
		PropertyTokenHolder tokens = getPropertyNameTokens(nestedProperty);
		String canonicalName = tokens.canonicalName;
		Object value = getPropertyValue(tokens, locale);
		if (value == null || (value instanceof Optional<?> optional && optional.isEmpty())) {
			if (isAutoGrowNestedPaths()) {
				value = setDefaultValue(tokens, locale);
			}
			else {
				throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + canonicalName);
			}
		}

		// Lookup cached sub-PropertyAccessor, create new one if not found.
		AbstractNestablePropertyAccessor nestedPa = nestedAccessors.get(canonicalName);
		if (nestedPa == null || nestedPa.getWrappedInstance() != ObjectUtils.unwrapOptional(value)) {
			if (logger.isTraceEnabled()) {
				logger.trace("Creating new nested " + getClass().getSimpleName() + " for property '" + canonicalName + "'");
			}
			nestedPa = newNestedPropertyAccessor(value, this.nestedPath + canonicalName + NESTED_PROPERTY_SEPARATOR);
			nestedAccessors.put(canonicalName, nestedPa);
		}
		else {
			if (logger.isTraceEnabled()) {
				logger.trace("Using cached nested property accessor for property '" + canonicalName + "'");
			}
		}
		return nestedPa;
	}

	private Object setDefaultValue(PropertyTokenHolder tokens, Locale locale) {
		PropertyValue pv = createDefaultPropertyValue(tokens, locale);
		setPropertyValue(tokens, pv, locale);
		Object defaultValue = getPropertyValue(tokens, locale);
		Assert.state(defaultValue != null, "Default value must not be null");
		return defaultValue;
	}

	private PropertyValue createDefaultPropertyValue(PropertyTokenHolder tokens, Locale locale) {
		TypeDescriptor desc = getPropertyTypeDescriptor(tokens.canonicalName, locale);
		if (desc == null) {
			throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + tokens.canonicalName,
					"Could not determine property type for auto-growing a default value");
		}
		Object defaultValue = newValue(desc.getType(), desc, tokens.canonicalName);
		return new PropertyValue(tokens.canonicalName, defaultValue);
	}

	private Object newValue(Class<?> type, TypeDescriptor desc, String name) {
		try {
			if (type.isArray()) {
				return createArray(type);
			}
			else if (Collection.class.isAssignableFrom(type)) {
				TypeDescriptor elementDesc = (desc != null ? desc.getElementTypeDescriptor() : null);
				return CollectionFactory.createCollection(type, (elementDesc != null ? elementDesc.getType() : null), 16);
			}
			else if (Map.class.isAssignableFrom(type)) {
				TypeDescriptor keyDesc = (desc != null ? desc.getMapKeyTypeDescriptor() : null);
				return CollectionFactory.createMap(type, (keyDesc != null ? keyDesc.getType() : null), 16);
			}
			else {
				Constructor<?> ctor = type.getDeclaredConstructor();
				if (Modifier.isPrivate(ctor.getModifiers())) {
					throw new IllegalAccessException("Auto-growing not allowed with private constructor: " + ctor);
				}
				return BeanUtils.instantiateClass(ctor);
			}
		}
		catch (Throwable ex) {
			throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + name,
					"Could not instantiate property type [" + type.getName() + "] to auto-grow nested property path", ex);
		}
	}

	/**
	 * Create the array for the given array type.
	 * @param arrayType the desired type of the target array
	 * @return a new array instance
	 */
	private static Object createArray(Class<?> arrayType) {
		Assert.notNull(arrayType, "Array type must not be null");
		Class<?> componentType = arrayType.componentType();
		if (componentType.isArray()) {
			Object array = Array.newInstance(componentType, 1);
			Array.set(array, 0, createArray(componentType));
			return array;
		}
		else {
			return Array.newInstance(componentType, 0);
		}
	}

	/**
	 * Parse the given property name into the corresponding property name tokens.
	 * @param propertyName the property name to parse
	 * @return representation of the parsed property tokens
	 */
	private PropertyTokenHolder getPropertyNameTokens(String propertyName) {
		String actualName = null;
		List<String> keys = new ArrayList<>(2);
		int searchIndex = 0;
		while (searchIndex != -1) {
			int keyStart = propertyName.indexOf(PROPERTY_KEY_PREFIX, searchIndex);
			searchIndex = -1;
			if (keyStart != -1) {
				int keyEnd = getPropertyNameKeyEnd(propertyName, keyStart + PROPERTY_KEY_PREFIX.length());
				if (keyEnd != -1) {
					if (actualName == null) {
						actualName = propertyName.substring(0, keyStart);
					}
					String key = propertyName.substring(keyStart + PROPERTY_KEY_PREFIX.length(), keyEnd);
					if (key.length() > 1 && ((key.startsWith("'") && key.endsWith("'")) ||
							(key.startsWith("\"") && key.endsWith("\"")))) {
						key = key.substring(1, key.length() - 1);
					}
					keys.add(key);
					searchIndex = keyEnd + PROPERTY_KEY_SUFFIX.length();
				}
			}
		}
		PropertyTokenHolder tokens = new PropertyTokenHolder(actualName != null ? actualName : propertyName);
		if (!keys.isEmpty()) {
			tokens.canonicalName += PROPERTY_KEY_PREFIX +
					StringUtils.collectionToDelimitedString(keys, PROPERTY_KEY_SUFFIX + PROPERTY_KEY_PREFIX) +
					PROPERTY_KEY_SUFFIX;
			tokens.keys = StringUtils.toStringArray(keys);
		}
		return tokens;
	}

	private int getPropertyNameKeyEnd(String propertyName, int startIndex) {
		int unclosedPrefixes = 0;
		int length = propertyName.length();
		for (int i = startIndex; i < length; i++) {
			switch (propertyName.charAt(i)) {
				case PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR -> {
					// The property name contains opening prefix(es)...
					unclosedPrefixes++;
				}
				case PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR -> {
					if (unclosedPrefixes == 0) {
						// No unclosed prefix(es) in the property name (left) ->
						// this is the suffix we are looking for.
						return i;
					}
					else {
						// This suffix does not close the initial prefix but rather
						// just one that occurred within the property name.
						unclosedPrefixes--;
					}
				}
			}
		}
		return -1;
	}

	@Override
	public String toString() {
		String className = getClass().getName();
		if (this.wrappedObject == null) {
			return className + ": no wrapped object set";
		}
		return className + ": wrapping object [" + ObjectUtils.identityToString(this.wrappedObject) + ']';
	}


	/**
	 * A handler for a specific property.
	 */
	protected abstract static class PropertyHandler {

		private final Class<?> propertyType;

		private final boolean readable;

		private final boolean writable;

		public PropertyHandler(Class<?> propertyType, boolean readable, boolean writable) {
			this.propertyType = propertyType;
			this.readable = readable;
			this.writable = writable;
		}

		public Class<?> getPropertyType() {
			return this.propertyType;
		}

		public boolean isReadable() {
			return this.readable;
		}

		public boolean isWritable() {
			return this.writable;
		}

		public abstract TypeDescriptor toTypeDescriptor();

		public abstract ResolvableType getResolvableType();

		public TypeDescriptor getMapKeyType(int nestingLevel) {
			return TypeDescriptor.valueOf(getResolvableType().getNested(nestingLevel).asMap().resolveGeneric(0));
		}

		public TypeDescriptor getMapValueType(int nestingLevel) {
			return TypeDescriptor.valueOf(getResolvableType().getNested(nestingLevel).asMap().resolveGeneric(1));
		}

		public TypeDescriptor getCollectionType(int nestingLevel) {
			return TypeDescriptor.valueOf(getResolvableType().getNested(nestingLevel).asCollection().resolveGeneric());
		}

		public abstract TypeDescriptor nested(int level);

		public abstract Object getValue() throws Exception;

		public abstract void setValue(Object value) throws Exception;

	}


	/**
	 * Holder class used to store property tokens.
	 */
	protected static class PropertyTokenHolder {

		public PropertyTokenHolder(String name) {
			this.actualName = name;
			this.canonicalName = name;
		}

		public String actualName;

		public String canonicalName;

		public String [] keys;
	}

}
