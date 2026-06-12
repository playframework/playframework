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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import play.data.internal.binding.core.CollectionFactory;
import play.data.internal.binding.core.convert.ConversionFailedException;
import play.data.internal.binding.core.convert.ConversionService;
import play.data.internal.binding.core.convert.TypeDescriptor;
import play.data.internal.binding.util.ClassUtils;
import play.data.internal.binding.util.CollectionUtils;
import play.data.internal.binding.util.ReflectionUtils;
import play.data.internal.binding.util.StringUtils;

/**
 * Internal helper class for converting property values to target types.
 *
 * <p>Works on a given {@link AbstractPropertyAccessor} instance.
 * Used as a delegate by {@link BeanWrapperImpl}.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Dave Syer
 * @author Yanming Zhou
 * @see BeanWrapperImpl
 */
class TypeConverterDelegate {

	private static final Log logger = LogFactory.getLog(TypeConverterDelegate.class);

	private final AbstractPropertyAccessor typeConverterSupport;


	/**
	 * Create a new TypeConverterDelegate for the given converter support.
	 * @param typeConverterSupport the converter support to use
	 */
	public TypeConverterDelegate(AbstractPropertyAccessor typeConverterSupport) {
		this.typeConverterSupport = typeConverterSupport;
	}


	/**
	 * Convert the value to the required type for the specified property.
	 * @param propertyName name of the property
	 * @param newValue the proposed new value
	 * @param requiredType the type we must convert to
	 * (or {@code null} if not known, for example in case of a collection element)
	 * @return the new value, possibly the result of type conversion
	 * @throws IllegalArgumentException if type conversion failed
	 */
	public <T> T convertIfNecessary(String propertyName,
			Object newValue, Class<T> requiredType) throws IllegalArgumentException {

		return convertIfNecessary(propertyName, newValue, requiredType, TypeDescriptor.valueOf(requiredType));
	}

	/**
	 * Convert the value to the required type (if necessary from a String),
	 * for the specified property.
	 * @param propertyName name of the property
	 * @param newValue the proposed new value
	 * @param requiredType the type we must convert to
	 * (or {@code null} if not known, for example in case of a collection element)
	 * @param typeDescriptor the descriptor for the target property or field
	 * @return the new value, possibly the result of type conversion
	 * @throws IllegalArgumentException if type conversion failed
	 */
	@SuppressWarnings("unchecked")
	public <T> T convertIfNecessary(String propertyName, Object newValue,
			Class<T> requiredType, TypeDescriptor typeDescriptor) throws IllegalArgumentException {

		ConversionFailedException conversionAttemptEx = null;

		ConversionService conversionService = this.typeConverterSupport.getConversionService();
		if (conversionService != null && newValue != null && typeDescriptor != null) {
			TypeDescriptor sourceTypeDesc = TypeDescriptor.forObject(newValue);
			if (conversionService.canConvert(sourceTypeDesc, typeDescriptor)) {
				try {
					return (T) conversionService.convert(newValue, sourceTypeDesc, typeDescriptor);
				}
				catch (ConversionFailedException ex) {
					// fallback to default conversion logic below
					conversionAttemptEx = ex;
				}
			}
		}

		Object convertedValue = newValue;

		// Value not of required type?
			if (typeDescriptor != null && requiredType != null && Collection.class.isAssignableFrom(requiredType)) {
				TypeDescriptor elementTypeDesc = typeDescriptor.getElementTypeDescriptor();
				if (elementTypeDesc != null) {
					Class<?> elementType = elementTypeDesc.getType();
					if (convertedValue instanceof String text) {
						if (Enum.class.isAssignableFrom(elementType)) {
							convertedValue = StringUtils.commaDelimitedListToStringArray(text);
						}
						else {
							// The removed CustomCollectionEditor used to wrap a single submitted string as a one-element collection.
							convertedValue = Collections.singletonList(text);
						}
					}
				}
				else if (convertedValue instanceof String text) {
					// The removed CustomCollectionEditor used to wrap a single submitted string as a one-element collection.
					convertedValue = Collections.singletonList(text);
				}
			}

		boolean standardConversion = false;

		if (requiredType != null) {
			// Try to apply some standard type conversion rules if appropriate.

			if (convertedValue != null) {
				if (Object.class == requiredType) {
					return (T) convertedValue;
				}
				else if (requiredType.isArray()) {
					// Array required -> apply appropriate conversion of elements.
					if (convertedValue instanceof String text &&
							Enum.class.isAssignableFrom(requiredType.componentType())) {
						convertedValue = StringUtils.commaDelimitedListToStringArray(text);
					}
					return (T) convertToTypedArray(convertedValue, propertyName, requiredType.componentType());
				}
				else if (convertedValue.getClass().isArray()) {
					if (Collection.class.isAssignableFrom(requiredType)) {
						convertedValue = convertToTypedCollection(CollectionUtils.arrayToList(convertedValue),
								propertyName, requiredType, typeDescriptor);
						standardConversion = true;
					}
					else if (Array.getLength(convertedValue) == 1) {
						convertedValue = Array.get(convertedValue, 0);
						standardConversion = true;
					}
				}
				else if (convertedValue instanceof Collection<?> coll) {
					// Convert elements to target type, if determined.
					convertedValue = convertToTypedCollection(coll, propertyName, requiredType, typeDescriptor);
					standardConversion = true;
				}
				else if (convertedValue instanceof Map<?, ?> map) {
					// Convert keys and values to respective target type, if determined.
					convertedValue = convertToTypedMap(map, propertyName, requiredType, typeDescriptor);
					standardConversion = true;
				}
				if (String.class == requiredType && ClassUtils.isPrimitiveOrWrapper(convertedValue.getClass())) {
					// We can stringify any primitive value...
					return (T) convertedValue.toString();
				}
				else if (convertedValue instanceof String text && !requiredType.isInstance(convertedValue)) {
					String trimmedValue = text.trim();
					if (requiredType.isEnum() && trimmedValue.isEmpty()) {
						// It's an empty enum identifier: reset the enum value to null.
						return null;
					}
					convertedValue = attemptToConvertStringToEnum(requiredType, trimmedValue, convertedValue);
					standardConversion = true;
				}
			}
			else {
				// convertedValue == null
				if (requiredType == Optional.class) {
					convertedValue = Optional.empty();
				}
			}

			if (!ClassUtils.isAssignableValue(requiredType, convertedValue)) {
				if (conversionAttemptEx != null) {
					// Original exception from former ConversionService call above...
					throw conversionAttemptEx;
				}
				else if (conversionService != null && typeDescriptor != null) {
					TypeDescriptor sourceTypeDesc = TypeDescriptor.forObject(newValue);
					if (conversionService.canConvert(sourceTypeDesc, typeDescriptor)) {
						return (T) conversionService.convert(newValue, sourceTypeDesc, typeDescriptor);
					}
				}

				// Definitely doesn't match: throw IllegalArgumentException/IllegalStateException
				StringBuilder msg = new StringBuilder();
				msg.append("Cannot convert value of type '").append(ClassUtils.getDescriptiveType(newValue));
				msg.append("' to required type '").append(ClassUtils.getQualifiedName(requiredType)).append('\'');
				if (propertyName != null) {
					msg.append(" for property '").append(propertyName).append('\'');
				}
					msg.append(": no matching conversion strategy found");
					throw new IllegalStateException(msg.toString());
			}
		}

		if (conversionAttemptEx != null) {
			if (!standardConversion && requiredType != null && Object.class != requiredType) {
				throw conversionAttemptEx;
			}
			logger.debug("Original ConversionService attempt failed - ignored since " +
					"fallback conversion eventually succeeded", conversionAttemptEx);
		}

		return (T) convertedValue;
	}

