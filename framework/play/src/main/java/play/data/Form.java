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

public class Form<T> {
    
    protected final Class<T> backedType;
    protected final Either<T2<Map<String,String>, Map<String,ValidationError>>,T> internal;
    protected final BeanWrapper beanWrapper;

    public Form(T t) {
        this.backedType = (Class<T>)t.getClass();
        this.internal = Either._2(t);
        this.beanWrapper = new BeanWrapperImpl(t);
        this.beanWrapper.setAutoGrowNestedPaths(true);
    }
    
    public Form(Class<T> clazz, T2<Map<String,String>, Map<String,ValidationError>> dataAndErrors) {
        this.backedType = clazz;
        this.internal = Either._1(dataAndErrors);
        this.beanWrapper = null;
    }
    
    public static <T> Form<T> bind(Map<String,String> data, T initialValue) {
        DataBinder dataBinder = new DataBinder(initialValue);
        dataBinder.setValidator(new SpringValidatorAdapter(play.data.validation.Validation.getValidator()));
        dataBinder.setConversionService(play.data.format.Formatters.conversion);
        dataBinder.bind(new MutablePropertyValues(data));
        dataBinder.validate();
        BindingResult result = dataBinder.getBindingResult();
        
        if(result.hasErrors()) {
            Map<String,ValidationError> errors = new HashMap<String,ValidationError>();
            for(FieldError error: result.getFieldErrors()) {
                String key = error.getObjectName() + "." + error.getField();
                if(key.startsWith("target.")) {
                    key = key.substring(7);
                }
                errors.put(key, new ValidationError(error.isBindingFailure(), error.getCodes(), error.getArguments(), error.getDefaultMessage()));
            }
            return new Form(initialValue.getClass(), T2(data, errors));
        } else {
            return new Form((T)result.getTarget());
        }
    }
    
    public boolean hasErrors() {
        for(T2<Map<String,String>, Map<String,ValidationError>> t: internal._1) {
            return !t._2.isEmpty();
        }

        return false;
    }
    
    public Map<String,ValidationError> errors() {
        for(T2<Map<String,String>, Map<String,ValidationError>> t: internal._1) {
            return t._2;
        }

        return new HashMap<String,ValidationError>();
    }
    
    public T get() {
        return this.internal._2.get();
    }
    
    public Field field(String key) {
        return new Field(this, key);
    }
    
    public String toString() {
        return internal.toString();
    }
    
    public static class Field {
        
        private final String name;
        private final Form<?> form;
        
        public Field(Form<?> form, String name) {
            this.form = form;
            this.name = name;
        }
        
        public String name() {
            return name;
        }
        
        public String value() {
            for(Object o: form.internal._2) {
                Object value = form.beanWrapper.getPropertyValue(name);
                if(value != null) {
                    return play.data.format.Formatters.print(form.beanWrapper.getPropertyTypeDescriptor(name), value);
                } else {
                    return "";
                }
            }
            
            String data = form.internal._1.get()._1.get(name);
            if(data == null) {
                return "";
            } else {
                return data;
            }
        }
        
        public ValidationError error() {
            for(T2<Map<String,String>, Map<String,ValidationError>> t: form.internal._1) {
                return t._2.get(name);
            }
            
            return null;
        }
        
        public Set<String> constraints() {
            Set<String> constraintsNames = new HashSet<String>();
            
            PropertyDescriptor property = play.data.validation.Validation.getValidator()
                .getConstraintsForClass(form.backedType)
                .getConstraintsForProperty(name);
            
            if(property != null) {
                for(ConstraintDescriptor<?> c: property.getConstraintDescriptors()) {
                    constraintsNames.add(c.getAnnotation().annotationType().getName());
                }
            }
            
            return constraintsNames;
        }
        
        public String toString() {
            return "Form.Field(" + name + ")";
        }
    
    }
    
    public static class Dynamic {
        
        private Map data;
        
        public Map getData() {
            return data;
        }
        
        public void setData(Map data) {
            this.data = data;
        }
        
        public String toString() {
            return "Form.Dynamic(" + data.toString() + ")";
        }
        
    }
    
}