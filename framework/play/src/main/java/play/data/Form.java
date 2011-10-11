package play.data;

import org.springframework.beans.*;
import org.springframework.validation.*;
import org.springframework.validation.beanvalidation.*;

import javax.validation.*;
import javax.validation.metadata.*;

import org.hibernate.validator.engine.*;

import java.util.*;

import play.libs.F;
import static play.libs.F.*;

import play.data.validation.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

public class Form<T> {
    
    @Target({ANNOTATION_TYPE})
    @Retention(RUNTIME)
    public static @interface Display {
        String name();
        String[] attributes() default {};
    }
    
    // --
    
    protected final Class<T> backedType;
    protected final Map<String,String> data;
    protected final Map<String,List<ValidationError>> errors;
    protected final Option<T> value;
    protected final T blankInstance;
    
    public Form(Class<T> clazz) {
        this(clazz, new HashMap<String,String>(), new HashMap<String,List<ValidationError>>(), None());
    }
    
    public Form(T t) {
        this((Class<T>)t.getClass(), new HashMap<String,String>(), new HashMap<String,List<ValidationError>>(), Some(t));
    }
    
    public Form(Class<T> clazz, Map<String,String> data, Map<String,List<ValidationError>> errors, Option<T> value) {
        this.backedType = clazz;
        this.data = data;
        this.errors = errors;
        this.value = value;
        try {
            blankInstance = backedType.newInstance();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public Form<T> bind() {
        Map<String,String> data = new HashMap<String,String>();
        Map<String,String[]> urlFormEncoded = play.mvc.Controller.request().urlFormEncoded();
        for(String key: urlFormEncoded.keySet()) {
            String[] value = urlFormEncoded.get(key);
            if(value.length > 0) {
                data.put(key, value[0]);
            }
        }
        return bind(data);
    }
    
    public Form<T> bind(Map<String,String> data) {
        DataBinder dataBinder = new DataBinder(blankInstance);
        dataBinder.setValidator(new SpringValidatorAdapter(play.data.validation.Validation.getValidator()));
        dataBinder.setConversionService(play.data.format.Formatters.conversion);
        dataBinder.bind(new MutablePropertyValues(data));
        dataBinder.validate();
        BindingResult result = dataBinder.getBindingResult();
        
        if(result.hasErrors()) {
            Map<String,List<ValidationError>> errors = new HashMap<String,List<ValidationError>>();
            for(FieldError error: result.getFieldErrors()) {
                String key = error.getObjectName() + "." + error.getField();
                if(key.startsWith("target.")) {
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
                errors.get(key).add(new ValidationError(key, error.isBindingFailure() ? "error.invalid" : error.getDefaultMessage(), arguments));
            }
            return new Form(backedType, data, errors, None());
        } else {
            String globalError = null;
            if(result.getTarget() != null) {
                try {
                    java.lang.reflect.Method v = result.getTarget().getClass().getMethod("validate");
                    globalError = (String)v.invoke(result.getTarget());
                } catch(Exception e) {}
            }
            if(globalError != null) {
                Map<String,List<ValidationError>> errors = new HashMap<String,List<ValidationError>>();
                errors.put("", new ArrayList<ValidationError>());
                errors.get("").add(new ValidationError("", globalError, new ArrayList()));
                return new Form(backedType, data, errors, None());
            }
            return new Form(backedType, data, errors, Some((T)result.getTarget()));
        }
    }
    
    public Form<T> fill(T value) {
        return new Form(backedType, new HashMap<String,String>(), new HashMap<String,ValidationError>(), Some(value));
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public boolean hasGlobalErrors() {
        return errors.containsKey("") && !errors.get("").isEmpty();
    }
    
    public List<ValidationError> globalErrors() {
        List<ValidationError> e = errors.get("");
        if(e == null) {
            e = new ArrayList<ValidationError>();
        }
        return e;
    }
    
    public ValidationError globalError() {
        List<ValidationError> errors = globalErrors();
        if(errors.isEmpty()) {
            return null;
        } else {
            return errors.get(0);
        }
    }
    
    public Map<String,List<ValidationError>> errors() {
        return errors;
    }
    
    public T get() {
        return value.get();
    }
    
    public Field apply(String key) {
        return field(key);
    }
    
    public void reject(ValidationError error) {
        if(!errors.containsKey(error.key())) {
           errors.put(error.key(), new ArrayList<ValidationError>()); 
        }
        errors.get(error.key()).add(error);
    }
    
    public void reject(String key, String error, List<Object> args) {
        reject(new ValidationError(key, error, args));
    }
    
    public void reject(String key, String error) {
        reject(key, error, new ArrayList());
    }
    
    public void reject(String error, List<Object> args) {
        reject(new ValidationError("", error, args));
    }
    
    public void reject(String error) {
        reject("", error, new ArrayList());
    }
    
    public Field field(String key) {
        
        // Value
        String fieldValue = null;
        if(value.isDefined()) {
            BeanWrapper beanWrapper = new BeanWrapperImpl(value.get());
            Object oValue = beanWrapper.getPropertyValue(key);
            if(oValue != null) {
                fieldValue = play.data.format.Formatters.print(beanWrapper.getPropertyTypeDescriptor(key), oValue);
            }
        } else {
            if(data.containsKey(key)) {
                fieldValue = data.get(key);
            } 
        }
        
        // Error
        List<ValidationError> fieldErrors = errors.get(key);
        if(fieldErrors == null) {
            fieldErrors = new ArrayList<ValidationError>();
        }
        
        // Format
        T2<String,List<Object>> format = null;
        BeanWrapper beanWrapper = new BeanWrapperImpl(blankInstance);
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
                        format = T2(d.name(), attributes);
                    }
                }
            }
        } catch(NullPointerException e) {}

        
        // Constraints
        PropertyDescriptor property = play.data.validation.Validation.getValidator().getConstraintsForClass(backedType).getConstraintsForProperty(key);
        List<T2<String,List<Object>>> constraints = new ArrayList<T2<String,List<Object>>>();
        if(property != null) {
            constraints = Constraints.displayableConstraint(property.getConstraintDescriptors());
        }
        
        return new Field(key, constraints, format, fieldErrors, fieldValue);
    }
    
    public String toString() {
        return "Form";
    }
    
    public static class Field {
        
        private final String name;
        private final List<T2<String,List<Object>>> constraints;
        private final T2<String,List<Object>> format;
        private final List<ValidationError> errors;
        private final String value;
        
        public Field(String name, List<T2<String,List<Object>>> constraints, T2<String,List<Object>> format, List<ValidationError> errors, String value) {
            this.name = name;
            this.constraints = constraints;
            this.format = format;
            this.errors = errors;
            this.value = value;
        }
        
        public String name() {
            return name;
        }
        
        public String value() {
            return value;
        }
        
        public List<ValidationError> errors() {
            return errors;
        }
        
        public List<T2<String,List<Object>>> constraints() {
            return constraints;
        }
        
        public T2<String,List<Object>> format() {
            return format;
        }
        
        public String toString() {
            return "Form.Field(" + name + ")";
        }
    
    }
    
}