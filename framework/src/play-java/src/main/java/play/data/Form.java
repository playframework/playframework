package play.data;

import javax.validation.*;
import javax.validation.metadata.*;

import java.util.*;
import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import play.mvc.Http;
import static play.libs.F.*;

import play.data.validation.*;

import org.springframework.beans.*;
import org.springframework.validation.*;
import org.springframework.validation.beanvalidation.*;
import org.springframework.context.support.*;

/**
 * Helper to manage HTML form description, submission and validation.
 */
public class Form<T> {

    // -- Form utilities
    
    /**
     * Instantiates a dynamic form.
     */
    public static DynamicForm form() {
        return new DynamicForm();
    }
    
    /**
     * Instantiates a new form that wraps the specified class.
     */
    public static <T> Form<T> form(Class<T> clazz) {
        return new Form<T>(clazz);
    }
    
    /**
     * Instantiates a new form that wraps the specified class.
     */
    public static <T> Form<T> form(String name, Class<T> clazz) {
        return new Form<T>(name, clazz);
    }
    
    /**
     * Instantiates a new form that wraps the specified class.
     */
    public static <T> Form<T> form(String name, Class<T> clazz, Class<?> group) {
        return new Form<T>(name, clazz, group);
    }

    /**
     * Instantiates a new form that wraps the specified class.
     */
    public static <T> Form<T> form(Class<T> clazz, Class<?> group) {
        return new Form<T>(null, clazz, group);
    }

    // ---
    
    /**
     * Defines a form element's display name.
     */
    @Retention(RUNTIME)
    @Target({ANNOTATION_TYPE})
    public static @interface Display {
        String name();
        String[] attributes() default {};
    }

    // --

    private final String rootName;
    private final Class<T> backedType;
    private final Map<String,String> data;
    private final Map<String,List<ValidationError>> errors;
    private final Option<T> value;
    private final Class<?> groups;

    private T blankInstance() {
        try {
            return backedType.newInstance();
        } catch(Exception e) {
            throw new RuntimeException("Cannot instantiate " + backedType + ". It must have a default constructor", e);
        }
    }

    /**
     * Creates a new <code>Form</code>.
     *
     * @param clazz wrapped class
     */
    public Form(Class<T> clazz) {
        this(null, clazz);
    }

    @SuppressWarnings("unchecked")
    public Form(String name, Class<T> clazz) {
        this(name, clazz, new HashMap<String,String>(), new HashMap<String,List<ValidationError>>(), None(),  null);
    }

    @SuppressWarnings("unchecked")
    public Form(String name, Class<T> clazz, Class<?> groups) {
        this(name, clazz, new HashMap<String,String>(), new HashMap<String,List<ValidationError>>(), None(), groups);
    }

    public Form(String rootName, Class<T> clazz, Map<String,String> data, Map<String,List<ValidationError>> errors, Option<T> value) {
        this(rootName, clazz, data, errors, value, null);
    }

    /**
     * Creates a new <code>Form</code>.
     *
     * @param clazz wrapped class
     * @param data the current form data (used to display the form)
     * @param errors the collection of errors associated with this form
     * @param value optional concrete value of type <code>T</code> if the form submission was successful
     */
    public Form(String rootName, Class<T> clazz, Map<String,String> data, Map<String,List<ValidationError>> errors, Option<T> value, Class<?> groups) {
        this.rootName = rootName;
        this.backedType = clazz;
        this.data = data;
        this.errors = errors;
        this.value = value;
        this.groups = groups;
    }

