package play.mvc;

import java.util.*;

import play.libs.F.*;

public interface QueryStringBindable<T> {
    
    public Option<T> bind(String key, Map<String,String[]> data);
    public String unbind(String key);
    public String javascriptUnbind();
    
}