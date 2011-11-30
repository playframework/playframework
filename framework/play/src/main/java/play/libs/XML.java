package play.libs;

import java.io.InputStream;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XML {

  public static Document fromInputStream(InputStream in) {
    DocumentBuilderFactory factory = null;
    DocumentBuilder builder = null;
    Document ret = null;

    try {
      factory = DocumentBuilderFactory.newInstance();
      builder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }

    try {
      ret = builder.parse(new InputSource(in));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return ret;
  }

}