    protected Map<String,String> requestData(Http.Request request) {

        Map<String,String[]> urlFormEncoded = new HashMap<String,String[]>();
        if(request.body().asFormUrlEncoded() != null) {
            urlFormEncoded = request.body().asFormUrlEncoded();
        }

        Map<String,String[]> multipartFormData = new HashMap<String,String[]>();
        if(request.body().asMultipartFormData() != null) {
            multipartFormData = request.body().asMultipartFormData().asFormUrlEncoded();
        }

        Map<String,String> jsonData = new HashMap<String,String>();
        if(request.body().asJson() != null) {
            jsonData = play.libs.Scala.asJava(
                play.api.data.FormUtils.fromJson("", 
                    play.api.libs.json.Json.parse(
                        play.libs.Json.stringify(request.body().asJson())
                    )
                )
            );
        }

        Map<String,String[]> queryString = request.queryString();

        Map<String,String> data = new HashMap<String,String>();

        for(String key: urlFormEncoded.keySet()) {
            String[] values = urlFormEncoded.get(key);
            if(key.endsWith("[]")) {
                String k = key.substring(0, key.length() - 2);
                for(int i=0; i<values.length; i++) {
                    data.put(k + "[" + i + "]", values[i]);
                }
            } else {
                if(values.length > 0) {
                    data.put(key, values[0]);
                }
            }
        }

        for(String key: multipartFormData.keySet()) {
            String[] values = multipartFormData.get(key);
            if(key.endsWith("[]")) {
                String k = key.substring(0, key.length() - 2);
                for(int i=0; i<values.length; i++) {
                    data.put(k + "[" + i + "]", values[i]);
                }
            } else {
                if(values.length > 0) {
                    data.put(key, values[0]);
                }
            }
        }

        for(String key: jsonData.keySet()) {
            data.put(key, jsonData.get(key));
        }

        for(String key: queryString.keySet()) {
            String[] values = queryString.get(key);
            if(key.endsWith("[]")) {
                String k = key.substring(0, key.length() - 2);
                for(int i=0; i<values.length; i++) {
                    data.put(k + "[" + i + "]", values[i]);
                }
            } else {
                if(values.length > 0) {
                    data.put(key, values[0]);
                }
            }
        }

        return data;
    }

    /**
     * Binds request data to this form - that is, handles form submission.
     *
     * @return a copy of this form filled with the new data
     */
    public Form<T> bindFromRequest(String... allowedFields) {
        return bind(requestData(play.mvc.Controller.request()), allowedFields);
    }

    /**
     * Binds request data to this form - that is, handles form submission.
     *
     * @return a copy of this form filled with the new data
     */
    public Form<T> bindFromRequest(Http.Request request, String... allowedFields) {
        return bind(requestData(request), allowedFields);
    }

    /**
     * Binds request data to this form - that is, handles form submission.
     *
     * @return a copy of this form filled with the new data
     */
    public Form<T> bindFromRequest(Map<String,String[]> requestData, String... allowedFields) {
        Map<String,String> data = new HashMap<String,String>();
        for(String key: requestData.keySet()) {
            String[] values = requestData.get(key);
            if(key.endsWith("[]")) {
                String k = key.substring(0, key.length() - 2);
                for(int i=0; i<values.length; i++) {
                    data.put(k + "[" + i + "]", values[i]);
                }
            } else {
                if(values.length > 0) {
                    data.put(key, values[0]);
                }
            }
        }
        return bind(data, allowedFields);
    }

