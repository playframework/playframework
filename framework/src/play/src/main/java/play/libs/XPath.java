package play.libs;

import java.util.List;
import java.util.Map;

import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Node;
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
    @SuppressWarnings("unchecked")
    public static List<Node> selectNodes(String path, Object node, Map<String, String> namespaces) {
        try {
            return getDOMXPath(path, namespaces).selectNodes(node);
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
    public static List<Node> selectNodes(String path, Object node) {
        return selectNodes(path, node, null);
    }

    public static Node selectNode(String path, Object node, Map<String, String> namespaces) {
        try {
            List<Node> nodes = selectNodes(path, node, namespaces);
            if (nodes.size() == 0) {
                return null;
            }
            return nodes.get(0);
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
            Node rnode = (Node) getDOMXPath(path, namespaces).selectSingleNode(node);
            if (rnode == null) {
                return null;
            }
            if (!(rnode instanceof Text)) {
                rnode = rnode.getFirstChild();
            }
            if (!(rnode instanceof Text)) {
                return null;
            }
            return ((Text) rnode).getData();
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

    private static DOMXPath getDOMXPath(String path, Map<String, String> namespaces) throws Exception {
        DOMXPath xpath = new DOMXPath(path);
        if (namespaces != null) {
            for (String prefix: namespaces.keySet()) {
                xpath.addNamespace(prefix, namespaces.get(prefix));
            }
        }
        return xpath;
    }

}
