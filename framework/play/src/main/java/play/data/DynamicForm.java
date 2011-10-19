package play.data;

import java.util.*;

import play.libs.F;
import static play.libs.F.*;

import play.data.validation.*;

public class DynamicForm extends Form<DynamicForm.Dynamic> {
    
    public DynamicForm() {
        super(DynamicForm.Dynamic.class);
    }
    
    public DynamicForm(Map<String,String> data, Map<String,List<ValidationError>> errors, Option<Dynamic> value) {
        super(DynamicForm.Dynamic.class, data, errors, value);
    }
    
    public String get(String key) {
        return (String)get().getData().get(key);
    }
    
    public DynamicForm bindFromRequest() {
        Form<Dynamic> form = super.bindFromRequest();
        return new DynamicForm(form.data, form.errors, form.value);
    }
    
    public DynamicForm bind(Map<String,String> data) {
        Form<Dynamic> form = super.bind(data);
        return new DynamicForm(form.data, form.errors, form.value);
    }
    
    public Field field(String key) {
        return super.field("data[" + key + "]");
    }
    
    public static class Dynamic {

        private Map data = new HashMap();

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

