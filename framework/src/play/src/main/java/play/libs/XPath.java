package play.libs;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.*;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import org.apache.ws.commons.util.NamespaceContextImpl;



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
    @SuppressWarnings("unchecked")
    public static NodeList selectNodes(String path, Object node, Map<String, String> namespaces) {
        try {
            XPathFactory factory = XPathFactory.newInstance();
            javax.xml.xpath.XPath xpath = factory.newXPath();
            
            if (namespaces != null) {
                NamespaceContextImpl nsContext = new NamespaceContextImpl();
                for (Map.Entry<String, String> namespace : namespaces.entrySet()) {
                    nsContext.startPrefixMapping(namespace.getKey(), namespace.getValue());                    
                }
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
                NamespaceContextImpl nsContext = new NamespaceContextImpl();
                for (Map.Entry<String, String> namespace : namespaces.entrySet()) {
                    nsContext.startPrefixMapping(namespace.getKey(), namespace.getValue());                    
                }
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
                NamespaceContextImpl nsContext = new NamespaceContextImpl();
                for (Map.Entry<String, String> namespace : namespaces.entrySet()) {
                    nsContext.startPrefixMapping(namespace.getKey(), namespace.getValue());                    
                }
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
