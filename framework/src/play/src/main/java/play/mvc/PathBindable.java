package play.mvc;

import play.libs.F.*;

public interface PathBindable<T> {
    
    public T bind(String key, String txt);
    public String unbind(String key);
    public String javascriptUnbind();
    
}