	private Object attemptToConvertStringToEnum(Class<?> requiredType, String trimmedValue, Object currentConvertedValue) {
		Object convertedValue = currentConvertedValue;

		if (convertedValue == currentConvertedValue) {
			// Try field lookup as fallback: for Java enum or custom enum
			// with values defined as static fields. Resulting value still needs
			// to be checked, hence we don't return it right away.
			try {
				Field enumField = requiredType.getField(trimmedValue);
				ReflectionUtils.makeAccessible(enumField);
				convertedValue = enumField.get(null);
			}
			catch (Throwable ex) {
				if (logger.isTraceEnabled()) {
					logger.trace("Field [" + convertedValue + "] isn't an enum value", ex);
				}
			}
		}

		return convertedValue;
	}
	private Object convertToTypedArray(Object input, String propertyName, Class<?> componentType) {
		if (input instanceof Collection<?> coll) {
			// Convert Collection elements to array elements.
			Object result = Array.newInstance(componentType, coll.size());
			int i = 0;
			for (Iterator<?> it = coll.iterator(); it.hasNext(); i++) {
				Object value = convertIfNecessary(
						buildIndexedPropertyName(propertyName, i), it.next(), componentType);
				Array.set(result, i, value);
			}
			return result;
		}
		else if (input.getClass().isArray()) {
			// Convert array elements, if necessary.
			if (componentType.equals(input.getClass().componentType())) {
				return input;
			}
			int arrayLength = Array.getLength(input);
			Object result = Array.newInstance(componentType, arrayLength);
			for (int i = 0; i < arrayLength; i++) {
				Object value = convertIfNecessary(
						buildIndexedPropertyName(propertyName, i), Array.get(input, i), componentType);
				Array.set(result, i, value);
			}
			return result;
		}
		else {
			// A plain value: convert it to an array with a single component.
			Object result = Array.newInstance(componentType, 1);
			Object value = convertIfNecessary(
					buildIndexedPropertyName(propertyName, 0), input, componentType);
			Array.set(result, 0, value);
			return result;
		}
	}

