/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs;

import java.util.List;
import java.util.Map;

import javax.xml.xpath.*;

import org.springframework.util.xml.SimpleNamespaceContext;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;


/**
 * XPath for parsing
 */
public class XPath {

    /**
     * Select all nodes that are selected by this XPath expression. If multiple nodes match,
     * multiple nodes will be returned. Nodes will be returned in document-order,
     * @param path
     * @param node
     * @param namespaces Namespaces that need to be available in the xpath, where the key is the
     * prefix and the value the namespace URI
     * @return
     */
    public static NodeList selectNodes(String path, Object node, Map<String, String> namespaces) {
        try {
            XPathFactory factory = XPathFactory.newInstance();
            javax.xml.xpath.XPath xpath = factory.newXPath();
            
            if (namespaces != null) {
                SimpleNamespaceContext nsContext = new SimpleNamespaceContext();
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
     * @param path
     * @param node
     * @return
     */
    public static NodeList selectNodes(String path, Object node) {
        return selectNodes(path, node, null);
    }

    public static Node selectNode(String path, Object node, Map<String, String> namespaces) {
        try {
            XPathFactory factory = XPathFactory.newInstance();
            javax.xml.xpath.XPath xpath = factory.newXPath();
            
            if (namespaces != null) {
                SimpleNamespaceContext nsContext = new SimpleNamespaceContext();
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

    private static void bindUnboundedNamespaces(SimpleNamespaceContext nsContext, Map<String, String> namespaces) {
        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            //making sure that namespace is not already bound. Otherwise UnsupportedException happens
            if(nsContext.getPrefix(entry.getValue()) == null) {
                nsContext.bindNamespaceUri(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Return the text of a node, or the value of an attribute
     * @param path the XPath to execute
     * @param node the node, node-set or Context object for evaluation. This value can be null.
     */
    public static String selectText(String path, Object node, Map<String, String> namespaces) {
        try {
            XPathFactory factory = XPathFactory.newInstance();
            javax.xml.xpath.XPath xpath = factory.newXPath();

            if (namespaces != null) {
                SimpleNamespaceContext nsContext = new SimpleNamespaceContext();
                bindUnboundedNamespaces(nsContext, namespaces);
                xpath.setNamespaceContext(nsContext);
            }

            return (String) xpath.evaluate(path, node, XPathConstants.STRING);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the text of a node, or the value of an attribute
     * @param path the XPath to execute
     * @param node the node, node-set or Context object for evaluation. This value can be null.
     */
    public static String selectText(String path, Object node) {
        return selectText(path, node, null);
    }

}
