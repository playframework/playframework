/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs;

import akka.util.ByteString;
import akka.util.ByteString$;
import akka.util.ByteStringBuilder;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * XML utilities.
 */
public class XML {

    /**
     * Parses an XML string as DOM.
     *
     * @param xml the input XML string
     * @return the parsed XML DOM root.
     */
    public static Document fromString(String xml) {
        try {
            return fromInputStream(
                    new ByteArrayInputStream(xml.getBytes("utf-8")),
                    "utf-8"
            );
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parses an InputStream as DOM.
     * @param in          the inputstream to parse.
     * @param encoding the encoding of the input stream, if not null.
     * @return the parsed XML DOM.
     */
    public static Document fromInputStream(InputStream in, String encoding) {
        InputSource is = new InputSource(in);
        if (encoding != null) {
            is.setEncoding(encoding);
        }

        return fromInputSource(is);
    }

    /**
     * Parses the input source as DOM.
     *
     * @param source The source to parse.
     * @return The Document.
     */
    public static Document fromInputSource(InputSource source) {
        try {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_GENERAL_ENTITIES_FEATURE, false);
            factory.setFeature(Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_PARAMETER_ENTITIES_FEATURE, false);
            factory.setFeature(Constants.XERCES_FEATURE_PREFIX + Constants.DISALLOW_DOCTYPE_DECL_FEATURE, true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();

            return builder.parse(source);

        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts the document to bytes.
     *
     * @param document The document to convert.
     * @return The ByteString representation of the document.
     */
    public static ByteString toBytes(Document document) {
        ByteStringBuilder builder = ByteString$.MODULE$.newBuilder();
        try {
            TransformerFactory.newInstance().newTransformer()
                    .transform(new DOMSource(document), new StreamResult(builder.asOutputStream()));
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
        return builder.result();
    }

    /**
     * Includes the SAX prefixes from 'com.sun.org.apache.xerces.internal.impl.Constants'
     * since they will likely be internal in JDK9
     */
    public static class Constants {
        public static final String SAX_FEATURE_PREFIX = "http://xml.org/sax/features/";
        public static final String XERCES_FEATURE_PREFIX = "http://apache.org/xml/features/";
        public static final String EXTERNAL_GENERAL_ENTITIES_FEATURE = "external-general-entities";
        public static final String EXTERNAL_PARAMETER_ENTITIES_FEATURE = "external-parameter-entities";
        public static final String DISALLOW_DOCTYPE_DECL_FEATURE = "disallow-doctype-decl";
    }

}
