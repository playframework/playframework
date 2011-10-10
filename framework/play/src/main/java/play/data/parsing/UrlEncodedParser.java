package play.data.parsing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

//import play.Logger;
//import play.exceptions.UnexpectedException;
import play.mvc.Http;
//import play.utils.Utils;

/**
 * Parse url-encoded requests.
 */
public class UrlEncodedParser  {

    boolean forQueryString = false;

    public static Map<String, String[]> parse(String urlEncoded, String encoding) {
        try {
            return new UrlEncodedParser().parse(new ByteArrayInputStream(urlEncoded.getBytes( encoding )),encoding);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Map<String, String[]> parseQueryString(InputStream is,String encoding) {
        UrlEncodedParser parser = new UrlEncodedParser();
        parser.forQueryString = true;
        return parser.parse(is,encoding);
    }

    private static void mergeValueInMap(Map<String, String[]> map, String name, String value) {
            String[] newValues = null;
            String[] oldValues = map.get(name);
            if (oldValues == null) {
                newValues = new String[1];
                newValues[0] = value;
            } else {
                newValues = new String[oldValues.length + 1];
                System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
                newValues[oldValues.length] = value;
            }
            map.put(name, newValues);
        }

    public Map<String, String[]> parse(InputStream is,String encoding) {
        // Encoding is either retrieved from contentType or it is the default encoding

        try {
            Map<String, String[]> params = new HashMap<String, String[]>();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ( (bytesRead = is.read(buffer)) > 0 ) {
                os.write( buffer, 0, bytesRead);
            }

            String data = new String(os.toByteArray(), encoding);
            if (data.length() == 0) {
                //data is empty - can skip the rest
                return new HashMap<String, String[]>(0);
            }

            // data is o the form:
            // a=b&b=c%12...

            // Let us parse in two phases - we wait until everything is parsed before
            // we decoded it - this makes it possible for use to look for the
            // special _charset_ param which can hold the charset the form is encoded in.
            //
            // http://www.crazysquirrel.com/computing/general/form-encoding.jspx
            // https://bugzilla.mozilla.org/show_bug.cgi?id=18643
            //
            // NB: _charset_ must always be used with accept-charset and it must have the same value

            String[] keyValues = data.split("&");
            for (String keyValue : keyValues) {
                // split this key-value on the first '='
                int i = keyValue.indexOf('=');
                String key=null;
                String value=null;
                if ( i > 0) {
                    key = keyValue.substring(0,i);
                    value = keyValue.substring(i+1);
                } else {
                    key = keyValue;
                }
                if (key.length()>0) {
                    mergeValueInMap(params, key, value);
                }
            }

            // Second phase - look for _charset_ param and do the encoding
            String charset = encoding;
            if (params.containsKey("_charset_")) {
                // The form contains a _charset_ param - When this is used together
                // with accept-charset, we can use _charset_ to extract the encoding.
                // PS: When rendering the view/form, _charset_ and accept-charset must be given the
                // same value - since only Firefox and sometimes IE actually sets it when Posting
                String providedCharset = params.get("_charset_")[0];
                // Must be sure the providedCharset is a valid encoding..
                try {
                    "test".getBytes(providedCharset);
                    charset = providedCharset; // it works..
                } catch (Exception e) {
                    //Logger.debug("Got invalid _charset_ in form: " + providedCharset);
                    // lets just use the default one..
                }
            }

            // We're ready to decode the params
            Map<String, String[]> decodedParams = new HashMap<String, String[]>(params.size());
            for (Map.Entry<String, String[]> e : params.entrySet()) {
                String key = URLDecoder.decode(e.getKey(),charset);
                for (String value : e.getValue()) {

                    mergeValueInMap(decodedParams, key, (value==null ? null : URLDecoder.decode(value,charset)));
                }
            }

            // add the complete body as a parameters
            if(!forQueryString) {
                decodedParams.put("body", new String[] {data});
            }

            return decodedParams;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}