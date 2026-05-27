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

package play.data.internal.binding.validation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import play.data.internal.binding.util.StringUtils;

/**
 * Default message code resolver.
 *
 * <p>Will create two message codes for an object error, in the following order:
 * <ul>
 * <li>1.: code + "." + object name
 * <li>2.: code
 * </ul>
 *
 * <p>Will create four message codes for a field specification, in the following order:
 * <ul>
 * <li>1.: code + "." + object name + "." + field
 * <li>2.: code + "." + field
 * <li>3.: code + "." + field type
 * <li>4.: code
 * </ul>
 *
 * <p>For example, in case of code "typeMismatch", object name "user", field "age":
 * <ul>
 * <li>1. try "typeMismatch.user.age"
 * <li>2. try "typeMismatch.age"
 * <li>3. try "typeMismatch.int"
 * <li>4. try "typeMismatch"
 * </ul>
 *
 * <p>This resolution algorithm thus can be leveraged for example to show
 * specific messages for binding errors like "required" and "typeMismatch":
 * <ul>
 * <li>at the object + field level ("age" field, but only on "user");
 * <li>at the field level (all "age" fields, no matter which object name);
 * <li>or at the general level (all fields, on any object).
 * </ul>
 *
 * <p>In case of array, {@link List} or {@link java.util.Map} properties,
 * both codes for specific elements and for the whole collection are
 * generated. Assuming a field "name" of an array "groups" in object "user":
 * <ul>
 * <li>1. try "typeMismatch.user.groups[0].name"
 * <li>2. try "typeMismatch.user.groups.name"
 * <li>3. try "typeMismatch.groups[0].name"
 * <li>4. try "typeMismatch.groups.name"
 * <li>5. try "typeMismatch.name"
 * <li>6. try "typeMismatch.java.lang.String"
 * <li>7. try "typeMismatch"
 * </ul>
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Chris Beams
 */
@SuppressWarnings("serial")
public class DefaultMessageCodesResolver implements MessageCodesResolver, Serializable {

	/**
	 * The separator that this implementation uses when resolving message codes.
	 */
	public static final String CODE_SEPARATOR = ".";

	@Override
	public String[] resolveMessageCodes(String errorCode, String objectName) {
		return resolveMessageCodes(errorCode, objectName, "", null);
	}

	/**
	 * Build the code list for the given code and field: an
	 * object/field-specific code, a field-specific code, a plain error code.
	 * <p>Arrays, Lists and Maps are resolved both for specific elements and
	 * the whole collection.
	 * <p>See the {@link DefaultMessageCodesResolver class level javadoc} for
	 * details on the generated codes.
	 * @return the list of codes
	 */
	@Override
	public String[] resolveMessageCodes(String errorCode, String objectName, String field, Class<?> fieldType) {
		Set<String> codeList = new LinkedHashSet<>();
		List<String> fieldList = new ArrayList<>();
		buildFieldList(field, fieldList);
		addCodes(codeList, errorCode, objectName, fieldList);
		int dotIndex = field.lastIndexOf('.');
		if (dotIndex != -1) {
			buildFieldList(field.substring(dotIndex + 1), fieldList);
		}
		addCodes(codeList, errorCode, null, fieldList);
		if (fieldType != null) {
			addCode(codeList, errorCode, null, fieldType.getName());
		}
		addCode(codeList, errorCode, null, null);
		return StringUtils.toStringArray(codeList);
	}

	private void addCodes(Collection<String> codeList, String errorCode, String objectName, Iterable<String> fields) {
		for (String field : fields) {
			addCode(codeList, errorCode, objectName, field);
		}
	}

	private void addCode(Collection<String> codeList, String errorCode, String objectName, String field) {
		codeList.add(postProcessMessageCode(toDelimitedString(errorCode, objectName, field)));
	}

	/**
	 * Add both keyed and non-keyed entries for the supplied {@code field}
	 * to the supplied field list.
	 */
	protected void buildFieldList(String field, List<String> fieldList) {
		fieldList.add(field);
		String plainField = field;
		int keyIndex = plainField.lastIndexOf('[');
		while (keyIndex != -1) {
			int endKeyIndex = plainField.indexOf(']', keyIndex);
			if (endKeyIndex != -1) {
				plainField = plainField.substring(0, keyIndex) + plainField.substring(endKeyIndex + 1);
				fieldList.add(plainField);
				keyIndex = plainField.lastIndexOf('[');
			}
			else {
				keyIndex = -1;
			}
		}
	}

	protected String postProcessMessageCode(String code) {
		return code;
	}

		/**
		 * Concatenate the given elements, delimiting each with
		 * {@link DefaultMessageCodesResolver#CODE_SEPARATOR}, skipping zero-length or
		 * null elements altogether.
		 */
		public static String toDelimitedString(String... elements) {
			StringJoiner rtn = new StringJoiner(CODE_SEPARATOR);
			for (String element : elements) {
				if (StringUtils.hasLength(element)) {
					rtn.add(element);
				}
			}
			return rtn.toString();
		}

}
