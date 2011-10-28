package play.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

/**
 * like Properties, but with:
 * encoding
 * generic
 * type helper
 */
public class Properties extends HashMap<String, String> {

    private static final long serialVersionUID = 1L;

    public Properties() {
    }

    public synchronized void load(InputStream is) throws IOException {
        load(is, "utf-8");
    }

    public synchronized void load(InputStream is, String encoding) throws IOException {
        if (is == null) {
            throw new NullPointerException("Can't read from null stream");
        }
        BufferedReader rd = new BufferedReader(new InputStreamReader(is, encoding));
        while (true) {
            String tmp = rd.readLine();
            if (tmp == null) {
                break;
            }
            tmp = tmp.trim();

            if (tmp.startsWith("#")) {
                continue;
            }
            if (!tmp.contains("=")) {
                put(tmp, "");
                continue;
            }

            String[] kv = tmp.split("=", 2);
            if (kv.length == 2) {
                put(kv[0], kv[1]);
            } else {
                put(kv[0], "");
            }
        }
        rd.close();
    }

    public String get(String key, String defaultValue) {
        if (containsKey(key)) {
            return get(key);
        } else {
            return defaultValue;
        }
    }

    public synchronized void store(OutputStream out) throws IOException {
        store(out, "utf-8");
    }

    public synchronized void store(OutputStream out, String encoding) throws IOException {
        if (out == null) {
            throw new NullPointerException("Can't store to null stream");
        }
        BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(out, encoding));
        for (String key : keySet()) {
            if (key.length() > 0) {
                wr.write(key + "=" + get(key) + System.getProperties().getProperty("line.separator"));
            }
        }
        wr.flush();
        wr.close();
    }

    public boolean getBoolean(String key) throws IllegalArgumentException {
        String s = get(key);
        if (s == null || "".equals(s)) {
            throw new IllegalArgumentException("Setting must be an boolean (values:true/false/yes/no/on/off) : " + key);
        }
        s = s.trim().toLowerCase();
        return "true".equals(s) || "on".equals(s) || "yes".equals(s);
    }

    public boolean getBoolean(String key, boolean defval) {
        String s = get(key);
        if (s == null || "".equals(s)) {
            return defval;
        }
        s = s.trim().toLowerCase();
        return "true".equals(s) || "on".equals(s) || "yes".equals(s);
    }

    public Object getClassInstance(String key) throws IllegalArgumentException {
        String s = get(key);
        if (s == null || "".equals(s)) {
            throw new IllegalArgumentException("Setting " + key + " must be a valid classname  : " + key);
        }
        try {
            return Class.forName(s).newInstance();
        } catch (ClassNotFoundException nfe) {
            throw new IllegalArgumentException(s + ": invalid class name for key " + key, nfe);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(s + ": class could not be reflected " + s, e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(s + ": class could not be reflected " + s, e);
        }
    }

    public Object getClassInstance(String key, Object defaultinstance)
            throws IllegalArgumentException {
        return (containsKey(key) ? getClassInstance(key) : defaultinstance);
    }

    public double getDouble(String key) throws IllegalArgumentException {
        String s = get(key);
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Property must be an double value :" + key);
        }
    }

    public double getDouble(String key, long defval) throws IllegalArgumentException {
        String s = get(key);
        if (s == null) {
            return defval;
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Property must be an double value :" + key);
        }
    }

    public void setDouble(String key, double val) {
        put(key, Double.toString(val));
    }

    public float getFloat(String key) throws IllegalArgumentException {
        String s = get(key);
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Property must be an float value :" + key);
        }
    }

    public float getFloat(String key, float defval) throws IllegalArgumentException {
        String s = get(key);
        if (s == null) {
            return defval;
        }
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Property must be an float value :" + key);
        }
    }

    public void setFloat(String key, float val) {
        put(key, Float.toString(val));
    }

    public int getInt(String key) throws IllegalArgumentException {
        String s = get(key);
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Property must be an integer value :" + key);
        }
    }

    public int getInt(String key, int defval) throws IllegalArgumentException {
        String s = get(key);
        if (s == null) {
            return defval;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Property must be an integer value :" + key);
        }
    }

    public void setInt(String key, int val) {
        put(key, Integer.toString(val));
    }

    public long getLong(String key) throws IllegalArgumentException {
        String s = get(key);
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Property must be an long value :" + key);
        }
    }

    public long getLong(String key, long defval) throws IllegalArgumentException {
        String s = get(key);
        if (s == null) {
            return defval;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Property must be an long value :" + key);
        }
    }

    public void setLong(String key, long val) {
        put(key, Long.toString(val));
    }

    public URL getURL(String key) throws IllegalArgumentException {
        try {
            return new URL(get(key));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Property " + key + " must be a valid URL (" + get(key) + ")");
        }
    }
}