	@SuppressWarnings("unchecked")
	private Collection<?> convertToTypedCollection(Collection<?> original, String propertyName,
			Class<?> requiredType, TypeDescriptor typeDescriptor) {

		if (!Collection.class.isAssignableFrom(requiredType)) {
			return original;
		}

		boolean approximable = CollectionFactory.isApproximableCollectionType(requiredType);
		if (!approximable && !canCreateCopy(requiredType)) {
			throw new IllegalStateException("Custom Collection type [" + requiredType.getName() +
					"] does not allow for creating a copy");
		}

		boolean originalAllowed = requiredType.isInstance(original);
		TypeDescriptor elementType = (typeDescriptor != null ? typeDescriptor.getElementTypeDescriptor() : null);
		if (elementType == null && originalAllowed) {
			return original;
		}

		Iterator<?> it;
		try {
			it = original.iterator();
		}
		catch (Throwable ex) {
			throw new IllegalStateException(
					"Cannot access Collection of type [" + original.getClass().getName() + "]", ex);
		}

		Collection<Object> convertedCopy;
		try {
			if (approximable && requiredType.isInstance(original)) {
				convertedCopy = CollectionFactory.createApproximateCollection(original, original.size());
			}
			else {
				convertedCopy = CollectionFactory.createCollection(requiredType, original.size());
			}
		}
		catch (Throwable ex) {
			throw new IllegalStateException(
					"Cannot create copy of Collection type [" + requiredType.getName() + "]", ex);
		}

		for (int i = 0; it.hasNext(); i++) {
			Object element = it.next();
			String indexedPropertyName = buildIndexedPropertyName(propertyName, i);
			Object convertedElement = convertIfNecessary(indexedPropertyName, element,
					(elementType != null ? elementType.getType() : null) , elementType);
			try {
				convertedCopy.add(convertedElement);
			}
			catch (Throwable ex) {
				throw new IllegalStateException(
						"Collection type [" + requiredType.getName() + "] does not accept converted elements", ex);
			}
			originalAllowed = originalAllowed && (element == convertedElement);
		}
		return (originalAllowed ? original : convertedCopy);
	}

	@SuppressWarnings("unchecked")
	private Map<?, ?> convertToTypedMap(Map<?, ?> original, String propertyName,
			Class<?> requiredType, TypeDescriptor typeDescriptor) {

		if (!Map.class.isAssignableFrom(requiredType)) {
			return original;
		}

		boolean approximable = CollectionFactory.isApproximableMapType(requiredType);
		if (!approximable && !canCreateCopy(requiredType)) {
			throw new IllegalStateException("Custom Map type [" + requiredType.getName() +
					"] does not allow for creating a copy");
		}

		boolean originalAllowed = requiredType.isInstance(original);
		TypeDescriptor keyType = (typeDescriptor != null ? typeDescriptor.getMapKeyTypeDescriptor() : null);
		TypeDescriptor valueType = (typeDescriptor != null ? typeDescriptor.getMapValueTypeDescriptor() : null);
		if (keyType == null && valueType == null && originalAllowed) {
			return original;
		}

		Iterator<?> it;
		try {
			it = original.entrySet().iterator();
		}
		catch (Throwable ex) {
			throw new IllegalStateException(
					"Cannot access Map of type [" + original.getClass().getName() + "]", ex);
		}

		Map<Object, Object> convertedCopy;
		try {
			if (approximable && requiredType.isInstance(original)) {
				convertedCopy = CollectionFactory.createApproximateMap(original, original.size());
			}
			else {
				convertedCopy = CollectionFactory.createMap(requiredType, original.size());
			}
		}
		catch (Throwable ex) {
			throw new IllegalStateException(
					"Cannot create copy of Map type [" + requiredType.getName() + "]", ex);
		}

		while (it.hasNext()) {
			Map.Entry<?, ?> entry = (Map.Entry<?, ?>) it.next();
			Object key = entry.getKey();
			Object value = entry.getValue();
			String keyedPropertyName = buildKeyedPropertyName(propertyName, key);
			Object convertedKey = convertIfNecessary(keyedPropertyName, key,
					(keyType != null ? keyType.getType() : null), keyType);
			Object convertedValue = convertIfNecessary(keyedPropertyName, value,
					(valueType!= null ? valueType.getType() : null), valueType);
			try {
				convertedCopy.put(convertedKey, convertedValue);
			}
			catch (Throwable ex) {
				throw new IllegalStateException(
						"Map type [" + requiredType.getName() + "] does not accept converted entries", ex);
			}
			originalAllowed = originalAllowed && (key == convertedKey) && (value == convertedValue);
		}
		return (originalAllowed ? original : convertedCopy);
	}

	private String buildIndexedPropertyName(String propertyName, int index) {
		return (propertyName != null ?
				propertyName + PropertyAccessor.PROPERTY_KEY_PREFIX + index + PropertyAccessor.PROPERTY_KEY_SUFFIX :
				null);
	}

	private String buildKeyedPropertyName(String propertyName, Object key) {
		return (propertyName != null ?
				propertyName + PropertyAccessor.PROPERTY_KEY_PREFIX + key + PropertyAccessor.PROPERTY_KEY_SUFFIX :
				null);
	}

	private boolean canCreateCopy(Class<?> requiredType) {
		return (!requiredType.isInterface() && !Modifier.isAbstract(requiredType.getModifiers()) &&
				Modifier.isPublic(requiredType.getModifiers()) && ClassUtils.hasConstructor(requiredType));
	}

}
