/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import org.hibernate.validator.HibernateValidatorFactory;
import org.hibernate.validator.engine.HibernateConstraintViolation;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.NotReadablePropertyException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import play.data.format.Formatters;
import play.data.validation.Constraints;
import play.data.validation.Constraints.ValidationPayload;
import play.data.validation.ValidationError;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.i18n.MessagesImpl;
import play.libs.AnnotationUtils;
import play.libs.typedmap.TypedMap;
import play.mvc.Http;
import play.mvc.Http.HttpVerbs;

import javax.validation.ConstraintViolation;
import javax.validation.groups.Default;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.PropertyDescriptor;
import javax.validation.ValidatorFactory;
import javax.validation.Validator;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static play.libs.F.Tuple;

/**
 * Helper to manage HTML form description, submission and validation.
 */
public class Form<T> {

    /**
     * Statically compiled Pattern for replacing pairs of "<" and ">" with an optional content and optionally prefixed with a dot. Needed to get the field from a violation.
     * This takes care of occurrences like "field.<list element>", "field<K>[somekey]", "field[somekey].<map value>", "field<K>[somekey].<map key>", etc.
     * We always want to end up with just "field" or "field[0]" in case of lists or "field[somekey]" in case of maps.
     * Also see https://github.com/hibernate/hibernate-validator/blob/6.0.5.Final/engine/src/main/java/org/hibernate/validator/internal/engine/path/NodeImpl.java#L51-L56
     */
    private static final Pattern REPLACE_COLLECTION_ELEMENT = Pattern.compile("\\.?<[^<]*>");

    /** Statically compiled Pattern for replacing "typeMismatch" in Form errors. */
    private static final Pattern REPLACE_TYPEMISMATCH = Pattern.compile("typeMismatch", Pattern.LITERAL);

    /**
     * Defines a form element's display name.
     */
    @Retention(RUNTIME)
    @Target({ANNOTATION_TYPE})
    public @interface Display {
        String name();
        String[] attributes() default {};
    }

    // --

    private final String rootName;
    private final Class<T> backedType;
    private final Map<String,String> rawData;
    private final Map<String, Http.MultipartFormData.FilePart<?>> files;
    private final List<ValidationError> errors;
    private final Optional<T> value;
    private final Class<?>[] groups;
    private final Lang lang;
    private final boolean directFieldAccess;
    final MessagesApi messagesApi;
    final Formatters formatters;
    final ValidatorFactory validatorFactory;
    final Config config;

    public Class<T> getBackedType() {
        return backedType;
    }

    protected T blankInstance() {
        try {
            return backedType.getDeclaredConstructor().newInstance();
        } catch(Exception e) {
            throw new RuntimeException("Cannot instantiate " + backedType + ". It must have a default constructor", e);
        }
    }

    /**
     * Creates a new <code>Form</code>.  Consider using a {@link FormFactory} rather than this constructor.
     *
     * @param clazz wrapped class
     * @param messagesApi    messagesApi component.
     * @param formatters     formatters component.
     * @param validatorFactory      validatorFactory component.
     * @param config      config component.
     */
    public Form(Class<T> clazz, MessagesApi messagesApi, Formatters formatters, ValidatorFactory validatorFactory, Config config) {
        this(null, clazz, messagesApi, formatters, validatorFactory, config);
    }

    public Form(String rootName, Class<T> clazz, MessagesApi messagesApi, Formatters formatters, ValidatorFactory validatorFactory, Config config) {
        this(rootName, clazz, (Class<?>)null, messagesApi, formatters, validatorFactory, config);
    }

    public Form(String rootName, Class<T> clazz, Class<?> group, MessagesApi messagesApi, Formatters formatters, ValidatorFactory validatorFactory, Config config) {
        this(rootName, clazz, group != null ? new Class[]{group} : null, messagesApi, formatters, validatorFactory, config);
    }

    public Form(String rootName, Class<T> clazz, Class<?>[] groups, MessagesApi messagesApi, Formatters formatters, ValidatorFactory validatorFactory, Config config) {
        this(rootName, clazz, new HashMap<>(), new ArrayList<>(), Optional.empty(), groups, messagesApi, formatters, validatorFactory, config);
    }

    public Form(String rootName, Class<T> clazz, Map<String,String> data, List<ValidationError> errors, Optional<T> value, MessagesApi messagesApi, Formatters formatters, ValidatorFactory validatorFactory, Config config) {
        this(rootName, clazz, data, Collections.emptyMap(), errors, value, messagesApi, formatters, validatorFactory, config);
    }

    public Form(String rootName, Class<T> clazz, Map<String,String> data, Map<String, Http.MultipartFormData.FilePart<?>> files, List<ValidationError> errors, Optional<T> value, MessagesApi messagesApi, Formatters formatters, ValidatorFactory validatorFactory, Config config) {
        this(rootName, clazz, data, files, errors, value, (Class<?>)null, messagesApi, formatters, validatorFactory, config);
    }

    public Form(String rootName, Class<T> clazz, Map<String,String> data, List<ValidationError> errors, Optional<T> value, Class<?> group, MessagesApi messagesApi, Formatters formatters, ValidatorFactory validatorFactory, Config config) {
        this(rootName, clazz, data, Collections.emptyMap(), errors, value, group, messagesApi, formatters, validatorFactory, config);
    }

    public Form(String rootName, Class<T> clazz, Map<String,String> data, Map<String, Http.MultipartFormData.FilePart<?>> files, List<ValidationError> errors, Optional<T> value, Class<?> group, MessagesApi messagesApi, Formatters formatters, ValidatorFactory validatorFactory, Config config) {
        this(rootName, clazz, data, files, errors, value, group != null ? new Class[]{group} : null, messagesApi, formatters, validatorFactory, config);
    }

    /**
     * Creates a new <code>Form</code>.  Consider using a {@link FormFactory} rather than this constructor.
     *
     * @param rootName    the root name.
     * @param clazz wrapped class
     * @param data the current form data (used to display the form)
     * @param errors the collection of errors associated with this form
     * @param value optional concrete value of type <code>T</code> if the form submission was successful
     * @param groups    the array of classes with the groups.
     * @param messagesApi needed to look up various messages
     * @param formatters used for parsing and printing form fields
     * @param validatorFactory the validatorFactory component.
     * @param config the config component.
     */
    public Form(String rootName, Class<T> clazz, Map<String,String> data, List<ValidationError> errors, Optional<T> value, Class<?>[] groups, MessagesApi messagesApi, Formatters formatters, ValidatorFactory validatorFactory, Config config) {
        this(rootName, clazz, data, Collections.emptyMap(), errors, value, groups, messagesApi, formatters, validatorFactory, config);
    }

