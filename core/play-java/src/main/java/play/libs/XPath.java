/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs;


import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * XPath for parsing
 */
public class XPath {

    static class PlayNamespaceContext implements NamespaceContext {

        private final Map<String, String> prefixMap = new HashMap<>();
        private final Map<String, Set<String>> namespaceMap = new HashMap<>();

        @Override
        public String getNamespaceURI(String prefix) {
            final String p = requireNonNull(prefix, "Null prefix");
            return Optional.of(prefixMap.get(p)).orElse(XMLConstants.NULL_NS_URI);
        }

        private Set<String> getPrefixesSet(String namespaceUri) {
            if (XMLConstants.XML_NS_URI.equals(namespaceUri)) {
                return singleton(XMLConstants.XML_NS_PREFIX);
            } else if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceUri)) {
                return singleton(XMLConstants.XMLNS_ATTRIBUTE);
            } else {
                Set<String> prefixes = namespaceMap.get(namespaceUri);
                return prefixes != null ? unmodifiableSet(prefixes) : emptySet();
            }
        }


        @Override
        public String getPrefix(String namespaceURI) {
            final String uri = requireNonNull(namespaceURI, "Null namespaceURI");
            return getPrefixesSet(uri).stream().findFirst().orElse(null);
        }

        @Override
        public Iterator getPrefixes(String namespaceURI) {
            final String uri = requireNonNull(namespaceURI, "Null namespaceURI");
            return getPrefixesSet(uri).iterator();
        }

        void bindNamespaceUri(String prefix, String namespaceURI) {
            final String p = requireNonNull(prefix, "Null prefix");
            final String uri = requireNonNull(namespaceURI, "Null namespaceURI");
            if (!XMLConstants.DEFAULT_NS_PREFIX.equals(p)) {
                prefixMap.put(p, uri);
                Set<String> prefixSet = namespaceMap.computeIfAbsent(uri, k -> new LinkedHashSet<>());
                prefixSet.add(p);
            }
        }
    }

    /**
     * Select all nodes that are selected by this XPath expression. If multiple nodes match,
     * multiple nodes will be returned. Nodes will be returned in document-order,
     * @param path the xpath expression
     * @param node the starting node
     * @param namespaces Namespaces that need to be available in the xpath, where the key is the
     * prefix and the value the namespace URI
     * @return result of evaluating the xpath expression against node
     */
    public static NodeList selectNodes(String path, Object node, Map<String, String> namespaces) {
        try {
            XPathFactory factory = XPathFactory.newInstance();
            javax.xml.xpath.XPath xpath = factory.newXPath();
            
            if (namespaces != null) {
                PlayNamespaceContext nsContext = new PlayNamespaceContext();
                bindUnboundedNamespaces(nsContext, namespaces);
                xpath.setNamespaceContext(nsContext);
            }

            return (NodeList) xpath.evaluate(path, node, XPathConstants.NODESET);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Select all nodes that are selected by this XPath expression. If multiple nodes match,
     * multiple nodes will be returned. Nodes will be returned in document-order,
     * @param path the xpath expression
     * @param node the starting node
     * @return result of evaluating the xpath expression against node
     */
    public static NodeList selectNodes(String path, Object node) {
        return selectNodes(path, node, null);
    }

    public static Node selectNode(String path, Object node, Map<String, String> namespaces) {
        try {
            XPathFactory factory = XPathFactory.newInstance();
            javax.xml.xpath.XPath xpath = factory.newXPath();
            
            if (namespaces != null) {
                PlayNamespaceContext nsContext = new PlayNamespaceContext();
                bindUnboundedNamespaces(nsContext, namespaces);
                xpath.setNamespaceContext(nsContext);
            }

            return (Node) xpath.evaluate(path, node, XPathConstants.NODE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Node selectNode(String path, Object node) {
        return selectNode(path, node, null);
    }

    private static void bindUnboundedNamespaces(PlayNamespaceContext nsContext, Map<String, String> namespaces) {
        namespaces.forEach((key, value) -> {
            if(nsContext.getPrefix(value) == null) {
                nsContext.bindNamespaceUri(key, value);
            }
        });
    }

    /**
     * @param path the XPath to execute
     * @param node the node, node-set or Context object for evaluation. This value can be null.
     * @param namespaces    the XML namespaces map
     * @return the text of a node, or the value of an attribute
     */
    public static String selectText(String path, Object node, Map<String, String> namespaces) {
        try {
            XPathFactory factory = XPathFactory.newInstance();
            javax.xml.xpath.XPath xpath = factory.newXPath();

            if (namespaces != null) {
                PlayNamespaceContext nsContext = new PlayNamespaceContext();
                bindUnboundedNamespaces(nsContext, namespaces);
                xpath.setNamespaceContext(nsContext);
            }

            return (String) xpath.evaluate(path, node, XPathConstants.STRING);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param path the XPath to execute
     * @param node the node, node-set or Context object for evaluation. This value can be null.
     * @return the text of a node, or the value of an attribute
     */
    public static String selectText(String path, Object node) {
        return selectText(path, node, null);
    }

}
