package controllers;

import play.libs.F;
import play.mvc.QueryStringBindable;

import java.util.Map;

public class Pager implements QueryStringBindable<Pager> {
    public int index;
    public int size;

    @Override
    public F.Option<Pager> bind(String key, Map<String, String[]> data) {
        if (data.containsKey(key + ".index") && data.containsKey(key + ".size")) {
            try {
                index = Integer.parseInt(data.get(key + ".index")[0]);
                size = Integer.parseInt(data.get(key + ".size")[0]);
                return F.Option.Some(this);
            } catch (NumberFormatException e) {
                return F.Option.None();
            }
        } else {
            return F.Option.None();
        }
    }

    @Override
    public String unbind(String key) {
        return key + ".index=" + index + "&" + key + ".size=" + size;
    }

    @Override
    public String javascriptUnbind() {
        return "function(k,v) {\n" +
                "    return encodeURIComponent(k+'.index')+'='+v.index+'&'+encodeURIComponent(k+'.size')+'='+v.size;\n" +
                "}";
    }

    @Override
    public String toString() {
        return "Pager{" +
                "index=" + index +
                ", size=" + size +
                '}';
    }
}