    /**
     * Creates a new <code>Form</code>.  Consider using a {@link FormFactory} rather than this constructor.
     *
     * @param rootName    the root name.
     * @param clazz wrapped class
     * @param data the current form data (used to display the form)
     * @param files the current form file data
     * @param errors the collection of errors associated with this form
     * @param value optional concrete value of type <code>T</code> if the form submission was successful
     * @param groups    the array of classes with the groups.
     * @param messagesApi needed to look up various messages
     * @param formatters used for parsing and printing form fields
     * @param validatorFactory the validatorFactory component.
     * @param config the config component.
     */
    public Form(String rootName, Class<T> clazz, Map<String,String> data, Map<String, Http.MultipartFormData.FilePart<?>> files, List<ValidationError> errors, Optional<T> value, Class<?>[] groups, MessagesApi messagesApi, Formatters formatters, ValidatorFactory validatorFactory, Config config) {
        this(rootName, clazz, data, files, errors, value, groups, messagesApi, formatters, validatorFactory, config, null);
    }

    /**
     * Creates a new <code>Form</code>.  Consider using a {@link FormFactory} rather than this constructor.
     *
     * @param rootName    the root name.
     * @param clazz wrapped class
     * @param data the current form data (used to display the form)
     * @param errors the collection of errors associated with this form
     * @param value optional concrete value of type <code>T</code> if the form submission was successful
     * @param groups    the array of classes with the groups.
     * @param messagesApi needed to look up various messages
     * @param formatters used for parsing and printing form fields
     * @param validatorFactory the validatorFactory component.
     * @param config the config component.
     * @param lang used for formatting when retrieving a field (via {@link #field(String)} or {@link #apply(String)}) and for translations in {@link #errorsAsJson()}
     */
    public Form(String rootName, Class<T> clazz, Map<String,String> data, List<ValidationError> errors, Optional<T> value, Class<?>[] groups, MessagesApi messagesApi, Formatters formatters, ValidatorFactory validatorFactory, Config config, Lang lang) {
        this(rootName, clazz, data, Collections.emptyMap(), errors, value, groups, messagesApi, formatters, validatorFactory, config, lang);
    }

    /**
     * Creates a new <code>Form</code>.  Consider using a {@link FormFactory} rather than this constructor.
     *
     * @param rootName    the root name.
     * @param clazz wrapped class
     * @param data the current form data (used to display the form)
     * @param files the current form file data
     * @param errors the collection of errors associated with this form
     * @param value optional concrete value of type <code>T</code> if the form submission was successful
     * @param groups    the array of classes with the groups.
     * @param messagesApi needed to look up various messages
     * @param formatters used for parsing and printing form fields
     * @param validatorFactory the validatorFactory component.
     * @param config the config component.
     * @param lang used for formatting when retrieving a field (via {@link #field(String)} or {@link #apply(String)}) and for translations in {@link #errorsAsJson()}
     */
    public Form(String rootName, Class<T> clazz, Map<String,String> data, Map<String, Http.MultipartFormData.FilePart<?>> files, List<ValidationError> errors, Optional<T> value, Class<?>[] groups, MessagesApi messagesApi, Formatters formatters, ValidatorFactory validatorFactory, Config config, Lang lang) {
        this(rootName, clazz, data, files, errors, value, groups, messagesApi, formatters, validatorFactory, config, lang, config != null && config.getBoolean("play.forms.binding.directFieldAccess"));
    }

    /**
     * Creates a new <code>Form</code>.  Consider using a {@link FormFactory} rather than this constructor.
     *
     * @param rootName    the root name.
     * @param clazz wrapped class
     * @param data the current form data (used to display the form)
     * @param errors the collection of errors associated with this form
     * @param value optional concrete value of type <code>T</code> if the form submission was successful
     * @param groups    the array of classes with the groups.
     * @param messagesApi needed to look up various messages
     * @param formatters used for parsing and printing form fields
     * @param validatorFactory the validatorFactory component.
     * @param config the config component.
     * @param lang used for formatting when retrieving a field (via {@link #field(String)} or {@link #apply(String)}) and for translations in {@link #errorsAsJson()}
     * @param directFieldAccess access fields of form directly during binding instead of using getters
     */
    public Form(String rootName, Class<T> clazz, Map<String,String> data, List<ValidationError> errors, Optional<T> value, Class<?>[] groups, MessagesApi messagesApi, Formatters formatters, ValidatorFactory validatorFactory, Config config, Lang lang, boolean directFieldAccess) {
        this(rootName, clazz, data, Collections.emptyMap(), errors, value, groups, messagesApi, formatters, validatorFactory, config, lang, directFieldAccess);
    }

    /**
     * Creates a new <code>Form</code>.  Consider using a {@link FormFactory} rather than this constructor.
     *
     * @param rootName    the root name.
     * @param clazz wrapped class
     * @param data the current form data (used to display the form)
     * @param files the current form file data
     * @param errors the collection of errors associated with this form
     * @param value optional concrete value of type <code>T</code> if the form submission was successful
     * @param groups    the array of classes with the groups.
     * @param messagesApi needed to look up various messages
     * @param formatters used for parsing and printing form fields
     * @param validatorFactory the validatorFactory component.
     * @param config the config component.
     * @param lang used for formatting when retrieving a field (via {@link #field(String)} or {@link #apply(String)}) and for translations in {@link #errorsAsJson()}
     * @param directFieldAccess access fields of form directly during binding instead of using getters
     */
    public Form(String rootName, Class<T> clazz, Map<String,String> data, Map<String, Http.MultipartFormData.FilePart<?>> files, List<ValidationError> errors, Optional<T> value, Class<?>[] groups, MessagesApi messagesApi, Formatters formatters, ValidatorFactory validatorFactory, Config config, Lang lang, boolean directFieldAccess) {
        this.rootName = rootName;
        this.backedType = clazz;
        this.rawData = data != null ? new HashMap<>(data) : new HashMap<>();
        this.files = files != null ? new HashMap<>(files) : new HashMap<>();
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
        this.value = value;
        this.groups = groups;
        this.messagesApi = messagesApi;
        this.formatters = formatters;
        this.validatorFactory = validatorFactory;
        this.config = config;
        this.lang = lang;
        this.directFieldAccess = directFieldAccess;
    }

    protected Map<String,String> requestData(Http.Request request) {

        Map<String,String[]> urlFormEncoded = new HashMap<>();
        if (request.body().asFormUrlEncoded() != null) {
            urlFormEncoded = request.body().asFormUrlEncoded();
        }

        Map<String,String[]> multipartFormData = new HashMap<>();
        if (request.body().asMultipartFormData() != null) {
            multipartFormData = request.body().asMultipartFormData().asFormUrlEncoded();
        }

        Map<String,String> jsonData = new HashMap<>();
        if (request.body().asJson() != null) {
            jsonData = play.libs.Scala.asJava(
                play.api.data.FormUtils.fromJson("",
                    play.api.libs.json.Json.parse(
                        play.libs.Json.stringify(request.body().asJson())
                    )
                )
            );
        }

        Map<String,String> data = new HashMap<>();

        fillDataWith(data, urlFormEncoded);
        fillDataWith(data, multipartFormData);

        jsonData.forEach(data::put);

        if(!request.method().equalsIgnoreCase(HttpVerbs.POST) && !request.method().equalsIgnoreCase(HttpVerbs.PUT) && !request.method().equalsIgnoreCase(HttpVerbs.PATCH)) {
            fillDataWith(data, request.queryString());
        }

        return data;
    }

