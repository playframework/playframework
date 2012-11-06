package play.libs;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * XML utilities.
 */
public class XML {
    
    /**
     * Parse an XML string as DOM.
     */ 
    public static Document fromString(String xml) {
        try {
            return fromInputStream(
                new ByteArrayInputStream(xml.getBytes("utf-8")),
                "utf-8"
            );
        } catch(UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parse an InputStream as DOM.
     */
    public static Document fromInputStream(InputStream in, String encoding) {
       try {

           DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
           factory.setNamespaceAware(true);
           DocumentBuilder builder = factory.newDocumentBuilder();

           InputSource is = new InputSource(in);
           is.setEncoding(encoding);
           
           return builder.parse(is);

       } catch (ParserConfigurationException e) {
           throw new RuntimeException(e);
       } catch (SAXException e) {
           throw new RuntimeException(e);
       } catch (IOException e) {
           throw new RuntimeException(e);
       }
    }

}
