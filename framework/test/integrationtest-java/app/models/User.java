package models;

import play.mvc.PathBindable;

public class User implements PathBindable<User> {
    public String email;

    @Override
    public User bind(String key, String txt) {
        email = txt;
        return this;
    }

    @Override
    public String unbind(String key) {
        return email;
    }

    @Override
    public String javascriptUnbind() {
        return "function(k,v) {\n" +
                "    return v.email;\n" +
                "}";
    }
}