    protected void fillDataWith(Map<String, String> data, Map<String, String[]> urlFormEncoded) {
        urlFormEncoded.forEach((key, values) -> {
            if (key.endsWith("[]")) {
                String k = key.substring(0, key.length() - 2);
                for (int i = 0; i < values.length; i++) {
                    data.put(k + "[" + i + "]", values[i]);
                }
            } else if (values.length > 0) {
                data.put(key, values[0]);
            }
        });
    }

    protected Map<String,Http.MultipartFormData.FilePart<?>> requestFileData(Http.Request request) {
        final Http.MultipartFormData multipartFormData = request.body().asMultipartFormData();
        if (multipartFormData != null) {
            return resolveDuplicateFilePartKeys(multipartFormData.getFiles());
        }
        return new HashMap<>();
    }

    protected <A> Map<String, Http.MultipartFormData.FilePart<?>> resolveDuplicateFilePartKeys(final List<Http.MultipartFormData.FilePart<A>> fileParts) {
        final Map<String, List<Http.MultipartFormData.FilePart<?>>> resolvedDuplicateKeys = fileParts.stream()
                .collect(Collectors.toMap(filePart -> filePart.getKey(), filePart -> new ArrayList<>(Arrays.asList(filePart)), (a, b) -> { a.addAll(b); return a; }));
        final Map<String, Http.MultipartFormData.FilePart<?>> data = new HashMap<>();
        resolvedDuplicateKeys.forEach((key, values) -> {
            if (key.endsWith("[]")) {
                String k = key.substring(0, key.length() - 2);
                for (int i = 0; i < values.size(); i++) {
                    data.put(k + "[" + i + "]", values.get(i));
                }
            } else if (!values.isEmpty()) {
                data.put(key, values.get(0));
            }
        });
        return data;
    }

    /**
     * @deprecated Deprecated as of 2.7.0.
     */
    @Deprecated
    protected Lang ctxLang() {
        return Http.Context.safeCurrent().map(ctx -> ctx.messages().lang()).orElse(null);
    }

    /**
     * @deprecated Deprecated as of 2.7.0.
     */
    @Deprecated
    protected TypedMap ctxRequestAttrs() {
        return Http.Context.safeCurrent().map(ctx -> ctx.request().attrs()).orElseGet(() -> TypedMap.empty());
    }

    /**
     * Binds request data to this form - that is, handles form submission.
     *
     * @param allowedFields    the fields that should be bound to the form, all fields if not specified.
     * @return a copy of this form filled with the new data
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link #bindFromRequest(Http.Request, String...)} instead.
     */
    @Deprecated
    public Form<T> bindFromRequest(String... allowedFields) {
        return bind(play.mvc.Controller.ctx().messages().lang(), play.mvc.Controller.request().attrs(), requestData(play.mvc.Controller.request()), requestFileData(play.mvc.Controller.request()), allowedFields);
    }

    /**
     * Binds request data to this form - that is, handles form submission.
     *
     * @param request          the request to bind data from.
     * @param allowedFields    the fields that should be bound to the form, all fields if not specified.
     * @return a copy of this form filled with the new data
     */
    public Form<T> bindFromRequest(Http.Request request, String... allowedFields) {
        return bind(this.messagesApi.preferred(request).lang(), request.attrs(), requestData(request), requestFileData(request), allowedFields);
    }

    /**
     * Binds request data to this form - that is, handles form submission.
     *
     * @param requestData      the map of data to bind from
     * @param allowedFields    the fields that should be bound to the form, all fields if not specified.
     * @return a copy of this form filled with the new data
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link #bindFromRequestData(Lang, TypedMap, Map, String...)} instead.
     */
    @Deprecated
    public Form<T> bindFromRequest(Map<String,String[]> requestData, String... allowedFields) {
        return bindFromRequestData(ctxLang(), ctxRequestAttrs(), requestData, allowedFields);
    }

    /**
     * Binds request data to this form - that is, handles form submission.
     *
     * @param lang used for validators and formatters during binding and is part of {@link ValidationPayload}.
     *             Later also used for formatting when retrieving a field (via {@link #field(String)} or {@link #apply(String)})
     *             and for translations in {@link #errorsAsJson()}. For these methods the lang can be change via {@link #withLang(Lang)}.
     * @param attrs will be passed to validators via {@link ValidationPayload}
     * @param requestData      the map of data to bind from
     * @param allowedFields    the fields that should be bound to the form, all fields if not specified.
     * @return a copy of this form filled with the new data
     */
    public Form<T> bindFromRequestData(Lang lang, TypedMap attrs, Map<String,String[]> requestData, String... allowedFields) {
        return bindFromRequestData(lang, attrs, requestData, Collections.emptyMap(), allowedFields);
    }

    /**
     * Binds request data to this form - that is, handles form submission.
     *
     * @param lang used for validators and formatters during binding and is part of {@link ValidationPayload}.
     *             Later also used for formatting when retrieving a field (via {@link #field(String)} or {@link #apply(String)})
     *             and for translations in {@link #errorsAsJson()}. For these methods the lang can be change via {@link #withLang(Lang)}.
     * @param attrs will be passed to validators via {@link ValidationPayload}
     * @param requestData      the map of data to bind from
     * @param requestFileData  the map of file data to bind from
     * @param allowedFields    the fields that should be bound to the form, all fields if not specified.
     * @return a copy of this form filled with the new data
     */
    public Form<T> bindFromRequestData(Lang lang, TypedMap attrs, Map<String,String[]> requestData, Map<String, Http.MultipartFormData.FilePart<?>> requestFileData, String... allowedFields) {
        Map<String,String> data = new HashMap<>();
        fillDataWith(data, requestData);
        return bind(lang, attrs, data, requestFileData, allowedFields);
    }

    /**
     * Binds Json data to this form - that is, handles form submission.
     *
     * @param data data to submit
     * @param allowedFields    the fields that should be bound to the form, all fields if not specified.
     * @return a copy of this form filled with the new data
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link #bind(Lang, TypedMap, JsonNode, String...)} instead.
     */
    @Deprecated
    public Form<T> bind(JsonNode data, String... allowedFields) {
        return bind(ctxLang(), ctxRequestAttrs(), data, allowedFields);
    }