    /**
     * Binds Json data to this form - that is, handles form submission.
     *
     * @param data data to submit
     * @return a copy of this form filled with the new data
     */
    public Form<T> bind(com.fasterxml.jackson.databind.JsonNode data, String... allowedFields) {
        return bind(
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

    private static final Set<String> internalAnnotationAttributes = new HashSet<String>(3);
    static {
        internalAnnotationAttributes.add("message");
        internalAnnotationAttributes.add("groups");
        internalAnnotationAttributes.add("payload");
    }

    protected Object[] getArgumentsForConstraint(String objectName, String field, ConstraintDescriptor<?> descriptor) {
        List<Object> arguments = new LinkedList<Object>();
        String[] codes = new String[] {objectName + Errors.NESTED_PATH_SEPARATOR + field, field};
        arguments.add(new DefaultMessageSourceResolvable(codes, field));
        // Using a TreeMap for alphabetical ordering of attribute names
        Map<String, Object> attributesToExpose = new TreeMap<String, Object>();
        for (Map.Entry<String, Object> entry : descriptor.getAttributes().entrySet()) {
            String attributeName = entry.getKey();
            Object attributeValue = entry.getValue();
            if (!internalAnnotationAttributes.contains(attributeName)) {
                attributesToExpose.put(attributeName, attributeValue);
            }
        }
        arguments.addAll(attributesToExpose.values());
        return arguments.toArray(new Object[arguments.size()]);
    }

    /**
     * Binds data to this form - that is, handles form submission.
     *
     * @param data data to submit
     * @return a copy of this form filled with the new data
     */
    @SuppressWarnings("unchecked")
    public Form<T> bind(Map<String,String> data, String... allowedFields) {

        DataBinder dataBinder = null;
        Map<String, String> objectData = data;
        if(rootName == null) {
            dataBinder = new DataBinder(blankInstance());
        } else {
            dataBinder = new DataBinder(blankInstance(), rootName);
            objectData = new HashMap<String,String>();
            for(String key: data.keySet()) {
                if(key.startsWith(rootName + ".")) {
                    objectData.put(key.substring(rootName.length() + 1), data.get(key));
                }
            }
        }
        if(allowedFields.length > 0) {
            dataBinder.setAllowedFields(allowedFields);
        }
        SpringValidatorAdapter validator = new SpringValidatorAdapter(play.data.validation.Validation.getValidator());
        dataBinder.setValidator(validator);
        dataBinder.setConversionService(play.data.format.Formatters.conversion);
        dataBinder.setAutoGrowNestedPaths(true);
        dataBinder.bind(new MutablePropertyValues(objectData));
        Set<ConstraintViolation<Object>> validationErrors;
        if (groups != null) {
            validationErrors = validator.validate(dataBinder.getTarget(), groups);
        } else {
            validationErrors = validator.validate(dataBinder.getTarget());
        }

        BindingResult result = dataBinder.getBindingResult();

        for (ConstraintViolation<Object> violation : validationErrors) {
            String field = violation.getPropertyPath().toString();
            FieldError fieldError = result.getFieldError(field);
            if (fieldError == null || !fieldError.isBindingFailure()) {
                try {
                    result.rejectValue(field,
                            violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
                            getArgumentsForConstraint(result.getObjectName(), field, violation.getConstraintDescriptor()),
                            violation.getMessage());
                }
                catch (NotReadablePropertyException ex) {
                    throw new IllegalStateException("JSR-303 validated property '" + field +
                            "' does not have a corresponding accessor for data binding - " +
                            "check your DataBinder's configuration (bean property versus direct field access)", ex);
                }
            }
        }

        if(result.hasErrors()) {
            Map<String,List<ValidationError>> errors = new HashMap<String,List<ValidationError>>();
            for(FieldError error: result.getFieldErrors()) {
                String key = error.getObjectName() + "." + error.getField();
                if(key.startsWith("target.") && rootName == null) {
                    key = key.substring(7);
                }
                List<Object> arguments = new ArrayList<Object>();
                for(Object arg: error.getArguments()) {
                    if(!(arg instanceof org.springframework.context.support.DefaultMessageSourceResolvable)) {
                        arguments.add(arg);
                    }                    
                }
                if(!errors.containsKey(key)) {
                   errors.put(key, new ArrayList<ValidationError>());
                }

                String message = "error.invalid";
                if( error.isBindingFailure() ){
                    for(String code : error.getCodes() ){
                        code = code.replace("typeMismatch", "error.invalid");
                        if( play.i18n.Messages.isDefined(code) ){
                            message = code;
                            break;
                        }
                    }
                }else{
                    message = error.getDefaultMessage();
                }

                errors.get(key).add(new ValidationError(key, message, arguments));
            }
            return new Form(rootName, backedType, data, errors, None(), groups);
        } else {
            Object globalError = null;
            if(result.getTarget() != null) {
                try {
                    java.lang.reflect.Method v = result.getTarget().getClass().getMethod("validate");
                    globalError = v.invoke(result.getTarget());
                } catch(NoSuchMethodException e) {
                } catch(Throwable e) {
                    throw new RuntimeException(e);
                }
            }
            if(globalError != null) {
                Map<String,List<ValidationError>> errors = new HashMap<String,List<ValidationError>>();
                if(globalError instanceof String) {
                    errors.put("", new ArrayList<ValidationError>());
                    errors.get("").add(new ValidationError("", (String)globalError, new ArrayList()));
                } else if(globalError instanceof List) {
                    for (ValidationError error : (List<ValidationError>) globalError) {
                      List<ValidationError> errorsForKey = errors.get(error.key());
                      if (errorsForKey == null) {
                        errors.put(error.key(), errorsForKey = new ArrayList<ValidationError>());
                      }
                      errorsForKey.add(error);
                    }
                } else if(globalError instanceof Map) {
                    errors = (Map<String,List<ValidationError>>)globalError;
                }
                return new Form(rootName, backedType, data, errors, None(), groups);
            }
            return new Form(rootName, backedType, new HashMap<String,String>(data), new HashMap<String,List<ValidationError>>(errors), Some((T)result.getTarget()), groups);
        }
    }

    /**
     * Retrieves the actual form data.
     */
    public Map<String,String> data() {
        return data;
    }

    public String name() {
        return rootName;
    }

    /**
     * Retrieves the actual form value.
     */
    public Option<T> value() {
        return value;
    }

    /**
     * Populates this form with an existing value, used for edit forms.
     *
     * @param value existing value of type <code>T</code> used to fill this form
     * @return a copy of this form filled with the new data
     */
    @SuppressWarnings("unchecked")
    public Form<T> fill(T value) {
        if(value == null) {
            throw new RuntimeException("Cannot fill a form with a null value");
        }
        return new Form(rootName, backedType, new HashMap<String,String>(), new HashMap<String,ValidationError>(), Some(value), groups);
    }

    /**
     * Returns <code>true</code> if there are any errors related to this form.
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Returns <code>true</code> if there any global errors related to this form.
     */
    public boolean hasGlobalErrors() {
        return errors.containsKey("") && !errors.get("").isEmpty();
    }

    /**
     * Retrieve all global errors - errors without a key.
     *
     * @return All global errors.
     */
    public List<ValidationError> globalErrors() {
        List<ValidationError> e = errors.get("");
        if(e == null) {
            e = new ArrayList<ValidationError>();
        }
        return e;
    }

    /**
     * Retrieves the first global error (an error without any key), if it exists.
     *
     * @return An error or <code>null</code>.
     */
    public ValidationError globalError() {
        List<ValidationError> errors = globalErrors();
        if(errors.isEmpty()) {
            return null;
        } else {
            return errors.get(0);
        }
    }

    /**
     * Returns all errors.
     *
     * @return All errors associated with this form.
     */
    public Map<String,List<ValidationError>> errors() {
        return errors;
    }

    /**
     * Retrieve an error by key.
     */
    public ValidationError error(String key) {
        List<ValidationError> err = errors.get(key);
        if(err == null || err.isEmpty()) {
            return null;
        } else {
            return err.get(0);
        }
    }

    /**
     * Returns the form errors serialized as Json.
     */
    public com.fasterxml.jackson.databind.JsonNode errorsAsJson() {
        return errorsAsJson(Http.Context.Implicit.lang());
    }

    /**
     * Returns the form errors serialized as Json using the given Lang.
     */
    public com.fasterxml.jackson.databind.JsonNode errorsAsJson(play.i18n.Lang lang) {
        Map<String, List<String>> allMessages = new HashMap<String, List<String>>();
        for (String key : errors.keySet()) {
            List<ValidationError> errs = errors.get(key);
            if (errs != null && !errs.isEmpty()) {
                List<String> messages = new ArrayList<String>();
                for (ValidationError error : errs) {
                    messages.add(play.i18n.Messages.get(lang, error.message(), error.arguments()));
                }
                allMessages.put(key, messages);
            }
        }
        return play.libs.Json.toJson(allMessages);
    }

    /**
     * Gets the concrete value if the submission was a success.
     */
    public T get() {
        return value.get();
    }

    /**
     * Adds an error to this form.
     *
     * @param error the <code>ValidationError</code> to add.
     */
    public void reject(ValidationError error) {
        if(!errors.containsKey(error.key())) {
           errors.put(error.key(), new ArrayList<ValidationError>()); 
        }
        errors.get(error.key()).add(error);
    }

    /**
     * Adds an error to this form.
     *
     * @param key the error key
     * @param error the error message
     * @param args the errot arguments
     */
    public void reject(String key, String error, List<Object> args) {
        reject(new ValidationError(key, error, args));
    }

    /**
     * Adds an error to this form.
     *
     * @param key the error key
     * @param error the error message
     */    
    public void reject(String key, String error) {
        reject(key, error, new ArrayList<Object>());
    }

    /**
     * Adds a global error to this form.
     *
     * @param error the error message
     * @param args the errot arguments
     */
    public void reject(String error, List<Object> args) {
        reject(new ValidationError("", error, args));
    }

    /**
     * Add a global error to this form.
     *
     * @param error the error message.
     */    
    public void reject(String error) {
        reject("", error, new ArrayList<Object>());
    }

    /**
     * Discard errors of this form
     */
    public void discardErrors() {
        errors.clear();
    }

    /**
     * Retrieve a field.
     *
     * @param key field name
     * @return the field (even if the field does not exist you get a field)
     */
    public Field apply(String key) {
        return field(key);
    }

    /**
     * Retrieve a field.
     *
     * @param key field name
     * @return the field (even if the field does not exist you get a field)
     */
    public Field field(String key) {

        // Value
        String fieldValue = null;
        if(data.containsKey(key)) {
            fieldValue = data.get(key);
        } else {
            if(value.isDefined()) {
                BeanWrapper beanWrapper = new BeanWrapperImpl(value.get());
                beanWrapper.setAutoGrowNestedPaths(true);
                String objectKey = key;
                if(rootName != null && key.startsWith(rootName + ".")) {
                    objectKey = key.substring(rootName.length() + 1);
                }
                if(beanWrapper.isReadableProperty(objectKey)) {
                    Object oValue = beanWrapper.getPropertyValue(objectKey);
                    if(oValue != null) {
                        fieldValue = play.data.format.Formatters.print(beanWrapper.getPropertyTypeDescriptor(objectKey), oValue);
                    }
                }
            }
        }

        // Error
        List<ValidationError> fieldErrors = errors.get(key);
        if(fieldErrors == null) {
            fieldErrors = new ArrayList<ValidationError>();
        }

        // Format
        Tuple<String,List<Object>> format = null;
        BeanWrapper beanWrapper = new BeanWrapperImpl(blankInstance());
        beanWrapper.setAutoGrowNestedPaths(true);
        try {
            for(Annotation a: beanWrapper.getPropertyTypeDescriptor(key).getAnnotations()) {
                Class<?> annotationType = a.annotationType();
                if(annotationType.isAnnotationPresent(play.data.Form.Display.class)) {
                    play.data.Form.Display d = annotationType.getAnnotation(play.data.Form.Display.class);
                    if(d.name().startsWith("format.")) {
                        List<Object> attributes = new ArrayList<Object>();
                        for(String attr: d.attributes()) {
                            Object attrValue = null;
                            try {
                                attrValue = a.getClass().getDeclaredMethod(attr).invoke(a);
                            } catch(Exception e) {}
                            attributes.add(attrValue);
                        }
                        format = Tuple(d.name(), attributes);
                    }
                }
            }
        } catch(NullPointerException e) {}

        // Constraints
        List<Tuple<String,List<Object>>> constraints = new ArrayList<Tuple<String,List<Object>>>();
        Class<?> classType = backedType;
        String leafKey = key;
        if(rootName != null && leafKey.startsWith(rootName + ".")) {
            leafKey = leafKey.substring(rootName.length() + 1);
        }
        int p = leafKey.lastIndexOf('.');
        if (p > 0) {
            classType = beanWrapper.getPropertyType(leafKey.substring(0, p));
            leafKey = leafKey.substring(p + 1);
        }
        if (classType != null) {
            BeanDescriptor beanDescriptor = play.data.validation.Validation.getValidator().getConstraintsForClass(classType);
            if (beanDescriptor != null) {
                PropertyDescriptor property = beanDescriptor.getConstraintsForProperty(leafKey);
                if(property != null) {
                    constraints = Constraints.displayableConstraint(property.getConstraintDescriptors());
                }
            }
        }

        return new Field(this, key, constraints, format, fieldErrors, fieldValue);
    }

    public String toString() {
        return "Form(of=" + backedType + ", data=" + data + ", value=" + value +", errors=" + errors + ")";
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

        /**
         * Creates a form field.
         *
         * @param name the field name
         * @param constraints the constraints associated with the field
         * @param format the format expected for this field
         * @param errors the errors associated with this field
         * @param value the field value ,if any
         */
        public Field(Form<?> form, String name, List<Tuple<String,List<Object>>> constraints, Tuple<String,List<Object>> format, List<ValidationError> errors, String value) {
            this.form = form;
            this.name = name;
            this.constraints = constraints;
            this.format = format;
            this.errors = errors;
            this.value = value;
        }

        /**
         * Returns the field name.
         *
         * @return The field name.
         */
        public String name() {
            return name;
        }

        /**
         * Returns the field value, if defined.
         *
         * @return The field value, if defined.
         */
        public String value() {
            return value;
        }

        public String valueOr(String or) {
            if(value == null) {
                return or;
            }
            return value;
        }

        /**
         * Returns all the errors associated with this field.
         *
         * @return The errors associated with this field.
         */
        public List<ValidationError> errors() {
            return errors;
        }

        /**
         * Returns all the constraints associated with this field.
         *
         * @return The constraints associated with this field.
         */
        public List<Tuple<String,List<Object>>> constraints() {
            return constraints;
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
         * Return the indexes available for this field (for repeated fields ad List)
         */
        @SuppressWarnings("rawtypes")
        public List<Integer> indexes() {
            List<Integer> result = new ArrayList<Integer>();
            if(form.value().isDefined()) {
                BeanWrapper beanWrapper = new BeanWrapperImpl(form.value().get());
                beanWrapper.setAutoGrowNestedPaths(true);
                String objectKey = name;
                if(form.name() != null && name.startsWith(form.name() + ".")) {
                    objectKey = name.substring(form.name().length() + 1);
                }
                if(beanWrapper.isReadableProperty(objectKey)) {
                    Object value = beanWrapper.getPropertyValue(objectKey);
                    if(value instanceof Collection) {
                        for(int i=0; i<((Collection)value).size(); i++) {
                            result.add(i);
                        }
                    }
                }
            } else {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^" + java.util.regex.Pattern.quote(name) + "\\[(\\d+)\\].*$");
                for(String key: form.data().keySet()) {
                    java.util.regex.Matcher matcher = pattern.matcher(key);
                    if(matcher.matches()) {
                        result.add(Integer.parseInt(matcher.group(1)));
                    }
                }
            }
            return result;
        }

        /**
         * Get a sub-field, with a key relative to the current field.
         */
        public Field sub(String key) {
            String subKey = null;
            if(key.startsWith("[")) {
                subKey = name + key;
            } else {
                subKey = name + "." + key;
            }
            return form.field(subKey);
        }

        public String toString() {
            return "Form.Field(" + name + ")";
        }

    }

}