    /**
     * Binds Json data to this form - that is, handles form submission.
     *
     * @param lang used for validators and formatters during binding and is part of {@link ValidationPayload}.
     *             Later also used for formatting when retrieving a field (via {@link #field(String)} or {@link #apply(String)})
     *             and for translations in {@link #errorsAsJson()}. For these methods the lang can be change via {@link #withLang(Lang)}.
     * @param attrs will be passed to validators via {@link ValidationPayload}
     * @param data data to submit
     * @param allowedFields    the fields that should be bound to the form, all fields if not specified.
     * @return a copy of this form filled with the new data
     */
    public Form<T> bind(Lang lang, TypedMap attrs, JsonNode data, String... allowedFields) {
        return bind(lang, attrs,
            play.libs.Scala.asJava(
                play.api.data.FormUtils.fromJson("",
                    play.api.libs.json.Json.parse(
                        play.libs.Json.stringify(data)
                    )
                )
            ),
            allowedFields
        );
    }

    private static final Set<String> internalAnnotationAttributes = new HashSet<>(3);
    static {
        internalAnnotationAttributes.add("message");
        internalAnnotationAttributes.add("groups");
        internalAnnotationAttributes.add("payload");
    }

    protected Object[] getArgumentsForConstraint(String objectName, String field, ConstraintViolation<Object> violation) {
        Annotation annotation = violation.getConstraintDescriptor().getAnnotation();
        if (annotation instanceof Constraints.ValidateWith) {
            Constraints.ValidateWith validateWithAnnotation = (Constraints.ValidateWith)annotation;
            if (violation.getMessage().equals(Constraints.ValidateWithValidator.defaultMessage)) {
                Constraints.ValidateWithValidator validateWithValidator = new Constraints.ValidateWithValidator();
                validateWithValidator.initialize(validateWithAnnotation);
                Tuple<String, Object[]> errorMessageKey = validateWithValidator.getErrorMessageKey();
                if (errorMessageKey != null && errorMessageKey._2 != null) {
                    return errorMessageKey._2;
                } else {
                    return new Object[0];
                }
            } else {
                return new Object[0];
            }
        }
        List<Object> arguments = new LinkedList<>();
        String[] codes = new String[] {objectName + Errors.NESTED_PATH_SEPARATOR + field, field};
        arguments.add(new DefaultMessageSourceResolvable(codes, field));
        // Using a TreeMap for alphabetical ordering of attribute names
        Map<String, Object> attributesToExpose = new TreeMap<>();
        violation.getConstraintDescriptor().getAttributes().forEach((attributeName, attributeValue) -> {
            if (!internalAnnotationAttributes.contains(attributeName)) {
                attributesToExpose.put(attributeName, attributeValue);
            }
        });
        arguments.addAll(attributesToExpose.values());
        return arguments.toArray(new Object[arguments.size()]);
    }

    /**
     * When dealing with @ValidateWith or @ValidatePayloadWith annotations, and message parameter is not used in
     * the annotation, extract the message from validator's getErrorMessageKey() method
     *
     * @param violation the constraint violation.
     * @return the message associated with the constraint violation.
     */
    protected String getMessageForConstraintViolation(ConstraintViolation<Object> violation) {
        String errorMessage = violation.getMessage();
        Annotation annotation = violation.getConstraintDescriptor().getAnnotation();
        if (annotation instanceof Constraints.ValidateWith) {
            Constraints.ValidateWith validateWithAnnotation = (Constraints.ValidateWith)annotation;
            if (violation.getMessage().equals(Constraints.ValidateWithValidator.defaultMessage)) {
                Constraints.ValidateWithValidator validateWithValidator = new Constraints.ValidateWithValidator();
                validateWithValidator.initialize(validateWithAnnotation);
                Tuple<String, Object[]> errorMessageKey = validateWithValidator.getErrorMessageKey();
                if (errorMessageKey != null && errorMessageKey._1 != null) {
                    errorMessage = errorMessageKey._1;
                }
            }
        }
        if (annotation instanceof Constraints.ValidatePayloadWith) {
            Constraints.ValidatePayloadWith validatePayloadWithAnnotation = (Constraints.ValidatePayloadWith)annotation;
            if (violation.getMessage().equals(Constraints.ValidatePayloadWithValidator.defaultMessage)) {
                Constraints.ValidatePayloadWithValidator validatePayloadWithValidator = new Constraints.ValidatePayloadWithValidator();
                validatePayloadWithValidator.initialize(validatePayloadWithAnnotation);
                Tuple<String, Object[]> errorMessageKey = validatePayloadWithValidator.getErrorMessageKey();
                if (errorMessageKey != null && errorMessageKey._1 != null) {
                    errorMessage = errorMessageKey._1;
                }
            }
        }

        return errorMessage;
    }

    private DataBinder dataBinder(String... allowedFields) {
        DataBinder dataBinder;
        if (rootName == null) {
            dataBinder = new DataBinder(blankInstance());
        } else {
            dataBinder = new DataBinder(blankInstance(), rootName);
        }
        if (allowedFields.length > 0) {
            dataBinder.setAllowedFields(allowedFields);
        }
        SpringValidatorAdapter validator = new SpringValidatorAdapter(this.validatorFactory.getValidator());
        dataBinder.setValidator(validator);
        dataBinder.setConversionService(formatters.conversion);
        dataBinder.setAutoGrowNestedPaths(true);
        if(this.directFieldAccess) {
            // FYI: initBeanPropertyAccess() is the default, let's switch to direct field access instead
            dataBinder.initDirectFieldAccess(); // this should happen last, when everything else was set on the dataBinder already
        }
        return dataBinder;
    }

    private Map<String, Object> getObjectData(Map<String, String> data, Map<String, Http.MultipartFormData.FilePart<?>> files) {
        final Map<String, Object> dataAndFilesMerged = new HashMap<>(data);
        dataAndFilesMerged.putAll(files);
        if (rootName != null) {
            final Map<String, Object> objectData = new HashMap<>();
            dataAndFilesMerged.forEach((key, value) -> {
                if (key.startsWith(rootName + ".")) {
                    objectData.put(key.substring(rootName.length() + 1), value);
                }
            });
            return objectData;
        }
        return dataAndFilesMerged;
    }

    private Set<ConstraintViolation<Object>> runValidation(Lang lang, TypedMap attrs, DataBinder dataBinder, Map<String, Object> objectData) {
        return withRequestLocale(lang, () -> {
            dataBinder.bind(new MutablePropertyValues(objectData));
            final ValidationPayload payload = new ValidationPayload(lang, lang != null ? new MessagesImpl(lang, this.messagesApi) : null, Http.Context.safeCurrent().map(ctx -> ctx.args).orElse(null), attrs, this.config);
            final Validator validator = validatorFactory.unwrap(HibernateValidatorFactory.class).usingContext().constraintValidatorPayload(payload).getValidator();
            if (groups != null) {
                return validator.validate(dataBinder.getTarget(), groups);
            } else {
                return validator.validate(dataBinder.getTarget());
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void addConstraintViolationToBindingResult(ConstraintViolation<Object> violation, BindingResult result) {
        String field = REPLACE_COLLECTION_ELEMENT.matcher(violation.getPropertyPath().toString()).replaceAll("");
        FieldError fieldError = result.getFieldError(field);
        if (fieldError == null || !fieldError.isBindingFailure()) {
            try {
                final Object dynamicPayload = violation.unwrap(HibernateConstraintViolation.class).getDynamicPayload(Object.class);

                if (dynamicPayload instanceof String) {
                    result.rejectValue(
                        "", // global error
                        violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
                        new Object[0], // no msg arguments to pass
                        (String)dynamicPayload // dynamicPayload itself is the error message(-key)
                    );
                } else if (dynamicPayload instanceof ValidationError) {
                    final ValidationError error = (ValidationError) dynamicPayload;
                    result.rejectValue(
                        error.key(),
                        violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
                        error.arguments() != null ? error.arguments().toArray() : new Object[0],
                        error.message()
                    );
                } else if (dynamicPayload instanceof List) {
                    ((List<ValidationError>) dynamicPayload).forEach(error ->
                        result.rejectValue(
                            error.key(),
                            violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
                            error.arguments() != null ? error.arguments().toArray() : new Object[0],
                            error.message()
                        )
                    );
                } else {
                    result.rejectValue(
                        field,
                        violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
                        getArgumentsForConstraint(result.getObjectName(), field, violation),
                        getMessageForConstraintViolation(violation)
                    );
                }
            } catch (NotReadablePropertyException ex) {
                throw new IllegalStateException("JSR-303 validated property '" + field +
                        "' does not have a corresponding accessor for data binding - " +
                        "check your DataBinder's configuration (bean property versus direct field access)", ex);
            }
        }
    }

    private List<ValidationError> getFieldErrorsAsValidationErrors(Lang lang, BindingResult result) {
        return result.getFieldErrors().stream().map(error -> {
            String key = error.getObjectName() + "." + error.getField();
            if (key.startsWith("target.") && rootName == null) {
                key = key.substring(7);
            }

            if (error.isBindingFailure()) {
                ImmutableList.Builder<String> builder = ImmutableList.builder();
                final Messages msgs = lang != null ? new MessagesImpl(lang, this.messagesApi) : null;
                for (String code: error.getCodes()) {
                    code = REPLACE_TYPEMISMATCH.matcher(code).replaceAll(Matcher.quoteReplacement("error.invalid"));
                    if (msgs == null || msgs.isDefinedAt(code)) {
                        builder.add(code);
                    }
                }
                return new ValidationError(key, builder.build().reverse(),
                        convertErrorArguments(error.getArguments()));
            } else {
                return new ValidationError(key, error.getDefaultMessage(),
                        convertErrorArguments(error.getArguments()));
            }
        }).collect(Collectors.toList());
    }

    private List<ValidationError> globalErrorsAsValidationErrors(BindingResult result) {
        return result.getGlobalErrors()
                .stream()
                .map(error ->
                    new ValidationError(
                        "",
                        error.getDefaultMessage(),
                        convertErrorArguments(error.getArguments())
                    )
                ).collect(Collectors.toList());
    }

    /**
     * Binds data to this form - that is, handles form submission.
     *
     * @param data data to submit
     * @param allowedFields    the fields that should be bound to the form, all fields if not specified.
     * @return a copy of this form filled with the new data
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link #bind(Lang, TypedMap, Map, String...)} instead.
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public Form<T> bind(Map<String,String> data, String... allowedFields) {
        return bind(ctxLang(), ctxRequestAttrs(), data, allowedFields);
    }

    /**
     * Binds data to this form - that is, handles form submission.
     *
     * @param lang used for validators and formatters during binding and is part of {@link ValidationPayload}.
     *             Later also used for formatting when retrieving a field (via {@link #field(String)} or {@link #apply(String)})
     *             and for translations in {@link #errorsAsJson()}. For these methods the lang can be change via {@link #withLang(Lang)}.
     * @param attrs will be passed to validators via {@link ValidationPayload}
     * @param data data to submit
     * @param allowedFields    the fields that should be bound to the form, all fields if not specified.
     * @return a copy of this form filled with the new data
     */
    @SuppressWarnings("unchecked")
    public Form<T> bind(Lang lang, TypedMap attrs, Map<String,String> data, String... allowedFields) {
        return bind(lang, attrs, data, Collections.emptyMap(), allowedFields);
    }

    /**
     * Binds data to this form - that is, handles form submission.
     *
     * @param lang used for validators and formatters during binding and is part of {@link ValidationPayload}.
     *             Later also used for formatting when retrieving a field (via {@link #field(String)} or {@link #apply(String)})
     *             and for translations in {@link #errorsAsJson()}. For these methods the lang can be change via {@link #withLang(Lang)}.
     * @param attrs will be passed to validators via {@link ValidationPayload}
     * @param data data to submit
     * @param allowedFields    the fields that should be bound to the form, all fields if not specified.
     * @return a copy of this form filled with the new data
     */
    @SuppressWarnings("unchecked")
    public Form<T> bind(Lang lang, TypedMap attrs, Map<String,String> data, Map<String, Http.MultipartFormData.FilePart<?>> files, String... allowedFields) {

        final DataBinder dataBinder = dataBinder(allowedFields);
        final Map<String, Object> objectDataFinal = getObjectData(data, files);

        final Set<ConstraintViolation<Object>> validationErrors = runValidation(lang, attrs, dataBinder, objectDataFinal);
        final BindingResult result = dataBinder.getBindingResult();

        validationErrors.forEach(violation -> addConstraintViolationToBindingResult(violation, result));

        boolean hasAnyError = result.hasErrors() || result.getGlobalErrorCount() > 0;

        if (hasAnyError) {
            final List<ValidationError> errors = getFieldErrorsAsValidationErrors(lang, result);
            final List<ValidationError> globalErrors = globalErrorsAsValidationErrors(result);

            errors.addAll(globalErrors);

            return new Form<>(rootName, backedType, data, files, errors, Optional.ofNullable((T)result.getTarget()), groups, messagesApi, formatters, this.validatorFactory, config, lang, directFieldAccess);
        }
        return new Form<>(rootName, backedType, data, files, errors, Optional.ofNullable((T)result.getTarget()), groups, messagesApi, formatters, this.validatorFactory, config, lang, directFieldAccess);
    }

    /**
     * Convert the error arguments.
     *
     * @param arguments The arguments to convert.
     * @return The converted arguments.
     */
    private List<Object> convertErrorArguments(Object[] arguments) {
        if(arguments == null) {
            return Collections.emptyList();
        }
        List<Object> converted = Arrays.stream(arguments)
                .filter(arg -> !(arg instanceof org.springframework.context.support.DefaultMessageSourceResolvable))
                .collect(Collectors.toList());
        return Collections.unmodifiableList(converted);
    }

    /**
     * @return the actual form data as unmodifiable map. Does not contain file data, use {@link #files()} to access files.
     */
    public Map<String,String> rawData() {
        return Collections.unmodifiableMap(rawData);
    }

    /**
     * @return the the files as unmodifiable map. Use {@link #rawData()} to access other form data.
     */
    public Map<String,Http.MultipartFormData.FilePart<?>> files() {
        return Collections.unmodifiableMap(files);
    }

    public String name() {
        return rootName;
    }

    /**
     * @return the actual form value - even when the form contains validation errors.
     */
    public Optional<T> value() {
        return value;
    }

    /**
     * Populates this form with an existing value, used for edit forms.
     *
     * @param value existing value of type <code>T</code> used to fill this form
     * @return a copy of this form filled with the new data
     */
    public Form<T> fill(T value) {
        if (value == null) {
            throw new RuntimeException("Cannot fill a form with a null value");
        }
        return new Form<>(
                rootName,
                backedType,
                new HashMap<>(),
                new HashMap<>(),
                new ArrayList<>(),
                Optional.ofNullable(value),
                groups,
                messagesApi,
                formatters,
                validatorFactory,
                config,
                lang,
                directFieldAccess
        );
    }

    /**
     * @return <code>true</code> if there are any errors related to this form.
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * @return <code>true</code> if there any global errors related to this form.
     */
    public boolean hasGlobalErrors() {
        return !globalErrors().isEmpty();
    }

    /**
     * Retrieve all global errors - errors without a key.
     *
     * @return All global errors.
     */
    public List<ValidationError> globalErrors() {
        return Collections.unmodifiableList(errors.stream().filter(error -> error.key().isEmpty()).collect(Collectors.toList()));
    }

    /**
     * Retrieves the first global error (an error without any key), if it exists.
     *
     * @return An error.
     *
     * @deprecated Deprecated as of 2.7.0. Method has been renamed to {@link #globalError()}.
     */
    @Deprecated
    public Optional<ValidationError> getGlobalError() {
        return globalError();
    }

    /**
     * Retrieves the first global error (an error without any key), if it exists.
     *
     * @return An error.
     */
    public Optional<ValidationError> globalError() {
        return globalErrors().stream().findFirst();
    }

    /**
     * Returns all errors.
     *
     * @return All errors associated with this form.
     *
     * @deprecated Deprecated as of 2.7.0. Method has been renamed to {@link #errors()}.
     */
    @Deprecated
    public List<ValidationError> allErrors() {
        return errors();
    }

    /**
     * Returns all errors.
     *
     * @return All errors associated with this form.
     */
    public List<ValidationError> errors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * @param key    the field name associated with the error.
     * @return All errors for this key.
     */
    public List<ValidationError> errors(String key) {
        if(key == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(errors.stream().filter(error -> error.key().equals(key)).collect(Collectors.toList()));
    }

    /**
     * @param key    the field name associated with the error.
     * @return an error by key
     *
     * @deprecated Deprecated as of 2.7.0. Method has been renamed to {@link #error(String)}.
     */
    @Deprecated
    public Optional<ValidationError> getError(String key) {
        return error(key);
    }

    /**
     * @param key    the field name associated with the error.
     * @return an error by key
     */
    public Optional<ValidationError> error(String key) {
        return errors(key).stream().findFirst();
    }

    /**
     * @return the form errors serialized as Json.
     */
    public JsonNode errorsAsJson() {
        return errorsAsJson(this.lang);
    }

    /**
     * Returns the form errors serialized as Json using the given Lang.
     * @param lang    the language to use.
     * @return the JSON node containing the errors.
     */
    public JsonNode errorsAsJson(Lang lang) {
        Map<String, List<String>> allMessages = new HashMap<>();
        errors.forEach(error -> {
            if (error != null) {
                final List<String> messages = new ArrayList<>();
                if (messagesApi != null && lang != null) {
                    final List<String> reversedMessages = new ArrayList<>(error.messages());
                    Collections.reverse(reversedMessages);
                    messages.add(messagesApi.get(lang, reversedMessages, translateMsgArg(error.arguments(), messagesApi, lang)));
                } else {
                    messages.add(error.message());
                }
                allMessages.put(error.key(), messages);
            }
        });
        return play.libs.Json.toJson(allMessages);
    }

    private Object translateMsgArg(List<Object> arguments, MessagesApi messagesApi, Lang lang) {
        if (arguments != null) {
            return arguments.stream().map(arg -> {
                    if (arg instanceof String) {
                        return messagesApi != null ? messagesApi.get(lang, (String)arg) : (String)arg;
                    }
                    if (arg instanceof List) {
                        return ((List<?>) arg).stream().map(key -> messagesApi != null ? messagesApi.get(lang, (String)key) : (String)key).collect(Collectors.toList());
                    }
                    return arg;
                }).collect(Collectors.toList());
        } else {
            return null;
       }
    }

    /**
     * Gets the concrete value only if the submission was a success.
     * If the form is invalid because of validation errors this method will throw an exception.
     * If you want to retrieve the value even when the form is invalid use {@link #value()} instead.
     *
     * @throws IllegalStateException if there are errors binding the form, including the errors as JSON in the message
     * @return the concrete value.
     */
    public T get() {
        return this.get(this.lang);
    }

    /**
     * Gets the concrete value only if the submission was a success.
     * If the form is invalid because of validation errors this method will throw an exception.
     * If you want to retrieve the value even when the form is invalid use {@link #value()} instead.
     *
     * @param lang if an IllegalStateException gets thrown it's used to translate the form errors within that exception
     * @throws IllegalStateException if there are errors binding the form, including the errors as JSON in the message
     * @return the concrete value.
     */
    public T get(Lang lang) {
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Error(s) binding form: " + errorsAsJson(lang));
        }
        return value.get();
    }

    /**
     * @param error the <code>ValidationError</code> to add to the returned form.
     *
     * @return a copy of this form with the given error added.
     */
    public Form<T> withError(final ValidationError error) {
        if (error == null) {
            throw new NullPointerException("Can't reject null-values");
        }
        final List<ValidationError> copiedErrors = new ArrayList<>(this.errors);
        copiedErrors.add(error);
        return new Form<T>(this.rootName, this.backedType, this.rawData, this.files, copiedErrors, this.value, this.groups, this.messagesApi, this.formatters, this.validatorFactory, this.config, this.lang, this.directFieldAccess);
    }

    /**
     * @param key the error key
     * @param error the error message
     * @param args the error arguments
     *
     * @return a copy of this form with the given error added.
     */
    public Form<T> withError(final String key, final String error, final List<Object> args) {
        return withError(new ValidationError(key, error, args != null ? new ArrayList<>(args) : new ArrayList<>()));
    }

    /**
     * @param key the error key
     * @param error the error message
     *
     * @return a copy of this form with the given error added.
     */
    public Form<T> withError(final String key, final String error) {
        return withError(key, error, new ArrayList<>());
    }

    /**
     * @param error the global error message
     * @param args the global error arguments
     *
     * @return a copy of this form with the given global error added.
     */
    public Form<T> withGlobalError(final String error, final List<Object> args) {
        return withError("", error, args);
    }

    /**
     * @param error the global error message
     *
     * @return a copy of this form with the given global error added.
     */
    public Form<T> withGlobalError(final String error) {
        return withGlobalError(error, new ArrayList<>());
    }

    /**
     * @return a copy of this form but with the errors discarded.
     */
    public Form<T> discardingErrors() {
        return new Form<T>(this.rootName, this.backedType, this.rawData, this.files, new ArrayList<>(), this.value, this.groups, this.messagesApi, this.formatters, this.validatorFactory, this.config, this.lang, this.directFieldAccess);
    }

    /**
     * Retrieves a field.
     *
     * @param key field name
     * @return the field (even if the field does not exist you get a field)
     */
    public Field apply(String key) {
        return apply(key, this.lang);
    }

    /**
     * Retrieves a field.
     *
     * @param key field name
     * @param lang the language to use for the formatter
     * @return the field (even if the field does not exist you get a field)
     */
    public Field apply(String key, Lang lang) {
        return field(key, lang);
    }

    /**
     * Retrieves a field.
     *
     * @param key field name
     * @return the field (even if the field does not exist you get a field)
     */
    public Field field(final String key) {
        return field(key, this.lang);
    }

    /**
     * Retrieves a field.
     *
     * @param key field name
     * @param lang used for formatting
     * @return the field (even if the field does not exist you get a field)
     */
    public Field field(final String key, final Lang lang) {

        // Value
        String fieldValue = null;
        Http.MultipartFormData.FilePart file = null;
        if (rawData.containsKey(key)) {
            fieldValue = rawData.get(key);
        } else if (files.containsKey(key)) {
            file = files.get(key);
        } else {
            if (value.isPresent()) {
                BeanWrapper beanWrapper = new BeanWrapperImpl(value.get());
                beanWrapper.setAutoGrowNestedPaths(true);
                String objectKey = key;
                if (rootName != null && key.startsWith(rootName + ".")) {
                    objectKey = key.substring(rootName.length() + 1);
                }
                if (beanWrapper.isReadableProperty(objectKey)) {
                    Object oValue = beanWrapper.getPropertyValue(objectKey);
                    if (oValue != null) {
                        if(oValue instanceof Http.MultipartFormData.FilePart<?>) {
                            file = (Http.MultipartFormData.FilePart<?>)oValue;
                        } else {
                            if (formatters != null) {
                                final String objectKeyFinal = objectKey;
                                fieldValue = withRequestLocale(lang, () -> formatters.print(beanWrapper.getPropertyTypeDescriptor(objectKeyFinal), oValue));
                            } else {
                                fieldValue = oValue.toString();
                            }
                        }
                    }
                }
            }
        }

        // Format
        Tuple<String,List<Object>> format = null;
        BeanWrapper beanWrapper = new BeanWrapperImpl(blankInstance());
        beanWrapper.setAutoGrowNestedPaths(true);
        try {
            for (Annotation a: beanWrapper.getPropertyTypeDescriptor(key).getAnnotations()) {
                Class<?> annotationType = a.annotationType();
                if (annotationType.isAnnotationPresent(play.data.Form.Display.class)) {
                    play.data.Form.Display d = annotationType.getAnnotation(play.data.Form.Display.class);
                    if (d.name().startsWith("format.")) {
                        List<Object> attributes = new ArrayList<>();
                        for (String attr: d.attributes()) {
                            Object attrValue = null;
                            try {
                                attrValue = a.getClass().getDeclaredMethod(attr).invoke(a);
                            } catch(Exception e) {
                                // do nothing
                            }
                            attributes.add(attrValue);
                        }
                        format = Tuple(d.name(), Collections.unmodifiableList(attributes));
                    }
                }
            }
        } catch(NullPointerException e) {
            // do nothing
        }

        // Constraints
        List<Tuple<String,List<Object>>> constraints = new ArrayList<>();
        Class<?> classType = backedType;
        String leafKey = key;
        if (rootName != null && leafKey.startsWith(rootName + ".")) {
            leafKey = leafKey.substring(rootName.length() + 1);
        }
        int p = leafKey.lastIndexOf('.');
        if (p > 0) {
            classType = beanWrapper.getPropertyType(leafKey.substring(0, p));
            leafKey = leafKey.substring(p + 1);
        }
        if (classType != null && this.validatorFactory != null) {
            BeanDescriptor beanDescriptor = this.validatorFactory.getValidator().getConstraintsForClass(classType);
            if (beanDescriptor != null) {
                PropertyDescriptor property = beanDescriptor.getConstraintsForProperty(leafKey);
                if (property != null) {
                    Annotation[] orderedAnnotations = null;
                    for (Class<?> c = classType; c != null; c = c.getSuperclass()) { // we also check the fields of all superclasses
                        java.lang.reflect.Field field = null;
                        try {
                            field = c.getDeclaredField(leafKey);
                        } catch (NoSuchFieldException | SecurityException e) {
                            continue;
                        }
                        // getDeclaredAnnotations also looks for private fields; also it provides the annotations in a guaranteed order
                        orderedAnnotations = AnnotationUtils.unwrapContainerAnnotations(field.getDeclaredAnnotations());
                        break;
                    }
                    constraints = Constraints.displayableConstraint(
                            property.findConstraints().unorderedAndMatchingGroups(groups != null ? groups : new Class[]{Default.class}).getConstraintDescriptors(),
                            orderedAnnotations
                        );
                }
            }
        }

        return new Field(this, key, constraints, format, errors(key), fieldValue, file);
    }

    /**
     * @return the lang used for formatting when retrieving a field (via {@link #field(String)} or {@link #apply(String)})
     * and for translations in {@link #errorsAsJson()}. For these methods the lang can be change via {@link #withLang(Lang)}.
     */
    public Optional<Lang> lang() {
        return Optional.ofNullable(this.lang);
    }

    /**
     * A copy of this form with the given lang set which is used for formatting when retrieving a field (via {@link #field(String)} or {@link #apply(String)})
     * and for translations in {@link #errorsAsJson()}.
     */
    public Form<T> withLang(Lang lang) {
        return new Form<T>(this.rootName, this.backedType, this.rawData, this.files, this.errors, this.value, this.groups, this.messagesApi, this.formatters, this.validatorFactory, this.config, lang, this.directFieldAccess);
    }

    /**
     * Sets if during binding fields of the form should be accessed directly or via getters.
     *
     * @param directFieldAccess {@code true} enables direct field access during form binding, {@code false} disables it and uses getters instead. If {@code null} falls back to config default.
     */
    public Form<T> withDirectFieldAccess(boolean directFieldAccess) {
        return new Form<T>(this.rootName, this.backedType, this.rawData, this.files, this.errors, this.value, this.groups, this.messagesApi, this.formatters, this.validatorFactory, this.config, lang, directFieldAccess);
    }

    public String toString() {
        return "Form(of=" + backedType + ", data=" + rawData + ", value=" + value +", errors=" + errors + ")";
    }

    /**
     * Sets the locale of the current request (if there is one) into Spring's LocaleContextHolder.
     *
     * @param <T> the return type.
     * @param code The code to execute while the locale is set
     * @return the result of the code block
     */
    private static <T> T withRequestLocale(Lang lang, Supplier<T> code) {
        try {
            LocaleContextHolder.setLocale(lang != null ? lang.toLocale() : null);
        } catch(Exception e) {
            // Just continue (Maybe there is no context or some internal error in LocaleContextHolder). System default locale will be used.
        }
        try {
            return code.get();
        } finally {
            LocaleContextHolder.resetLocaleContext(); // Clean up ThreadLocal
        }
    }

    /**
     * A form field.
     */
    public static class Field {

        private final Form<?> form;
        private final String name;
        private final List<Tuple<String,List<Object>>> constraints;
        private final Tuple<String,List<Object>> format;
        private final List<ValidationError> errors;
        private final String value;
        private final Http.MultipartFormData.FilePart<?> file;

        /**
         * Creates a form field.
         *
         * @param form        the form.
         * @param name the field name
         * @param constraints the constraints associated with the field
         * @param format the format expected for this field
         * @param errors the errors associated with this field
         * @param value the field value, if any
         */
        public Field(Form<?> form, String name, List<Tuple<String,List<Object>>> constraints, Tuple<String,List<Object>> format, List<ValidationError> errors, String value) {
            this(form, name, constraints, format, errors, value, null);
        }

        public Field(Form<?> form, String name, List<Tuple<String,List<Object>>> constraints, Tuple<String,List<Object>> format, List<ValidationError> errors, Http.MultipartFormData.FilePart<?> file) {
            this(form, name, constraints, format, errors, null, file);
        }

        public Field(Form<?> form, String name, List<Tuple<String,List<Object>>> constraints, Tuple<String,List<Object>> format, List<ValidationError> errors, String value, Http.MultipartFormData.FilePart<?> file) {
            this.form = form;
            this.name = name;
            this.constraints = constraints != null ? new ArrayList<>(constraints) : new ArrayList<>();
            this.format = format;
            this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
            this.value = value;
            this.file = file;
        }

        /**
         * @return The field name.
         *
         * @deprecated Deprecated as of 2.7.0. Method has been renamed to {@link #name()}.
         */
        @Deprecated
        public Optional<String> getName() {
            return name();
        }

        /**
         * @return The field name.
         */
        public Optional<String> name() {
            return Optional.ofNullable(name);
        }

        /**
         * @return The field value, if defined.
         *
         * @deprecated Deprecated as of 2.7.0. Method has been renamed to {@link #value()}.
         */
        @Deprecated
        public Optional<String> getValue() {
            return value();
        }

        /**
         * @return The field value, if defined.
         */
        public Optional<String> value() {
            return Optional.ofNullable(value);
        }

        /**
         * @return The file, if defined.
         */
        public <A> Optional<Http.MultipartFormData.FilePart<A>> file() {
            return Optional.ofNullable((Http.MultipartFormData.FilePart<A>)file);
        }

        /**
         * Returns all the errors associated with this field.
         *
         * @return The errors associated with this field.
         */
        public List<ValidationError> errors() {
            return Collections.unmodifiableList(errors);
        }

        /**
         * Returns all the constraints associated with this field.
         *
         * @return The constraints associated with this field.
         */
        public List<Tuple<String,List<Object>>> constraints() {
            return Collections.unmodifiableList(constraints);
        }

        /**
         * Returns the expected format for this field.
         *
         * @return The expected format for this field.
         */
        public Tuple<String,List<Object>> format() {
            return format;
        }

        /**
         * @return the indexes available for this field (for repeated fields and List)
         */
        public List<Integer> indexes() {
            if(form == null) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableList(form.value().map((Function<Object, List<Integer>>) value -> {
                BeanWrapper beanWrapper = new BeanWrapperImpl(value);
                beanWrapper.setAutoGrowNestedPaths(true);
                String objectKey = name;
                if (form.name() != null && name.startsWith(form.name() + ".")) {
                    objectKey = name.substring(form.name().length() + 1);
                }

                List<Integer> result = new ArrayList<>();
                if (beanWrapper.isReadableProperty(objectKey)) {
                    Object value1 = beanWrapper.getPropertyValue(objectKey);
                    if (value1 instanceof Collection) {
                        for (int i = 0; i<((Collection<?>) value1).size(); i++) {
                            result.add(i);
                        }
                    }
                }

                return result;
            }).orElseGet(() -> {
                Set<Integer> result = new TreeSet<>();
                Pattern pattern = Pattern.compile("^" + Pattern.quote(name) + "\\[(\\d+)\\].*$");

                final Set<String> mergedSet = new LinkedHashSet<>(form.rawData().keySet());
                mergedSet.addAll(form.files().keySet());
                for (String key: mergedSet) {
                    java.util.regex.Matcher matcher = pattern.matcher(key);
                    if (matcher.matches()) {
                        result.add(Integer.parseInt(matcher.group(1)));
                    }
                }

                List<Integer> sortedResult = new ArrayList<>(result);
                Collections.sort(sortedResult);
                return sortedResult;
            }));
        }

        /**
         * Get a sub-field, with a key relative to the current field.
         * @param key    the key
         * @return the subfield corresponding to the key.
         */
        public Field sub(String key) {
            return sub(key, form.lang);
        }

        /**
         * Get a sub-field, with a key relative to the current field.
         * @param key    the key
         * @param lang used for formatting
         * @return the subfield corresponding to the key.
         */
        public Field sub(String key, Lang lang) {
            String subKey;
            if (key.startsWith("[")) {
                subKey = name + key;
            } else {
                subKey = name + "." + key;
            }
            return form.field(subKey, lang);
        }

        public String toString() {
            return "Form.Field(" + name + ")";
        }

    }

}
